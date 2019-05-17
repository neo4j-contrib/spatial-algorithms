package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.graphdb.traversal.Evaluators;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.Distance;

import java.util.*;

public class OSMTraverser {
    public static List<List<Node>> traverseOSMGraph(Node main) {
        List<List<Node>> wayNodes = collectWays(main);
        return connectWays(wayNodes);
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

        Set<Node> ways = new HashSet<>();
        ResourceIterable<Node> wayIterator = new MonoDirectionalTraversalDescription().depthFirst()
                .relationships(RelationshipType.withName("MEMBER"), Direction.OUTGOING).traverse(main).nodes();

        for (Node way : wayIterator) {
            if (ways.contains(way)) {
                continue;
            }

            ways.add(way);

            Iterator<Label> labelIterator = way.getLabels().iterator();
            boolean flag = false;
            while (labelIterator.hasNext()) {
                Label label = labelIterator.next();
                if (label.name().equals("OSMWay")) {
                    flag = true;
                }
            }
            if (!flag) {
                continue;
            }

            Node startWayNode = new MonoDirectionalTraversalDescription().depthFirst()
                    .relationships(RelationshipType.withName("FIRST_NODE"), Direction.OUTGOING)
                    .evaluator(Evaluators.includeWhereLastRelationshipTypeIs(RelationshipType.withName("FIRST_NODE")))
                    .traverse(way).iterator().next().endNode();

            TraversalDescription followWay = new MonoDirectionalTraversalDescription()
                    .depthFirst().relationships(RelationshipType.withName("NEXT"));

            List<Node> currentWayNodes = new ArrayList<>();

            while (currentWayNodes.isEmpty() || !currentWayNodes.get(0).equals(currentWayNodes.get(currentWayNodes.size() - 1))) {
                ResourceIterator<Node> wayNodeIterator = followWay.traverse(startWayNode).nodes().iterator();

                if (!currentWayNodes.isEmpty()) {
                    wayNodeIterator.next();
                }


                while (wayNodeIterator.hasNext()) {
                    Node wayNode = wayNodeIterator.next();
                    currentWayNodes.add(wayNode);
                }

                Node lastCurrentWayNode = currentWayNodes.get(currentWayNodes.size() - 1);

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("main", main.getId());
                parameters.put("lastCurrentWayNode", lastCurrentWayNode.getId());

                Result result = db.execute("MATCH (l:OSMWayNode)-[:NODE]->(:OSMNode)<-[:NODE]-(n:OSMWayNode)<-[:NEXT*0..]-(:OSMWayNode)<-[:FIRST_NODE]-(w:OSMWay)<-[:MEMBER]-(m:OSMRelation) " +
                        "WHERE id(l) = $lastCurrentWayNode AND id(m) = $main AND l <> n RETURN n AS NEXT, w AS WAY;", parameters);

                flag = false;
                while (result.hasNext()) {
                    Map<String, Object> next = result.next();
                    Node nextWay = (Node) next.get("WAY");

                    if (ways.contains(nextWay)) {
                        break;
                    }

                    startWayNode = (Node) next.get("NEXT");
                    ways.add(nextWay);
                    flag = true;
                    break;
                }

                if (!flag) {
                    break;
                }
            }

            wayNodes.add(currentWayNodes);
        }
        return wayNodes;
    }

    /**
     * Connect neighboring ways to create polygons.
     *
     * @param wayNodes List of ways
     * @return Nest list of nodes where each inner list describes a polygon
     */
    private static List<List<Node>> connectWays(List<List<Node>> wayNodes) {
        int totalSteps = 0;
        double totalDistance = 0;
        for (List<Node> wayNodeList : wayNodes) {
            totalSteps += wayNodeList.size() - 1;
            for (int i = 0; i < wayNodeList.size() - 1; i++) {
                Node a = wayNodeList.get(i);
                Node b = wayNodeList.get(i+1);
                totalDistance += Distance.distance(getCoordinates(a), getCoordinates(b));
            }
        }

        double meanStepSize = totalDistance/totalSteps;

        List<List<Node>> polygons= new ArrayList<>();

        List<List<Node>> polygonsToComplete = new ArrayList<>();
        polygonsToComplete.add(new ArrayList<>(wayNodes.get(0)));
        wayNodes.remove(0);
        while (wayNodes.size() > 0) {
            List<Node> wayToAdd = wayNodes.get(0);
            wayNodes.remove(0);

            double[] first = getCoordinates(wayToAdd.get(0));
            double[] last = getCoordinates(wayToAdd.get(wayToAdd.size() - 1));
            double distance = Distance.distance(first, last);

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

                double distanceFirst = Distance.distance(first, lastCoordinates);
                double distanceLast = Distance.distance(last, lastCoordinates);

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
            distance = Distance.distance(first, last);

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

    private static double[] getCoordinates(Node wayNode) {
        RelationshipType nodeRel = RelationshipType.withName("NODE");
        Node node = wayNode.getSingleRelationship(nodeRel, Direction.OUTGOING).getEndNode();

        Point point = (Point) node.getProperty("location");

        return point.getCoordinate().getCoordinate().stream().mapToDouble(i -> i).toArray();
    }
}
