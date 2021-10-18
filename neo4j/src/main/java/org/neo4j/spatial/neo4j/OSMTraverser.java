package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.wgs84.WGSUtil;
import org.neo4j.spatial.core.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class OSMTraverser {
    /**
     * Traverse the OpenStreetMap graph looking for geometries in the form of a list of polygons and a list of polylines.
     *
     * @param tx the database transaction in which to query the database
     * @param main The node representing the OSMRelation
     * @return A pair of two collections, one of polygons and one of polylines
     */
    public static Pair<List<List<Node>>, List<List<Node>>> traverseOSMGraph(Transaction tx, Node main, double proximityThreshold) {
        List<List<Node>> wayNodes = collectWays(tx, main);
        List<EnrichedWay> candidates = wayNodes.stream().map(EnrichedWay::new).collect(Collectors.toList());
        int totalNodeCount = candidates.stream().mapToInt(EnrichedWay::size).sum();
        System.out.println("Found " + candidates.size() + " polygon/polyline candidates comprising " + totalNodeCount + " nodes from " + wayNodes.size() + " ways within " + main);

        Pair<List<EnrichedWay>, List<EnrichedWay>> enrichedWays = connectWaysByCommonNode(candidates);

        List<EnrichedWay> polygons = new ArrayList<>(enrichedWays.first());
        List<EnrichedWay> polylines = new ArrayList<>(enrichedWays.other());
        debugPolygonPolylines(totalNodeCount, polygons, polylines);

        if (enrichedWays.other().size() > 0) {
            System.out.println("Attempting proximity connections to covert some of " + polylines.size() + " polylines into polygons");
            enrichedWays = connectWaysByProximity(polylines, proximityThreshold);

            polygons.addAll(enrichedWays.first());
            polylines = enrichedWays.other();
            debugPolygonPolylines(totalNodeCount, polygons, polylines);
        }

        List<List<Node>> polygonNodes = polygons.stream().map(p -> p.wayNodes).collect(Collectors.toList());
        List<List<Node>> polylineNodes = polylines.stream().map(p -> p.wayNodes).collect(Collectors.toList());

        return Pair.of(polygonNodes, polylineNodes);
    }

    private static void debugPolygonPolylines(int totalNodeCount, List<EnrichedWay> polygons, List<EnrichedWay> polylines) {
        int polygonNodeCount = polygons.stream().mapToInt(EnrichedWay::size).sum();
        int polylineNodeCount = polylines.stream().mapToInt(EnrichedWay::size).sum();
        System.out.println("We have " + polygons.size() + " polygons (" + polygonNodeCount + "/" + totalNodeCount + " nodes, " + (100 * polygonNodeCount / totalNodeCount) + "%)");
        for (Pair<EnrichedWay, Integer> top : polygons.stream().map(w -> Pair.of(w, w.size())).sorted((a, b) -> b.other() - a.other()).limit(10).collect(Collectors.toList())) {
            System.out.println("\t" + top.other() + "\t" + top.first());
        }
        System.out.println("We have " + polylines.size() + " polylines (" + polylineNodeCount + "/" + totalNodeCount + " nodes, " + (100 * polylineNodeCount / totalNodeCount) + "%)");
        for (Pair<EnrichedWay, Integer> top : polylines.stream().map(w -> Pair.of(w, w.size())).sorted((a, b) -> b.other() - a.other()).limit(10).collect(Collectors.toList())) {
            System.out.println("\t" + top.other() + "\t" + top.first());
        }
    }

    /**
     * Find all ways related to the OSMRelation and try to combine connected ways (share same node at extreme points)
     *
     * @param tx the database transaction in which to query the database
     * @param main The node representing the OSMRelation
     * @return List of ways where each way is connected by a common node
     */
    private static List<List<Node>> collectWays(Transaction tx, Node main) {
        List<List<Node>> wayNodes = new ArrayList<>();
        String findWayNodes = null;
        if (main.hasProperty("relation_osm_id")) {
            findWayNodes = "MATCH (r:OSMRelation)-[:MEMBER*]->(w:OSMWay)-[:FIRST_NODE]->(f:OSMWayNode), " +
                    "(f)-[:NEXT*0..]->(wn:OSMWayNode) WHERE id(r) = $main " +
                    "RETURN f, collect(wn) AS nodes";
        } else if (main.hasProperty("way_osm_id")) {
            findWayNodes = "MATCH (w:OSMWay)-[:FIRST_NODE]->(f:OSMWayNode), " +
                    "(f)-[:NEXT*0..]->(wn:OSMWayNode) WHERE id(w) = $main " +
                    "RETURN f, collect(wn) AS nodes";
        } else {
            throw new IllegalArgumentException("Cannot find ways from OSM node that is neither a Relation nor a Way: " + main);
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("main", main.getId());
        Result result = tx.execute(findWayNodes, parameters);
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
    private static Pair<List<EnrichedWay>, List<EnrichedWay>> connectWaysByProximity(List<EnrichedWay> candidates, double proximityThreshold) {
        List<EnrichedWay> polygons = new ArrayList<>();
        List<EnrichedWay> polylines = new ArrayList<>();

        polylines.add(candidates.remove(0));

        while (candidates.size() > 0) {
            EnrichedWay wayToAdd = candidates.remove(0);

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

            // Closest polyline is close enough to merge
            if (minDistance <= proximityThreshold) {
                EnrichedWay wayToAddTo = polylines.get(bestIndex);
                wayToAddTo.join(wayToAdd, joinDirection);

                Vector first = new Vector(true, wayToAddTo.first.other());
                Vector last = new Vector(true, wayToAddTo.last.other());
                double distance = WGSUtil.distance(first, last);

                //The polyline closes itself
                if (AlgoUtil.lessOrEqual(distance, proximityThreshold)) {
                    polygons.add(wayToAddTo);
                    polylines.remove(wayToAddTo);
                }
            } else {
                Vector first = new Vector(true, wayToAdd.first.other());
                Vector last = new Vector(true, wayToAdd.last.other());
                double distance = WGSUtil.distance(first, last);

                //The polyline closes itself
                if (AlgoUtil.lessOrEqual(distance, proximityThreshold)) {
                    polygons.add(wayToAdd);
                    polylines.remove(wayToAdd);
                } else {
                    polylines.add(wayToAdd);
                }
            }
        }

        return Pair.of(polygons, polylines);
    }

    private static class EnrichedWay {
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

        int size() {
            return wayNodes.size();
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
