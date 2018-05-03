package org.amanzi.spatial.neo4j;

import org.amanzi.spatial.algo.Within;
import org.amanzi.spatial.core.Polygon;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.spatial.Coordinate;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserFunction;
import org.neo4j.values.storable.CoordinateReferenceSystem;

import java.util.*;
import java.util.stream.Stream;

public class UserDefinedFunctions {

    @Procedure("amanzi.polygon")
    public Stream<PolygonResult> makePolygon(@Name("points") List<Point> points) {
        if (points == null || points.size() < 3) {
            throw new IllegalArgumentException("Invalid 'points', should be a list of at least 3, but was: " + (points == null ? "null" : points.size()));
        } else if (points.get(0).equals(points.get(points.size() - 1))) {
            return Stream.of(new PolygonResult(points));
        } else {
            ArrayList<Point> polygon = new ArrayList<>(points.size() + 1);
            polygon.addAll(points);
            polygon.add(points.get(0));
            return Stream.of(new PolygonResult(polygon));
        }
    }

    @UserFunction("amanzi.boundingBoxFor")
    public Map<String, Point> boundingBoxFor(@Name("polygon") List<Point> polygon) {
        if (polygon == null || polygon.size() < 4) {
            throw new IllegalArgumentException("Invalid 'polygon', should be a list of at least 4, but was: " + (polygon == null ? "null" : polygon.size()));
        } else if (!polygon.get(0).equals(polygon.get(polygon.size() - 1))) {
            throw new IllegalArgumentException("Invalid 'polygon', first and last point should be the same, but were: " + polygon.get(0) + " and " + polygon.get(polygon.size() - 1));
        } else {
            CRS crs = polygon.get(0).getCRS();
            double[] min = asPoint(polygon.get(0)).getCoordinate();
            double[] max = asPoint(polygon.get(0)).getCoordinate();
            for (Point p : polygon) {
                double[] vertex = asPoint(p).getCoordinate();
                for (int i = 0; i < vertex.length; i++) {
                    if (vertex[i] < min[i]) {
                        min[i] = vertex[i];
                    }
                    if (vertex[i] > max[i]) {
                        max[i] = vertex[i];
                    }
                }
            }
            HashMap<String, Point> bbox = new HashMap<>();
            bbox.put("min", asPoint(crs, min));
            bbox.put("max", asPoint(crs, max));
            return bbox;
        }
    }

    @UserFunction("amanzi.withinPolygon")
    public boolean withinPolygon(@Name("point") Point point, @Name("polygon") List<Point> polygon, @Name(value = "touching", defaultValue = "false") boolean touching) {
        if (polygon == null || polygon.size() < 4) {
            throw new IllegalArgumentException("Invalid 'polygon', should be a list of at least 4, but was: " + polygon.size());
        } else if (!polygon.get(0).equals(polygon.get(polygon.size() - 1))) {
            throw new IllegalArgumentException("Invalid 'polygon', first and last point should be the same, but were: " + polygon.get(0) + " and " + polygon.get(polygon.size() - 1));
        } else {
            CRS polyCrs = polygon.get(0).getCRS();
            CRS pointCrs = point.getCRS();
            if (!polyCrs.equals(pointCrs)) {
                throw new IllegalArgumentException("Cannot compare geometries of different CRS: " + polyCrs + " !+ " + pointCrs);
            } else {
                Polygon geometry = Polygon.simple(asPoints(polygon));
                return Within.within(geometry, asPoint(point), touching);
            }
        }
    }

    private org.amanzi.spatial.core.Point[] asPoints(List<Point> polygon) {
        org.amanzi.spatial.core.Point[] points = new org.amanzi.spatial.core.Point[polygon.size()];
        for (int i = 0; i < points.length; i++) {
            points[i] = asPoint(polygon.get(i));
        }
        return points;
    }

    private org.amanzi.spatial.core.Point asPoint(Point point) {
        List<Double> coordinates = point.getCoordinate().getCoordinate();
        double[] coords = new double[coordinates.size()];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = coordinates.get(i);
        }
        return new org.amanzi.spatial.core.Point(coords);
    }

    private Point asPoint(CRS crs, double[] coords) {
        return new Neo4jPoint(crs, new Coordinate(coords));
    }

    private class Neo4jPoint implements Point {
        private final List<Coordinate> coordinates;
        private final CRS crs;

        private Neo4jPoint(CRS crs, Coordinate coordinate) {
            this.crs = crs;
            this.coordinates = Arrays.asList(coordinate);
        }

        @Override
        public List<Coordinate> getCoordinates() {
            return coordinates;
        }

        @Override
        public CRS getCRS() {
            return crs;
        }
    }

    public class PolygonResult {
        public List<Point> polygon;

        private PolygonResult(List<Point> points) {
            this.polygon = points;
        }
    }
}
