package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.spatial.algo.Distance;
import org.neo4j.spatial.algo.DistanceCalculator;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.Arrays;
import java.util.Iterator;

import static java.lang.String.format;

public abstract class Neo4jSimpleGraphPolygon implements Polygon.SimplePolygon {
    private long osmRelationId;
    private CRS crs;
    private ResourceIterator<Node> nodeIterator;
    boolean traversing;
    Node start;
    Node main;
    Point startPoint;

    public Neo4jSimpleGraphPolygon(Node main, long osmRelationId) {
        this.osmRelationId = osmRelationId;
        this.traversing = false;
        this.main = main;
        this.start = main;
        crs = extractPoint(main).getCRS();
    }

    @Override
    public CRS getCRS() {
        return crs;
    }

    @Override
    public int dimension() {
        return extractPoint(this.main).dimension();
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public String toString() {
        return format("Neo4jSimpleGraphNodePolygon%s", Arrays.toString(getPoints()));
    }

    private Traverser getNewTraverser(Node start) {
        return new MonoDirectionalTraversalDescription()
                .depthFirst()
                .relationships(Relation.NEXT, Direction.BOTH)
                .relationships(Relation.NEXT_IN_POLYGON, Direction.BOTH)
                .uniqueness(Uniqueness.NONE)
                .evaluator(new WayEvaluator(osmRelationId, null, null)).traverse(start);
    }

    private Traverser getNewTraverser(Node start, Direction nextDirection, Direction nextInPolygonDirection) {
        return new MonoDirectionalTraversalDescription()
                .depthFirst()
                .relationships(Relation.NEXT, Direction.BOTH)
                .relationships(Relation.NEXT_IN_POLYGON)
                .uniqueness(Uniqueness.NONE)
                .evaluator(new WayEvaluator(osmRelationId, nextDirection, nextInPolygonDirection)).traverse(start);
    }

    @Override
    public boolean fullyTraversed() {
        if (this.nodeIterator != null) {
            return !this.nodeIterator.hasNext() && this.traversing;
        }
        return false;
    }

    @Override
    public void startTraversal(Point startPoint, Point directionPoint) {
        ResourceIterator<Node> iterator = getNewTraverser(this.main).nodes().iterator();
        this.traversing = false;

        Distance calculator = DistanceCalculator.getCalculator(startPoint);

        Point firstPoint = null;

        double minDistance = Double.MAX_VALUE;
        while (iterator.hasNext()) {
            Node next = iterator.next();
            Point extracted = extractPoint(next);
            if (firstPoint == null) {
                firstPoint = extracted;
            } else if (firstPoint.equals(extracted)) {
                break;
            }

            double currentDistance = calculator.distance(extracted, startPoint);
            if (currentDistance <= minDistance) {
                minDistance = currentDistance;
                this.start = next;
                this.startPoint = extracted;
            }
        }
        Pair<Direction, Direction> directions = getClosestNeighborToDirection(directionPoint);
        this.nodeIterator = getNewTraverser(this.start, directions.first(), directions.other()).nodes().iterator();
    }

    private Pair<Direction, Direction> getClosestNeighborToDirection(Point directionPoint) {
        double minDistance = Double.MAX_VALUE;
        Direction minDirection = null;
        boolean nextInPolygon = true;

        Distance calculator = DistanceCalculator.getCalculator(directionPoint);

        for (Relationship relationship : this.start.getRelationships(Relation.NEXT_IN_POLYGON)) {
            if (WayEvaluator.nextInPolygon(relationship, osmRelationId)) {
                Node other = relationship.getOtherNode(this.start);

                double currentDistance = calculator.distance(directionPoint, extractPoint(other));
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    minDirection = relationship.getStartNode().equals(this.start) ? Direction.OUTGOING : Direction.INCOMING;
                }
            }
        }

        for (Relationship relationship : this.start.getRelationships(Relation.NEXT)) {
            Node other = relationship.getOtherNode(this.start);

            double currentDistance = calculator.distance(directionPoint, extractPoint(other));
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                minDirection = relationship.getStartNode().equals(this.start) ? Direction.OUTGOING : Direction.INCOMING;
                nextInPolygon = false;
            }
        }

