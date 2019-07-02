package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.wgs84.WGSUtil;
import org.neo4j.spatial.core.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.stream.Collectors;

public class OSMTraverser {
    public static Pair<List<List<Node>>, List<List<Node>>> traverseOSMGraph(Node main) {
        List<List<Node>> wayNodes = collectWays(main);
        List<EnrichedWay> candidates = wayNodes.stream().map(EnrichedWay::new).collect(Collectors.toList());

        Pair<List<EnrichedWay>, List<EnrichedWay>> enrichedWays = connectWaysByCommonNode(candidates);

        List<EnrichedWay> polygons = new ArrayList<>(enrichedWays.first());
        List<EnrichedWay> polylines = new ArrayList<>(enrichedWays.other());

        if (enrichedWays.other().size() > 0) {
            enrichedWays = connectWaysByProximity(polylines);

            polygons.addAll(enrichedWays.first());
            polylines = enrichedWays.other();
        }

        List<List<Node>> polygonNodes = polygons.stream().map(p -> p.wayNodes).collect(Collectors.toList());
        List<List<Node>> polylineNodes = polylines.stream().map(p -> p.wayNodes).collect(Collectors.toList());

        return Pair.of(polygonNodes, polylineNodes);
    }

    /**
     * Find all ways related to the OSMRelation and try to combine connected ways (share same node at extreme points)
     *
     * @param main The node representing the OSMRelation
     * @return List of ways where each way is connected by a common node
     */
    private static List<List<Node>> collectWays(Node main) {
        GraphDatabaseService db = main.getGraphDatabase();
        List<List<Node>> wayNodes = new ArrayList<>();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("main", main.getId());
        String findWayNodes = "MATCH (r:OSMRelation)-[:MEMBER*]->(w:OSMWay)-[:FIRST_NODE]->(f:OSMWayNode), " +
                "(f)-[:NEXT*0..]->(wn:OSMWayNode) WHERE id(r) = $main " +
                "RETURN f, collect(wn) AS nodes";
        Result result = db.execute(findWayNodes, parameters);
        while(result.hasNext()) {
            Map<String, Object> next = result.next();
            List<Node> nextWay = (List<Node>) next.get("nodes");
            wayNodes.add(nextWay);
        }
        return wayNodes;
    }

    /**
     * Connect neighboring ways to create polygons by common nodes.
     *
     * @param candidates List of candidate ways
     * @return First list contains enriched ways which describe full polygons
     * and the second list contains enriched ways which are not complete polygons
     */
    private static Pair<List<EnrichedWay>, List<EnrichedWay>> connectWaysByCommonNode(List<EnrichedWay> candidates) {
        List<EnrichedWay> polygons = new ArrayList<>();
        List<EnrichedWay> notPolygons = new ArrayList<>();

        while (!candidates.isEmpty()) {
            boolean joinedSome;
            EnrichedWay way = candidates.get(0);
            candidates.remove(0);

            do {
                joinedSome = false;
                ListIterator<EnrichedWay> wayIter = candidates.listIterator();
                while (wayIter.hasNext()) {
                    EnrichedWay candidate = wayIter.next();
                    if (way.joinByCommonNode(candidate)) {
                        wayIter.remove();
                        joinedSome = true;
                    }
                }
            } while (joinedSome);

            if (way.isClosed()) {
                polygons.add(way);
            } else {
                notPolygons.add(way);
            }
        }

        return Pair.of(polygons, notPolygons);
    }

