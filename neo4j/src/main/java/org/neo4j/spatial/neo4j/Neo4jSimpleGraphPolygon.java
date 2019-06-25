package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.helpers.collection.Pair;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
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
    Node pointer;
    Node start;
    Node main;
    Point startPoint;

    public Neo4jSimpleGraphPolygon(Node main, long osmRelationId) {
        this.osmRelationId = osmRelationId;
        this.traversing = false;
        this.pointer = null;
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
                .relationships(Relation.NEXT_IN_POLYGON, Direction.OUTGOING)
                .uniqueness(Uniqueness.NONE)
                .evaluator(new WayEvaluator(osmRelationId)).traverse(start);
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

        double minDistance = Double.MAX_VALUE;
        while (iterator.hasNext()) {
            Node next = iterator.next();
            Point extracted = extractPoint(next);

            double currentDistance = DistanceCalculator.distance(extracted, startPoint);
            if (currentDistance <= minDistance) {
                minDistance = currentDistance;
                this.start = next;
                this.pointer = next;
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

        for (Relationship relationship : this.start.getRelationships(Relation.NEXT_IN_POLYGON)) {
            if (WayEvaluator.nextInPolygon(relationship, osmRelationId)) {
                Node other = relationship.getOtherNode(this.start);

                double currentDistance = DistanceCalculator.distance(directionPoint, extractPoint(other));
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    minDirection = relationship.getStartNode().equals(this.start) ? Direction.OUTGOING : Direction.INCOMING;
                }
            }
        }

        for (Relationship relationship : this.start.getRelationships(Relation.NEXT)) {
            Node other = relationship.getOtherNode(this.start);

            double currentDistance = DistanceCalculator.distance(directionPoint, extractPoint(other));
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
        this.pointer = main;
        this.traversing = false;
        this.nodeIterator = getNewTraverser(this.start).nodes().iterator();
    }

    abstract Point extractPoint(Node node);

    Node getNextNode(Node node) {
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
        private boolean withDirection;
        private boolean firstWay;

        private Node firstNode;
        private Node secondNode;
        private boolean finished;
        private Direction lastNextDirection;

        public WayEvaluator(long relationId) {
            this.relationId = relationId;
            this.withDirection = false;
            this.finished = false;
        }

        public WayEvaluator(long relationId, Direction nextDirection, Direction nextInPolygonDirection) {
            this.relationId = relationId;
            this.nextDirection = nextDirection;
            this.nextInPolygonDirection = nextInPolygonDirection;
            this.withDirection = true;
            this.firstWay = true;
            this.finished = false;
        }

        @Override
        public Evaluation evaluate(Path path) {
            if (withDirection) {
                return withDirection(path);
            } else {
                return withoutDirection(path);
            }
        }

        private Evaluation withDirection(Path path) {
            if (finished) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            Relationship rel = path.lastRelationship();
            Node endNode = path.endNode();

            if (path.length() == 1) {
                if (rel.isType(Relation.NEXT) && nextInPolygonDirection != null) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                } else if (rel.isType(Relation.NEXT_IN_POLYGON) && nextDirection != null) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            if (endNode.equals(firstNode) || endNode.equals(secondNode)) {
                finished = true;
                return Evaluation.INCLUDE_AND_PRUNE;
            }

            if (firstNode == null) {
                firstNode = endNode;
            } else if (secondNode == null) {
                secondNode = endNode;
            }

            if (rel == null) {
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            if (rel.isType(Relation.NEXT_IN_POLYGON)) {
                if (!validDirection(rel, endNode, nextInPolygonDirection)) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                if (!onlyOption(rel.getOtherNode(endNode))) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                if (nextInPolygon(rel)) {
                    firstWay = false;
                    lastNextDirection = null;
                    return Evaluation.INCLUDE_AND_CONTINUE;
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            if (rel.isType(Relation.NEXT) && validDirection(rel, endNode, lastNextDirection)) {
                if (!validDirection(rel, endNode, nextDirection) && firstWay) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                for (Relationship relationship : endNode.getRelationships(Relation.NEXT)) {
                    if (!relationship.equals(rel)) {
                        lastNextDirection = rel.getEndNode().equals(endNode) ? Direction.OUTGOING : Direction.INCOMING;;
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                for (Relationship relationship : endNode.getRelationships(Relation.NEXT_IN_POLYGON, Direction.OUTGOING)) {
                    if (nextInPolygon(relationship)) {
                        lastNextDirection = rel.getEndNode().equals(endNode) ? Direction.OUTGOING : Direction.INCOMING;;
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                Node oneButLast = path.lastRelationship().getOtherNode(endNode);

                for (Relationship relationship : oneButLast.getRelationships(Relation.NEXT_IN_POLYGON, Direction.INCOMING)) {
                    if (nextInPolygon(relationship)) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                }
                lastNextDirection = rel.getEndNode().equals(endNode) ? Direction.OUTGOING : Direction.INCOMING;;
                return Evaluation.INCLUDE_AND_CONTINUE;
            }
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        private boolean onlyOption(Node node) {
            if (nextDirection != null) {
                Iterator<Relationship> firstStep = node.getRelationships(Relation.NEXT, nextDirection).iterator();
                if (firstStep.hasNext()) {
                    Node oneStep = firstStep.next().getOtherNode(node);
                    Iterator<Relationship> secondStep = oneStep.getRelationships(Relation.NEXT, nextDirection).iterator();
                    if (secondStep.hasNext()) {
                        return false;
                    }
                }
            }
            return true;
        }

        private Evaluation withoutDirection(Path path) {
            if (finished) {
                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            Relationship rel = path.lastRelationship();
            Node endNode = path.endNode();

            if (endNode.equals(firstNode) || endNode.equals(secondNode)) {
                finished = true;
                return Evaluation.EXCLUDE_AND_PRUNE;
            }

            if (firstNode == null) {
                firstNode = endNode;
            } else if (secondNode == null) {
                secondNode = endNode;
            }

            if (rel == null) {
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            if (rel.isType(Relation.NEXT_IN_POLYGON)) {
                return nextInPolygon(rel) ? Evaluation.INCLUDE_AND_CONTINUE : Evaluation.EXCLUDE_AND_PRUNE;
            }

            if (rel.isType(Relation.NEXT)) {
                for (Relationship relationship : endNode.getRelationships(Relation.NEXT)) {
                    if (!relationship.equals(rel)) {
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                for (Relationship relationship : endNode.getRelationships(Relation.NEXT_IN_POLYGON, Direction.OUTGOING)) {
                    if (nextInPolygon(relationship)) {
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                Node oneButLast = path.lastRelationship().getOtherNode(endNode);

                for (Relationship relationship : oneButLast.getRelationships(Relation.NEXT_IN_POLYGON, Direction.INCOMING)) {
                    if (nextInPolygon(relationship)) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                }
                return Evaluation.INCLUDE_AND_CONTINUE;
            }
            return Evaluation.EXCLUDE_AND_PRUNE;
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
