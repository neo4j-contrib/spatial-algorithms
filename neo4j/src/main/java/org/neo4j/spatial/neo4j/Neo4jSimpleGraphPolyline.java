package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
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
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polyline;

import java.util.Arrays;
import java.util.Iterator;

import static java.lang.String.format;

public abstract class Neo4jSimpleGraphPolyline implements Polyline {
    private long osmRelationId;
    private ResourceIterator<Node> nodeIterator;
    boolean traversing;
    Node pointer;
    Node start;
    Node main;
    Point startPoint;

    public Neo4jSimpleGraphPolyline(Node main, long osmRelationId) {
        this.osmRelationId = osmRelationId;
        this.traversing = false;
        this.pointer = null;
        this.main = main;
        this.start = main;
    }

    @Override
    public int dimension() {
        return extractPoint(this.main).dimension();
    }

    @Override
    public String toString() {
        return format("Neo4jSimpleGraphNodePolygon%s", Arrays.toString(getPoints()));
    }

    private Traverser getNewTraverser(Node start) {
        return new MonoDirectionalTraversalDescription()
                .depthFirst()
                .relationships(Relation.NEXT, Direction.BOTH)
                .relationships(Relation.NEXT_IN_POLYLINE, Direction.OUTGOING)
                .relationships(Relation.END_OF_POLYLINE)
                .uniqueness(Uniqueness.NONE)
                .evaluator(new WayEvaluator(osmRelationId)).traverse(start);
    }

    private Traverser getNewTraverser(Node start, Relation relation, Direction direction) {
        return new MonoDirectionalTraversalDescription()
                .depthFirst()
                .relationships(Relation.NEXT, Direction.BOTH)
                .relationships(Relation.NEXT_IN_POLYLINE)
                .relationships(Relation.END_OF_POLYLINE)
                .uniqueness(Uniqueness.NONE)
                .evaluator(new WayEvaluator(osmRelationId, relation, direction)).traverse(start);
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

        double minDistance = Double.MAX_VALUE;
        while (iterator.hasNext()) {
            Node next = iterator.next();
            Point extracted = extractPoint(next);

            double currentDistance = calculator.distance(extracted, startPoint);
            if (currentDistance <= minDistance) {
                minDistance = currentDistance;
                this.start = next;
                this.pointer = next;
                this.startPoint = extracted;
            }
        }

        Pair<Relation, Direction> relationDirection = getClosestNeighborToDirection(directionPoint);
        this.nodeIterator = getNewTraverser(this.start, relationDirection.first(), relationDirection.other()).nodes().iterator();
    }

    private Pair<Relation, Direction> getClosestNeighborToDirection(Point directionPoint) {
        double minDistance = Double.MAX_VALUE;
        Direction minDirection = null;
        Relation minRelation = null;

        Distance calculator = DistanceCalculator.getCalculator(directionPoint);

        for (Relationship relationship : this.start.getRelationships(Relation.NEXT_IN_POLYLINE)) {
            if (WayEvaluator.partOfPolyline(relationship, osmRelationId)) {
                Node other = relationship.getOtherNode(this.start);

                double currentDistance = calculator.distance(directionPoint, extractPoint(other));
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    minRelation = Relation.NEXT_IN_POLYGON;
                    minDirection = relationship.getStartNode().equals(this.start) ? Direction.OUTGOING : Direction.INCOMING;
                }
            }
        }

