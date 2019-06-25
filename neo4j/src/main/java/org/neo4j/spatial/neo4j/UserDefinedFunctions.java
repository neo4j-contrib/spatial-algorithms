package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.spatial.Coordinate;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;
import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianMCSweepLineIntersect;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianNaiveIntersect;
import org.neo4j.spatial.algo.cartesian.CartesianWithin;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.values.storable.CoordinateReferenceSystem;
import org.neo4j.values.storable.Values;

import java.util.*;
import java.util.stream.Stream;

public class UserDefinedFunctions {

    @Context
    public Log log;

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

    @Procedure(name = "neo4j.createOSMArrayPolygon", mode = Mode.WRITE)
    public void createOSMArrayPolygon(@Name("main") Node main) {
        List<List<Node>> polystrings = OSMTraverser.traverseOSMGraph(main);

        List<Point> points = new ArrayList<>();
        for (Node wayNode : polystrings.get(0)) {
            Node node = wayNode.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();

            Point point = (Point) node.getProperty("location");
            points.add(point);
        }

        for (List<Node> polystring : polystrings) {
            StringJoiner joiner = new StringJoiner(",", "LINESTRING(", ")");
            for (Node wayNode : polystring) {
                Node node = wayNode.getSingleRelationship(Relation.NODE, Direction.OUTGOING).getEndNode();

                Point point = (Point) node.getProperty("location");
                joiner.add(point.getCoordinate().getCoordinate().get(0) + " " + point.getCoordinate().getCoordinate().get(1));
            }
        }

        main.setProperty("polygon", points.toArray(new Point[0]));
    }

    @Procedure(name = "neo4j.createArrayCache", mode = Mode.WRITE)
    public void createArrayCache(@Name("main") Node main) {
        GraphDatabaseService db = main.getGraphDatabase();

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("main", main.getId());
        Result mainResult = db.execute("MATCH (p:Polygon)<-[:POLYGON_STRUCTURE*]-(m:OSMRelation) WHERE id(m)=$main RETURN p AS polygonNode", parameters);

        long relation_osm_id = (long) main.getProperty("relation_osm_id");

        while (mainResult.hasNext()) {
            Node polygonNode = (Node) mainResult.next().get("polygonNode");

            parameters = new HashMap<>();
            parameters.put("polygonNode", polygonNode.getId());
            Result startNodeResult = db.execute("MATCH (p:Polygon)-[:POLYGON_START]->(:OSMWay)-[:FIRST_NODE]->(n:OSMWayNode) WHERE id(p)=$polygonNode RETURN n AS startNode", parameters);

            if (!startNodeResult.hasNext()) {
                return;
            }

            Node startNode = (Node) startNodeResult.next().get("startNode");

            Neo4jSimpleGraphNodePolygon polygon = new Neo4jSimpleGraphNodePolygon(startNode, relation_osm_id);

            polygonNode.setProperty("polygon", Arrays.stream(polygon.getPoints()).map(p -> Values.pointValue(CoordinateReferenceSystem.WGS84, p.getCoordinate())).toArray(Point[]::new));
        }
    }

    @Procedure(name = "neo4j.createOSMGraphPolygon", mode = Mode.WRITE)
    public void createOSMPolygon(@Name("main") Node main) {
        GraphDatabaseService db = main.getGraphDatabase();
        long id = (long) main.getProperty("relation_osm_id");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        db.execute("MATCH (m:OSMRelation)-[:POLYGON_STRUCTURE*]->(p:Polygon) WHERE m.relation_osm_id = $id DETACH DELETE p", parameters);
        //TODO fix this by deleting id from array
//        db.execute("MATCH (:OSMWayNode)-[n:NEXT_IN_POLYGON]->(:OSMWayNode)  DELETE n");

        List<List<Node>> polystrings = OSMTraverser.traverseOSMGraph(main);

        GraphPolygonBuilder graphPolygonBuilder = new GraphPolygonBuilder(main, polystrings);
        graphPolygonBuilder.build();
    }

