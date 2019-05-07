package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.spatial.Coordinate;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.procedure.Mode;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.UserFunction;
import org.neo4j.spatial.algo.ConvexHull;
import org.neo4j.spatial.algo.Intersect.MCSweepLineIntersect;
import org.neo4j.spatial.algo.Intersect.NaiveIntersect;
import org.neo4j.spatial.algo.Within;
import org.neo4j.spatial.core.Polygon;

import java.util.*;
import java.util.stream.Stream;

public class UserDefinedFunctions {

    @Procedure("neo4j.polygon")
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

    @Procedure(name = "neo4j.OSMPolygon", mode = Mode.WRITE)
    public void makeOSMPolygon(@Name("main") Node main) {
        GraphDatabaseService db = main.getGraphDatabase();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("main", main);
        Result result = db.execute(
                "WITH $main as r " +
                "MATCH (r)-[:MEMBER]->(n:OSMWay) " +
                "WITH reverse(collect(n)) as ways " +
                "UNWIND ways as n " +
                "MATCH (n)-[:FIRST_NODE]->(a:OSMWayNode)-[:NEXT*0..]->(:OSMWayNode)-[:NODE]->(x:OSMNode) " +
                "WITH collect(id(x)) as nodes " +
                "UNWIND reduce(a=[last(nodes)], x in nodes | CASE WHEN x=last(a) THEN a ELSE a+x END) as x " +
                "MATCH (n) WHERE id(n)=x " +
                "RETURN collect(n.location) as locations", parameters);

        List<Point> locations = (List<Point>) result.next().get("locations");

        Label label = Label.label("POLYGON");
        RelationshipType next = RelationshipType.withName("NEXT");

        Node previous = db.createNode(label);
        previous.setProperty("location", locations.get(0));

        main.createRelationshipTo(previous, RelationshipType.withName("START"));

        for (int i = 1; i < locations.size(); i++) {
            Node node = db.createNode(label);
            node.setProperty("location", locations.get(i));
            previous.createRelationshipTo(node, next);

            previous = node;
        }

        result.close();
    }

    @UserFunction("neo4j.boundingBoxFor")
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

    @UserFunction("neo4j.withinPolygon")
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

    @UserFunction("neo4j.convexHullArray")
    public List<Point> convexHull(@Name("polygon") List<Point> polygon) {
        if (polygon == null) {
            throw new IllegalArgumentException("Invalid 'polygon', 'polygon' was not defined");
        } else if (polygon.size() < 3) {
            throw new IllegalArgumentException("Invalid 'polygon', should be a list of at least 3, but was: " + polygon.size());
        }

        CRS crs = polygon.get(0).getCRS();

        org.neo4j.spatial.core.Point[] convertedPoints = asPoints(polygon);
        Polygon.SimplePolygon convexHull = ConvexHull.convexHull(Polygon.simple(convertedPoints));

        return asPoints(crs, convexHull.getPoints());
    }

    @UserFunction("neo4j.convexHullGraph")
    public List<Point> convexHull(@Name("polygonNode") Node polygonNode, @Name("locationProperty") String locationProperty, @Name("relationStart") String relationStart, @Name("relationNext") String relationNext) {
        Neo4jSimpleGraphPolygon polygon = new Neo4jSimpleGraphPolygon(polygonNode, locationProperty, relationStart, relationNext);

        Polygon.SimplePolygon convexHull = ConvexHull.convexHull(polygon);
        return asPoints(polygon.getCRS(), convexHull.getPoints());
    }

    @UserFunction("neo4j.naiveIntersectArray")
    public List<Point> naiveIntersectArray(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        validatePolygons(polygon1, polygon2);

        Polygon.SimplePolygon convertedPolygon1 = getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = getSimplePolygon(polygon2);

        org.neo4j.spatial.core.Point[] intersections = new NaiveIntersect().intersect(convertedPolygon1, convertedPolygon2);
        return asPoints(polygon1.get(0).getCRS(), intersections);
    }

    private Polygon.SimplePolygon getSimplePolygon(@Name("polygon1") List<Point> polygon1) {
        org.neo4j.spatial.core.Point[] convertedPoints1 = asPoints(polygon1);
        return Polygon.simple(convertedPoints1);
    }

    @UserFunction("neo4j.MCSweepLineIntersectArray")
    public List<Point> MCSweepLineIntersectArray(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        validatePolygons(polygon1, polygon2);

        Polygon.SimplePolygon convertedPolygon1 = getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = getSimplePolygon(polygon2);

        org.neo4j.spatial.core.Point[] intersections = new MCSweepLineIntersect().intersect(convertedPolygon1, convertedPolygon2);
        return asPoints(polygon1.get(0).getCRS(), intersections);
    }

    private void validatePolygons(List<Point> polygon1, List<Point> polygon2) {
        if (polygon1 == null) {
            throw new IllegalArgumentException("Invalid 'polygon1', 'polygon1' was not defined");
        } else if (polygon1.size() < 3) {
            throw new IllegalArgumentException("Invalid 'polygon1', should be a list of at least 3, but was: " + polygon1.size());
        } else if (polygon2 == null) {
            throw new IllegalArgumentException("Invalid 'polygon2', 'polygon2' was not defined");
        } else if (polygon2.size() < 3) {
            throw new IllegalArgumentException("Invalid 'polygon2', should be a list of at least 3, but was: " + polygon2.size());
        }

        CRS CRS1 = polygon1.get(0).getCRS();
        CRS CRS2 = polygon2.get(0).getCRS();
        if (!CRS1.equals(CRS2)) {
            throw new IllegalArgumentException("Cannot compare geometries of different CRS: " + CRS1 + " !+ " + CRS2);
        }
    }


    private org.neo4j.spatial.core.Point[] asPoints(List<Point> polygon) {
        org.neo4j.spatial.core.Point[] points = new org.neo4j.spatial.core.Point[polygon.size()];
        for (int i = 0; i < points.length; i++) {
            points[i] = asPoint(polygon.get(i));
        }
        return points;
    }

    private org.neo4j.spatial.core.Point asPoint(Point point) {
        List<Double> coordinates = point.getCoordinate().getCoordinate();
        double[] coords = new double[coordinates.size()];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = coordinates.get(i);
        }
        return org.neo4j.spatial.core.Point.point(coords);
    }

    private List<Point> asPoints(CRS crs, org.neo4j.spatial.core.Point[] points) {
        List<Point> converted = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            converted.add(asPoint(crs, points[i]));
        }
        return converted;
    }

    private Point asPoint(CRS crs, org.neo4j.spatial.core.Point point) {
        return new Neo4jPoint(crs, new Coordinate(point.getCoordinate()));
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
