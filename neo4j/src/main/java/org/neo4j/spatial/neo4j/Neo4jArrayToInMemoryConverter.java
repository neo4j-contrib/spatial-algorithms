package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;

public class Neo4jArrayToInMemoryConverter {
    private static final String POLYGON_PROPERTY = "polygon";
    private static final String POLYLINE_PROPERTY = "polyline";

    public static Polygon.SimplePolygon convertToInMemoryPolygon(Node node) {
        org.neo4j.graphdb.spatial.Point[] neo4jPoints = (org.neo4j.graphdb.spatial.Point[]) node.getProperty(POLYGON_PROPERTY);

        Point[] result = new Point[neo4jPoints.length];
        for (int i = 0; i < neo4jPoints.length; i++) {
            CRS neo4jCRS = neo4jPoints[i].getCRS();
            org.neo4j.spatial.core.CRS crs = CRSConverter.toInMemoryCRS(neo4jCRS);
            result[i] = Point.point(crs, neo4jPoints[i].getCoordinate().getCoordinate().clone());
        }

        return Polygon.simple(result);
    }

    public static Polyline convertToInMemoryPolyline(Node node) {
        org.neo4j.graphdb.spatial.Point[] neo4jPoints = (org.neo4j.graphdb.spatial.Point[]) node.getProperty(POLYLINE_PROPERTY);

        Point[] result = new Point[neo4jPoints.length];
        for (int i = 0; i < neo4jPoints.length; i++) {
            CRS neo4jCRS = neo4jPoints[i].getCRS();
            org.neo4j.spatial.core.CRS crs = CRSConverter.toInMemoryCRS(neo4jCRS);
            result[i] = Point.point(crs, neo4jPoints[i].getCoordinate().getCoordinate().clone());
        }

        return Polyline.polyline(result);
    }
}
