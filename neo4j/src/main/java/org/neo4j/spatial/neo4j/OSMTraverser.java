package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.cartesian.CartesianDistance;

import java.util.*;
import java.util.stream.Collectors;

public class OSMTraverser {
    public static List<List<Node>> traverseOSMGraph(Node main) {
        Stack<List<Node>> wayNodes = collectWays(main);
        return connectWays(wayNodes);
    }

    /**
     * Find all ways related to the OSMRelation and try to combine connected ways (share same node at extreme points)
     *
     * @param main The node representing the OSMRelation
     * @return List of ways where each way is connected by a common node
     */
    private static Stack<List<Node>> collectWays(Node main) {
        GraphDatabaseService db = main.getGraphDatabase();
        Stack<List<Node>> wayNodes = new Stack<>();

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
     * Connect neighboring ways to create polygons.
     *
     * @param wayNodes List of ways
     * @return Nest list of nodes where each inner list describes a polygon
     */
    private static List<List<Node>> connectWaysX(Stack<List<Node>> wayNodes) {
        List<EnrichedWay> polygons = new ArrayList<>();
        List<EnrichedWay> notPolygons = new ArrayList<>();
        List<EnrichedWay> candidates = wayNodes.stream().map(EnrichedWay::new).collect(Collectors.toList());

        while (!candidates.isEmpty()) {
            boolean mergedSome;
            EnrichedWay way = candidates.get(0);
            candidates.remove(0);

            do {
                mergedSome = false;
                ListIterator<EnrichedWay> wayIter = candidates.listIterator();
                while (wayIter.hasNext()) {
                    EnrichedWay candidate = wayIter.next();
                    if (way.join(candidate)) {
                        wayIter.remove();
                        mergedSome = true;
                    }
                }
            } while (mergedSome);

            if (way.isClosed()) {
                polygons.add(way);
            } else {
                notPolygons.add(way);
            }
        }

        return polygons.stream().map(p -> p.wayNodes).collect(Collectors.toList());
    }

    private static class EnrichedWay {
        Pair<Node, double[]> first;
        Pair<Node, double[]> last;
        private List<Node> wayNodes;

        EnrichedWay (List<Node> wayNodes) {
            this.wayNodes = wayNodes;
            this.first = getOSMNode(wayNodes.get(0));
            this.last = getOSMNode(wayNodes.get(wayNodes.size() - 1));
        }

        boolean join(EnrichedWay other) {
            if (first.first().equals(other.last.first())) {
                this.wayNodes.remove(0);
                other.wayNodes.addAll(wayNodes);
                wayNodes = other.wayNodes;
                this.first = other.first;
                return true;
            } else if (last.first().equals(other.first.first())) {
                other.wayNodes.remove(0);
                wayNodes.addAll(other.wayNodes);
                this.last = other.last;
                return true;
            } else if (first.first().equals(other.first.first())) {
                this.wayNodes.remove(0);
                Collections.reverse(other.wayNodes);
                other.wayNodes.addAll(wayNodes);
                wayNodes = other.wayNodes;
                this.first = other.last;
                return true;
            } else if (last.first().equals(other.last.first())) {
                Collections.reverse(other.wayNodes);
                other.wayNodes.remove(0);
                this.wayNodes.addAll(other.wayNodes);
                this.last = other.first;
                return true;
            }
            return false;
        }

        boolean isClosed() {
            return first.first().equals(last.first());
        }
    }
    /**
     * Connect neighboring ways to create polygons.
     *
     * @param wayNodes List of ways
     * @return Nest list of nodes where each inner list describes a polygon
     */
    private static List<List<Node>> connectWays(Stack<List<Node>> wayNodes) {
        int totalSteps = 0;
        double totalDistance = 0;
        for (List<Node> wayNodeList : wayNodes) {
            totalSteps += wayNodeList.size() - 1;
            for (int i = 0; i < wayNodeList.size() - 1; i++) {
                Node a = wayNodeList.get(i);
                Node b = wayNodeList.get(i+1);
                totalDistance += CartesianDistance.distance(getCoordinates(a), getCoordinates(b));
            }
        }

        double meanStepSize = totalDistance/totalSteps;

        List<List<Node>> polygons= new ArrayList<>();

        List<List<Node>> polygonsToComplete = new ArrayList<>();
        polygonsToComplete.add(new ArrayList<>(wayNodes.pop()));
        while (wayNodes.size() > 0) {
            List<Node> wayToAdd = wayNodes.pop();

            double[] first = getCoordinates(wayToAdd.get(0));
            double[] last = getCoordinates(wayToAdd.get(wayToAdd.size() - 1));
            double distance = CartesianDistance.distance(first, last);

            // TODO: Confirm that we actually need this
            //The polystring closes itself
            if (AlgoUtil.lessOrEqual(distance, meanStepSize)) {
                polygons.add(wayToAdd);
                continue;
            }

            double minDistance = Double.MAX_VALUE;
            int bestIndex = -1;
            boolean forward = true;

            for (int i = 0; i < polygonsToComplete.size(); i++) {
                List<Node> candidateWayToAddTo = polygonsToComplete.get(i);
                double[] lastCoordinates = getCoordinates(candidateWayToAddTo.get(candidateWayToAddTo.size() - 1));

                double distanceFirst = CartesianDistance.distance(first, lastCoordinates);
                double distanceLast = CartesianDistance.distance(last, lastCoordinates);

                if (distanceFirst < minDistance) {
                    minDistance = distanceFirst;
                    bestIndex = i;
                    forward = true;
                }

                if (distanceLast < minDistance) {
                    minDistance = distanceLast;
                    bestIndex = i;
                    forward = false;
                }
            }

            //No polystring is close, just close the polystring
            if (minDistance > meanStepSize) {
                polygonsToComplete.add(wayToAdd);
                continue;
            }

            List<Node> wayToAddTo = polygonsToComplete.get(bestIndex);

            if (forward) {
                wayToAddTo.addAll(wayToAdd);
            } else {
                List<Node> reversed = new ArrayList<>(wayToAdd);
                Collections.reverse(reversed);
                wayToAddTo.addAll(reversed);
            }

            first = getCoordinates(wayToAddTo.get(0));
            last = getCoordinates(wayToAddTo.get(wayToAddTo.size() - 1));
            distance = CartesianDistance.distance(first, last);

            //The polystring closes itself
            if (AlgoUtil.lessOrEqual(distance, meanStepSize)) {
                polygons.add(wayToAddTo);
                polygonsToComplete.remove(wayToAddTo);
            }
        }

        while (!polygonsToComplete.isEmpty()) {
            polygons.add(polygonsToComplete.get(0));
            polygonsToComplete.remove(0);
        }
        return polygons;
    }

    /**
     * @param wayNode
     * @return The coordinate belonging to the OSMWayNode
     */
    private static Pair<Node, double[]> getOSMNode(Node wayNode) {
        Node node = wayNode.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();

        Point point = (Point) node.getProperty("location");

        return Pair.of(node, point.getCoordinate().getCoordinate().stream().mapToDouble(i -> i).toArray());
    }

    /**
     * @param wayNode
     * @return The coordinate belonging to the OSMWayNode
     */
    private static double[] getCoordinates(Node wayNode) {
        Node node = wayNode.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();

        Point point = (Point) node.getProperty("location");

        return point.getCoordinate().getCoordinate().stream().mapToDouble(i -> i).toArray();
    }
}