    /**
     * Connect neighboring ways to create polygons by proximity of the end nodes.
     *
     * @param candidates List of candidate ways
     * @return List of enriched ways where each enriched way describes a polygon
     */
    private static Pair<List<EnrichedWay>, List<EnrichedWay>> connectWaysByProximity(List<EnrichedWay> candidates) {
        List<EnrichedWay> polygons = new ArrayList<>();
        List<EnrichedWay> polylines = new ArrayList<>();

        polylines.add(candidates.get(0));
        candidates.remove(0);

        while (candidates.size() > 0) {
            EnrichedWay wayToAdd = candidates.get(0);
            candidates.remove(0);

            double minDistance = Double.MAX_VALUE;
            int bestIndex = -1;
            EnrichedWay.JoinDirection joinDirection = null;

            for (int i = 0; i < polylines.size(); i++) {
                EnrichedWay candidateWayToAddTo = polylines.get(i);

                Pair<Double, EnrichedWay.JoinDirection> distanceDirection = candidateWayToAddTo.distanceTo(wayToAdd);

                if (distanceDirection.first() < minDistance) {
                    minDistance = distanceDirection.first();
                    joinDirection = distanceDirection.other();
                    bestIndex = i;
                }
            }

            //No other polyline is close
            if (minDistance > EnrichedWay.PROXIMITY_THRESHOLD) {
                Vector first = new Vector(true, wayToAdd.first.other());
                Vector last = new Vector(true, wayToAdd.last.other());
                double distance = WGSUtil.distance(first, last);

                //The polyline closes itself
                if (AlgoUtil.lessOrEqual(distance, EnrichedWay.PROXIMITY_THRESHOLD)) {
                    polygons.add(wayToAdd);
                    polylines.remove(wayToAdd);
                } else {
                    polylines.add(wayToAdd);
                }

                continue;
            }

            EnrichedWay wayToAddTo = polylines.get(bestIndex);
            wayToAddTo.join(wayToAdd, joinDirection);

            Vector first = new Vector(true, wayToAddTo.first.other());
            Vector last = new Vector(true, wayToAddTo.last.other());
            double distance = WGSUtil.distance(first, last);

            //The polyline closes itself
            if (AlgoUtil.lessOrEqual(distance, EnrichedWay.PROXIMITY_THRESHOLD)) {
                polygons.add(wayToAddTo);
                polylines.remove(wayToAddTo);
            }
        }

        return Pair.of(polygons, polylines);
    }

    private static class EnrichedWay {
        static final double PROXIMITY_THRESHOLD = 250; //in meters

        Pair<Node, double[]> first;
        Pair<Node, double[]> last;
        private List<Node> wayNodes;

        enum JoinDirection {
            FF, FL, LF, LL
        }

        EnrichedWay (List<Node> wayNodes) {
            this.wayNodes = wayNodes;
            this.first = getOSMNode(wayNodes.get(0));
            this.last = getOSMNode(wayNodes.get(wayNodes.size() - 1));
        }

        boolean joinByCommonNode(EnrichedWay other) {
            if (first.first().equals(other.last.first())) {
                join(other, JoinDirection.FL);
                return true;
            } else if (last.first().equals(other.first.first())) {
                join(other, JoinDirection.LF);
                return true;
            } else if (first.first().equals(other.first.first())) {
                join(other, JoinDirection.FF);
                return true;
            } else if (last.first().equals(other.last.first())) {
                join(other, JoinDirection.LL);
                return true;
            }
            return false;
        }

        void join(EnrichedWay other, JoinDirection direction) {
            switch (direction) {
                case FF:
                    Collections.reverse(other.wayNodes);
                    other.wayNodes.addAll(wayNodes);
                    wayNodes = other.wayNodes;
                    this.first = other.last;
                    break;
                case FL:
                    other.wayNodes.addAll(wayNodes);
                    wayNodes = other.wayNodes;
                    this.first = other.first;
                    break;
                case LF:
                    wayNodes.addAll(other.wayNodes);
                    this.last = other.last;
                    break;
                case LL:
                    Collections.reverse(other.wayNodes);
                    this.wayNodes.addAll(other.wayNodes);
                    this.last = other.first;
                    break;
            }
        }

        Pair<Double, JoinDirection> distanceTo(EnrichedWay other) {
            double[] tf = this.first.other();
            double[] tl = this.last.other();
            double[] of = other.first.other();
            double[] ol = other.last.other();

            List<Pair<Double, JoinDirection>> options = new ArrayList<>();
            options.add(Pair.of(distance(tf, of), JoinDirection.FF));
            options.add(Pair.of(distance(tf, ol), JoinDirection.FL));
            options.add(Pair.of(distance(tl, of), JoinDirection.LF));
            options.add(Pair.of(distance(tl, ol), JoinDirection.LL));

            return options.stream().min(Comparator.comparingDouble(Pair::first)).get();
        }

        private double distance(double[] a, double[] b) {
            Vector u = new Vector(true, a);
            Vector v = new Vector(true, b);
            return WGSUtil.distance(u, v);
        }

        boolean isClosed() {
            return first.first().equals(last.first());
        }

        @Override
        public String toString() {
            return "EW[" + Arrays.toString(first.other()) + ", " + Arrays.toString(last.other()) + "]";
        }

        /**
         * @param wayNode
         * @return The coordinate belonging to the OSMWayNode
         */
        static Pair<Node, double[]> getOSMNode(Node wayNode) {
            Node node = wayNode.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();

            Point point = (Point) node.getProperty("location");

            return Pair.of(node, point.getCoordinate().getCoordinate().stream().mapToDouble(i -> i).toArray());
        }
    }
}
