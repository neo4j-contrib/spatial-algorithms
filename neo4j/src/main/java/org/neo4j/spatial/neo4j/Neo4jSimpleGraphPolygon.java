package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.traversal.Evaluation;
import org.neo4j.graphdb.traversal.Evaluator;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.impl.traversal.MonoDirectionalTraversalDescription;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.PolygonUtil;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class Neo4jSimpleGraphPolygon implements Polygon.SimplePolygon {
    private final Neo4jPoint[] points;

    public Neo4jSimpleGraphPolygon(Node main, String property, long osmRelationId, RelationshipCombination[] relationNext) {
        Neo4jPoint[] unclosed = traverseGraph(main, property, osmRelationId, relationNext);
        this.points = new PolygonUtil<Neo4jPoint>().closeRing(unclosed);
        if (points.length < 4) {
            throw new IllegalArgumentException("Polygon cannot have less than 4 points");
        }
        assertAllSameDimension(this.points);
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
        return format("Neo4jSimpleGraphPolygon%s", Arrays.toString(points));
    }


    @Override
    public String toWKTPointString() {
        StringJoiner joiner = new StringJoiner(",", "(", ")");
        for (Point point : getPoints()) {
            joiner.add(point.getCoordinate()[0] + " " + point.getCoordinate()[1]);
        }
        return joiner.toString();
    }

    public CRS getCRS() {
        return this.points[0].getCRS();
    }

    private Neo4jPoint[] traverseGraph(Node main, String property, long osmRelationId, RelationshipCombination[] relationNext) {
        RelationshipType nodeRel = RelationshipType.withName("NODE");

        TraversalDescription traversalDescription = new MonoDirectionalTraversalDescription()
                .depthFirst()
                .evaluator(new WayEvaluator(osmRelationId));


        for (RelationshipCombination combination : relationNext) {
            traversalDescription = traversalDescription.relationships(combination.getType(), combination.getDirection());
        }

        List<Node> wayNodes = traversalDescription.traverse(main).nodes().stream().collect(Collectors.toList());
        Neo4jPoint[] points = new Neo4jPoint[wayNodes.size()];
        for (int i = 0; i < points.length; i++) {
            Node node = wayNodes.get(i).getSingleRelationship(nodeRel, Direction.OUTGOING).getEndNode();
            points[i] = new Neo4jPoint(node, property);
        }

        return points;
    }

    private static class WayEvaluator implements Evaluator {
        private static final RelationshipType NEXT_IN_POLYGON = RelationshipType.withName("NEXT_IN_POLYGON");
        private static final RelationshipType NEXT = RelationshipType.withName("NEXT");

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

            if (rel.isType(NEXT_IN_POLYGON)) {
                return nextInPolygon(rel) ? Evaluation.INCLUDE_AND_CONTINUE : Evaluation.EXCLUDE_AND_PRUNE;
            }

            if (rel.isType(NEXT)) {
                boolean moreRelations = false;
                for (Relationship relationship : node.getRelationships(NEXT)) {
                    if (!relationship.equals(rel)) {
                        moreRelations = true;
                    }
                }

                for (Relationship relationship : node.getRelationships(NEXT_IN_POLYGON, Direction.OUTGOING)) {
                    moreRelations = true;
                }

                if (!moreRelations) {
                    return Evaluation.EXCLUDE_AND_PRUNE;
                }


                Node oneButLast = path.lastRelationship().getOtherNode(node);

                for (Relationship relationship : oneButLast.getRelationships(NEXT_IN_POLYGON, Direction.INCOMING)) {
                    if (nextInPolygon(relationship) && !moreRelations) {
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

    private void assertAllSameDimension(Neo4jPoint... points) {
        for (int i = 1; i < points.length; i++) {
            if (points[0].dimension() != points[i].dimension()) {
                throw new IllegalArgumentException(format("Point[%d] has different dimension to Point[%d]: %d != %d", i, 0, points[i].dimension(), points[0].dimension()));
            }
        }
    }
}
