package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.String.format;

public abstract class Neo4jSimpleGraphPolygon implements Polygon.SimplePolygon {
    protected Point[] points;

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

    @Override
    public String toWKTPointString() {
        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (Point point : getPoints()) {
            joiner.add(point.getCoordinate()[0] + " " + point.getCoordinate()[1]);
        }
        return joiner.toString();
    }

    protected Node[] traverseGraph(Node main, long osmRelationId) {
        TraversalDescription traversalDescription = new MonoDirectionalTraversalDescription()
                .depthFirst()
                .relationships(Relation.NEXT, Direction.BOTH)
                .relationships(Relation.NEXT_IN_POLYGON, Direction.OUTGOING)
                .evaluator(new WayEvaluator(osmRelationId));

        return traversalDescription.traverse(main).nodes().stream().toArray(Node[]::new);
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
