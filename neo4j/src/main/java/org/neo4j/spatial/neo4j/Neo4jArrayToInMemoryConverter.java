package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class Neo4jArrayToInMemoryConverter {
    private static final String PROPERTY = "polygon";

    public static Polygon.SimplePolygon convertToInMemory(Node node) {
        org.neo4j.graphdb.spatial.Point[] neo4jPoints = (org.neo4j.graphdb.spatial.Point[]) node.getProperty(PROPERTY);

        Point[] result = new Point[neo4jPoints.length];
        for (int i = 0; i < neo4jPoints.length; i++) {
            CRS neo4jCRS = neo4jPoints[i].getCRS();
            org.neo4j.spatial.core.CRS crs = CRSConverter.toInMemoryCRS(neo4jCRS);
            result[i] = Point.point(crs, neo4jPoints[i].getCoordinate().getCoordinate().stream().mapToDouble(d -> d).toArray());
        }

        return Polygon.simple(result);
    }
}