        if (nextInPolygon) {
            return Pair.of(null, minDirection);
        } else {
            return Pair.of(minDirection, null);
        }
    }

    @Override
    public void startTraversal() {
        this.start = main;
        this.traversing = false;
        this.nodeIterator = getNewTraverser(this.start).nodes().iterator();
    }

    abstract Point extractPoint(Node node);

    Node getNextNode() {
        if (this.nodeIterator == null) {
            throw new TraversalException("No traversal is currently ongoing");
        }

        return this.nodeIterator.next();
    }

    protected Node[] traverseWholePolygon(Node main) {
        return getNewTraverser(main).nodes().stream().toArray(Node[]::new);
    }

    private static class WayEvaluator implements Evaluator {
        private long relationId;
        private Direction nextDirection;
        private Direction nextInPolygonDirection;
        private boolean firstWay;

        private long firstLocationNode = -1;
        private long previousLocationNode = -1;
        private boolean finished;
        private Direction lastNextDirection;

        public WayEvaluator(long relationId, Direction nextDirection, Direction nextInPolygonDirection) {
            this.relationId = relationId;
            this.nextDirection = nextDirection;
            this.nextInPolygonDirection = nextInPolygonDirection;
            this.firstWay = true;
            this.finished = false;
        }

        @Override
        public Evaluation evaluate(Path path) {
            if (finished) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            Relationship rel = path.lastRelationship();
            Node endNode = path.endNode();
            long locationNode = getLocationNode(endNode);

            if (path.length() == 1) {
                if (rel.isType(Relation.NEXT) && nextInPolygonDirection != null) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                } else if (rel.isType(Relation.NEXT_IN_POLYGON) && nextDirection != null) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            if (locationNode == previousLocationNode) {
                if (rel.isType(Relation.NEXT_IN_POLYGON)) {
                    if (!nextInPolygon(rel) || !validDirection(rel, endNode, nextInPolygonDirection)) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                    firstWay = false;
                    lastNextDirection = null;
                }
                return Evaluation.EXCLUDE_AND_CONTINUE;
            }

            if (firstLocationNode == locationNode) {
                finished = true;
                return Evaluation.INCLUDE_AND_PRUNE;
            }

            if (rel == null) {
                firstLocationNode = locationNode;
                previousLocationNode = locationNode;
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            if (rel.isType(Relation.NEXT_IN_POLYGON)) {
                if (!validDirection(rel, endNode, nextInPolygonDirection)) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                if (nextInPolygon(rel)) {
                    firstWay = false;
                    lastNextDirection = null;
                    previousLocationNode = locationNode;
                    return Evaluation.INCLUDE_AND_CONTINUE;
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            if (rel.isType(Relation.NEXT) && validDirection(rel, endNode, lastNextDirection)) {
                if (!validDirection(rel, endNode, nextDirection) && firstWay) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                previousLocationNode = locationNode;
                lastNextDirection = rel.getEndNode().equals(endNode) ? Direction.OUTGOING : Direction.INCOMING;
                return Evaluation.INCLUDE_AND_CONTINUE;
            }
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        private long getLocationNode(Node node) {
            return node.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNodeId();
        }

        private boolean validDirection(Relationship rel, Node endNode, Direction direction) {
            if (direction != null) {
                if (direction == Direction.OUTGOING && endNode.equals(rel.getStartNode())) {
                    return false;
                } else if (direction == Direction.INCOMING && endNode.equals(rel.getEndNode())) {
                    return false;
                }
            }
            return true;
        }

        private boolean nextInPolygon(Relationship rel) {
            return nextInPolygon(rel, relationId);
        }

        static boolean nextInPolygon(Relationship rel, long relationId) {
            long[] ids = (long[]) rel.getProperty("relation_osm_ids");

            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == relationId) {
                    return true;
                }
            }
            return false;
        }
    }

    private class TraversalException extends RuntimeException {
        public TraversalException(String message) {
        }
    }
}