    private MultiPolygon getGraphNodePolygon(Node main) {
        long relationId = (long) main.getProperty("relation_osm_id");
        MultiPolygon multiPolygon = new MultiPolygon();
        insertChildrenGraphNode(main, multiPolygon, relationId);

        return multiPolygon;
    }

    @UserFunction(name = "neo4j.getGraphPolygonWKT")
    public String getGraphPolygonWKT(@Name("main") Node main) {
        return getGraphNodePolygon(main).toWKT();
    }

    private MultiPolygon getArrayPolygon(Node main) {
        MultiPolygon multiPolygon = new MultiPolygon();
        insertChildrenArray(main, multiPolygon);

        return multiPolygon;
    }

    @UserFunction(name = "neo4j.getArrayPolygonWKT")
    public String getArrayPolygonWKT(@Name("main") Node main) {
        return getArrayPolygon(main).toWKT();
    }

    private void insertChildrenGraphNode(Node node, MultiPolygon multiPolygon, long relationId) {
        for (Relationship polygonStructure : node.getRelationships(Relation.POLYGON_STRUCTURE, Direction.OUTGOING)) {
            Node child = polygonStructure.getEndNode();
            Node start = child.getSingleRelationship(Relation.POLYGON_START, Direction.OUTGOING).getEndNode().getSingleRelationship(Relation.FIRST_NODE, Direction.OUTGOING).getEndNode();

            Polygon.SimplePolygon polygon = new Neo4jSimpleGraphNodePolygon(start, relationId);
            MultiPolygon.MultiPolygonNode childNode = new MultiPolygon.MultiPolygonNode(polygon);
            multiPolygon.addChild(childNode);

            insertChildrenGraphNode(child, childNode, relationId);
        }
    }

    private void insertChildrenArray(Node node, MultiPolygon multiPolygon) {
        for (Relationship polygonStructure : node.getRelationships(Relation.POLYGON_STRUCTURE, Direction.OUTGOING)) {
            Node child = polygonStructure.getEndNode();

            Polygon.SimplePolygon polygon = Neo4jArrayToInMemoryConverter.convertToInMemoryPolygon(child);
            MultiPolygon.MultiPolygonNode childNode = new MultiPolygon.MultiPolygonNode(polygon);
            multiPolygon.addChild(childNode);

            insertChildrenArray(child, childNode);
        }
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
    public boolean withinPolygon(@Name("point") Point point, @Name("polygon") List<Point> polygon) {
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
                Polygon.SimplePolygon geometry = Polygon.simple(asPoints(polygon));
                return CartesianWithin.within(geometry, asPoint(point));
            }
        }
    }

    @UserFunction("neo4j.convexHullPoints")
    public List<Point> convexHullPoints(@Name("points") List<Point> points) {
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(asPoints(points));

        return asPoints(CoordinateReferenceSystem.WGS84, convexHull.getPoints());
    }

    @UserFunction("neo4j.convexHullArray")
    public List<Point> convexHullArray(@Name("main") Node main) {
        MultiPolygon multiPolygon = getArrayPolygon(main);
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(multiPolygon);

        return asPoints(CoordinateReferenceSystem.WGS84, convexHull.getPoints());
    }

    @UserFunction("neo4j.convexHullGraphNode")
    public List<Point> convexHullGraphNode(@Name("main") Node main) {
        MultiPolygon multiPolygon = getGraphNodePolygon(main);
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(multiPolygon);

        return asPoints(CoordinateReferenceSystem.WGS84, convexHull.getPoints());
    }

    @UserFunction("neo4j.naiveIntersectArray")
    public List<Point> naiveIntersectArray(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        validatePolygons(polygon1, polygon2);

        Polygon.SimplePolygon convertedPolygon1 = getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = getSimplePolygon(polygon2);

        org.neo4j.spatial.core.Point[] intersections = new CartesianNaiveIntersect().intersect(convertedPolygon1, convertedPolygon2);
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

        org.neo4j.spatial.core.Point[] intersections = new CartesianMCSweepLineIntersect().intersect(convertedPolygon1, convertedPolygon2);
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
        org.neo4j.spatial.core.CRS crs = CRSConverter.toInMemoryCRS(point.getCRS());
        return org.neo4j.spatial.core.Point.point(crs, coords);
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
