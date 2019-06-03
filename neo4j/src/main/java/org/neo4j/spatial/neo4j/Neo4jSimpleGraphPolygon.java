package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.Arrays;

import static java.lang.String.format;

public abstract class Neo4jSimpleGraphPolygon implements Polygon.SimplePolygon {
    Point[] points;
    private long osmRelationId;
    private ResourceIterator<Node> nodeIterator;
    boolean traversing;
    Node pointer;
    Node start;

    public Neo4jSimpleGraphPolygon(Node main, long osmRelationId) {
        this.osmRelationId = osmRelationId;
        this.traversing = false;
        this.pointer = null;
        this.start = main;
    }

    @Override
    public int dimension() {
        return this.points[0].dimension();
    }

    @Override
    public Point[] getPoints() {
        return points;
    }

    @Override
    public boolean isSimple() {
        return true;
    }

    @Override
    public String toString() {
        return format("Neo4jSimpleGraphNodePolygon%s", Arrays.toString(points));
    }

    private Traverser getNewTraverser(Node start) {
        return new MonoDirectionalTraversalDescription()
                .depthFirst()
                .relationships(Relation.NEXT, Direction.BOTH)
                .relationships(Relation.NEXT_IN_POLYGON, Direction.OUTGOING)
                .evaluator(new WayEvaluator(osmRelationId)).traverse(start);
    }

    @Override
    public boolean fullyTraversed() {
        if (this.nodeIterator != null) {
            return !this.nodeIterator.hasNext() && this.traversing;
        }
        return false;
    }

    @Override
    public void startTraversal(Point point) {
        ResourceIterator<Node> iterator = getNewTraverser(start).nodes().iterator();

        while (iterator.hasNext()) {
            Node next = iterator.next();
            Point extracted = extractPoint(next);

            if (extracted.equals(point)) {
                this.start = next;
                break;
            }
        }

        this.traversing = false;
        getNewTraverser(this.start);
    }

    abstract Point extractPoint(Node node);

    Node getNextNode(Node node) {
        if (this.nodeIterator == null || !this.nodeIterator.hasNext()) {
            this.nodeIterator = getNewTraverser(node).nodes().iterator();
        }

        return this.nodeIterator.next();
    }

    protected Node[] traverseGraph(Node main) {
        return getNewTraverser(main).nodes().stream().toArray(Node[]::new);
    }

    private static class WayEvaluator implements Evaluator {
        private long relationId;

        public WayEvaluator(long relationId) {
            this.relationId = relationId;
        }

        @Override
        public Evaluation evaluate(Path path) {
            Relationship rel = path.lastRelationship();
            Node node = path.endNode();

            if (rel == null) {
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            if (rel.isType(Relation.NEXT_IN_POLYGON)) {
                return nextInPolygon(rel) ? Evaluation.INCLUDE_AND_CONTINUE : Evaluation.EXCLUDE_AND_PRUNE;
            }

            if (rel.isType(Relation.NEXT)) {
                for (Relationship relationship : node.getRelationships(Relation.NEXT)) {
                    if (!relationship.equals(rel)) {
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                for (Relationship relationship : node.getRelationships(Relation.NEXT_IN_POLYGON, Direction.OUTGOING)) {
                    if (nextInPolygon(relationship)) {
                        return Evaluation.INCLUDE_AND_CONTINUE;
                    }
                }

                Node oneButLast = path.lastRelationship().getOtherNode(node);

                for (Relationship relationship : oneButLast.getRelationships(Relation.NEXT_IN_POLYGON, Direction.INCOMING)) {
                    if (nextInPolygon(relationship)) {
                        return Evaluation.EXCLUDE_AND_PRUNE;
                    }
                }
                return Evaluation.INCLUDE_AND_CONTINUE;
            }

            return Evaluation.EXCLUDE_AND_PRUNE;
        }

        private boolean nextInPolygon(Relationship rel) {
            long[] ids = (long[]) rel.getProperty("relation_osm_ids");

            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == relationId) {
                    return true;
                }
            }
            return false;
        }
    }

    protected void assertAllSameDimension(Point... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].dimension() != points[i].dimension()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].dimension(), points[0].dimension()));
            }
        }
    }
}