        for (Relationship relationship : this.start.getRelationships(Relation.END_OF_POLYLINE)) {
            if (WayEvaluator.partOfPolyline(relationship, osmRelationId)) {
                Node other = relationship.getOtherNode(this.start);

                double currentDistance = calculator.distance(directionPoint, extractPoint(other));
                if (currentDistance < minDistance) {
                    minDistance = currentDistance;
                    minRelation = Relation.END_OF_POLYLINE;
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
                minRelation = Relation.NEXT;
            }
        }

        return Pair.of(minRelation, minDirection);
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
        private Relation relation;
        private Direction direction;
        private boolean withDirection;
        private boolean firstWay;

        private Direction lastNextDirection;

        public WayEvaluator(long relationId) {
            this.relationId = relationId;
            this.withDirection = false;
        }

        public WayEvaluator(long relationId, Relation relation, Direction direction) {
            this.relationId = relationId;
            this.relation = relation;
            this.direction = direction;
            this.withDirection = true;
            this.firstWay = true;
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
            Relationship rel = path.lastRelationship();
            Node endNode = path.endNode();

            if (path.length() == 1) {
                if (!(rel.isType(this.relation) && validDirection(rel, endNode, this.direction))) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            if (rel == null) {
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            if (rel.isType(Relation.NEXT_IN_POLYLINE)) {
                if (!validDirection(rel, endNode, direction)) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                if (!onlyOption(rel.getOtherNode(endNode))) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                if (partOfPolyline(rel)) {
                    firstWay = false;
                    lastNextDirection = null;
                    return Evaluation.INCLUDE_AND_CONTINUE;
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            if (rel.isType(Relation.END_OF_POLYLINE)) {
                if (!validDirection(rel, endNode, direction)) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                if (!onlyOption(rel.getOtherNode(endNode))) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                if (partOfPolyline(rel)) {
                    firstWay = false;
                    lastNextDirection = null;
                    return Evaluation.INCLUDE_AND_CONTINUE;
                } else {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }
            }

            if (rel.isType(Relation.NEXT) && validDirection(rel, endNode, lastNextDirection)) {
                if (!validDirection(rel, endNode, direction) && firstWay) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }

                Node oneButLast = path.lastRelationship().getOtherNode(endNode);

                for (Relationship relationship : endNode.getRelationships(Relation.END_OF_POLYLINE)) {
                    if (relationship.getOtherNodeId(endNode.getId()) == oneButLast.getId()) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                }

                for (Relationship relationship : endNode.getRelationships(Relation.NEXT)) {
                    if (!relationship.equals(rel)) {
                        lastNextDirection = rel.getEndNode().equals(endNode) ? Direction.OUTGOING : Direction.INCOMING;
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                for (Relationship relationship : endNode.getRelationships(Relation.NEXT_IN_POLYLINE, Direction.OUTGOING)) {
                    if (partOfPolyline(relationship)) {
                        lastNextDirection = rel.getEndNode().equals(endNode) ? Direction.OUTGOING : Direction.INCOMING;
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                for (Relationship relationship : oneButLast.getRelationships(Relation.NEXT_IN_POLYLINE, Direction.INCOMING)) {
                    if (partOfPolyline(relationship)) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                }
                lastNextDirection = rel.getEndNode().equals(endNode) ? Direction.OUTGOING : Direction.INCOMING;;
                return Evaluation.INCLUDE_AND_CONTINUE;
            }
            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        private boolean onlyOption(Node node) {
            if (this.relation == Relation.NEXT) {
                Iterator<Relationship> firstStep = node.getRelationships(Relation.NEXT, direction).iterator();
                if (firstStep.hasNext()) {
                    Node oneStep = firstStep.next().getOtherNode(node);
                    Iterator<Relationship> secondStep = oneStep.getRelationships(Relation.NEXT, direction).iterator();
                    if (secondStep.hasNext()) {
                        return false;
                    }
                }
            }
            return true;
        }

        private Evaluation withoutDirection(Path path) {
            Relationship rel = path.lastRelationship();
            Node endNode = path.endNode();

            if (rel == null) {
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            if (rel.isType(Relation.NEXT_IN_POLYLINE) || rel.isType(Relation.END_OF_POLYLINE)) {
                return partOfPolyline(rel) ? Evaluation.INCLUDE_AND_CONTINUE : Evaluation.EXCLUDE_AND_PRUNE;
            }

            if (rel.isType(Relation.NEXT)) {
                Node oneButLast = path.lastRelationship().getOtherNode(endNode);

                for (Relationship relationship : endNode.getRelationships(Relation.END_OF_POLYLINE)) {
                    if (relationship.getOtherNodeId(endNode.getId()) == oneButLast.getId()) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                }

                for (Relationship relationship : endNode.getRelationships(Relation.NEXT)) {
                    if (!relationship.equals(rel)) {
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                for (Relationship relationship : endNode.getRelationships(Relation.NEXT_IN_POLYLINE, Direction.OUTGOING)) {
                    if (partOfPolyline(relationship)) {
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                for (Relationship relationship : oneButLast.getRelationships(Relation.NEXT_IN_POLYLINE, Direction.INCOMING)) {
                    if (partOfPolyline(relationship)) {
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

        private boolean partOfPolyline(Relationship rel) {
            return partOfPolyline(rel, relationId);
        }

        static boolean partOfPolyline(Relationship rel, long relationId) {
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
