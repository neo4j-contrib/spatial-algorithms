package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.spatial.CRS;
import org.neo4j.graphdb.spatial.Coordinate;
import org.neo4j.graphdb.spatial.Point;
import org.neo4j.internal.helpers.collection.Pair;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;
import org.neo4j.spatial.algo.*;
import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianMCSweepLineIntersect;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianNaiveIntersect;
import org.neo4j.spatial.algo.WithinCalculator;
import org.neo4j.spatial.algo.wgs84.WGS84ConvexHull;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;
import org.neo4j.values.storable.CoordinateReferenceSystem;
import org.neo4j.values.storable.Values;

import java.util.*;
import java.util.stream.Stream;

import static org.neo4j.spatial.neo4j.CRSConverter.toNeo4jCRS;

public class UserDefinedFunctions {

    @Context
    public Log log;
    
    @Context
    public Transaction tx;

    @UserFunction("spatial.polygon")
    public List<Point> makePolygon(@Name("points") List<Point> points) {
        if (points == null || points.size() < 3) {
            throw new IllegalArgumentException("Invalid 'points', should be a list of at least 3, but was: " + (points == null ? "null" : points.size()));
        } else if (points.get(0).equals(points.get(points.size() - 1))) {
            return points;
        } else {
            ArrayList<Point> polygon = new ArrayList<>(points.size() + 1);
            polygon.addAll(points);
            polygon.add(points.get(0));
            return polygon;
        }
    }

    // TODO write tests
    @Description( "Creates a polygon as a Point[] property named 'polygon' on the node" )
    @Procedure(name = "spatial.osm.property.createPolygon", mode = Mode.WRITE)
    public Stream<PointArraySizeResult> createArrayCache(@Name("main") Node main) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("main", main.getId());
        long relation_osm_id = (long) main.getProperty("relation_osm_id");

        Result mainResult = tx.execute("MATCH (p:Polygon)<-[:POLYGON_STRUCTURE*]-(m:OSMRelation) WHERE id(m)=$main RETURN p AS polygonNode", parameters);
        if (!mainResult.hasNext()) {
            throw new IllegalArgumentException("No polygon structure found - does " + main + " really have :POLYGON_STRUCTURE relationships? Perhaps you have not run spatial.osm.graph.createPolygon(" + main + ") yet?");
        }

        List<PointArraySizeResult> result = new ArrayList<>();
        while (mainResult.hasNext()) {
            Node polygonNode = (Node) mainResult.next().get("polygonNode");

            parameters = new HashMap<>();
            parameters.put("polygonNode", polygonNode.getId());
            Result startNodeResult = tx.execute("MATCH (p:Polygon)-[:POLYGON_START]->(:OSMWay)-[:FIRST_NODE]->(n:OSMWayNode) WHERE id(p)=$polygonNode RETURN n AS startNode", parameters);

            if (!startNodeResult.hasNext()) {
                throw new IllegalArgumentException("Broken polygon structure found - polygon " + polygonNode + " is missing a ':POLYGON_START' relationship to an 'OSMWay' node");
            }

            Node startNode = (Node) startNodeResult.next().get("startNode");
            Neo4jSimpleGraphNodePolygon polygon = new Neo4jSimpleGraphNodePolygon(startNode, relation_osm_id);
            Point[] polygonPoints = Arrays.stream(polygon.getPoints()).map(p -> Values.pointValue(CoordinateReferenceSystem.WGS84, p.getCoordinate())).toArray(Point[]::new);
            result.add(new PointArraySizeResult(polygonNode.getId(), polygonPoints.length));
            polygonNode.setProperty("polygon", polygonPoints);
        }
        return result.stream();
    }

    // TODO write tests
    @Description( "Creates a polyline as a Point[] property named 'polyline' on the node" )
    @Procedure(name = "spatial.osm.property.createPolyline", mode = Mode.WRITE)
    public Stream<PointArraySizeResult> createArrayLine(@Name("main") Node main) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("main", main.getId());
        long relation_osm_id = (long) main.getProperty("relation_osm_id");

        Result mainResult = tx.execute("MATCH (p:Polyline)<-[:POLYLINE_STRUCTURE*]-(m:OSMRelation) WHERE id(m)=$main RETURN p AS polylineNode", parameters);
        if (!mainResult.hasNext()) {
            throw new IllegalArgumentException("No polyline structure found - does " + main + " really have :POLYLINE_STRUCTURE relationships? Perhaps you have not run spatial.osm.graph.createPolygon(" + main + ") yet?");
        }

        // TODO: We could stream results from this iterator with a mapping function rather than building state
        List<PointArraySizeResult> result = new ArrayList<>();
        while (mainResult.hasNext()) {
            Node polylineNode = (Node) mainResult.next().get("polylineNode");

            parameters = new HashMap<>();
            parameters.put("polylineNode", polylineNode.getId());
            Result startNodeResult = tx.execute("MATCH (p:Polyline)-[:POLYLINE_START]->(n:OSMWayNode) WHERE id(p)=$polylineNode RETURN n AS startNode", parameters);

            if (!startNodeResult.hasNext()) {
                throw new IllegalArgumentException("Broken polyline structure found - polyline " + polylineNode + " is missing a ':POLYLINE_START' relationship to an 'OSMWayNode' node");
            }

            try {
                Node startNode = (Node) startNodeResult.next().get("startNode");
                Neo4jSimpleGraphNodePolyline polyline = new Neo4jSimpleGraphNodePolyline(startNode, relation_osm_id);
                Point[] polylinePoints = Arrays.stream(polyline.getPoints()).map(p -> Values.pointValue(CoordinateReferenceSystem.WGS84, p.getCoordinate())).toArray(Point[]::new);
                result.add(new PointArraySizeResult(polylineNode.getId(), polylinePoints.length));
                polylineNode.setProperty("polyline", polylinePoints);
            } catch (Exception e) {
                log.error("Failed to create polyline at " + polylineNode + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        return result.stream();
    }

    @Procedure(name = "spatial.osm.graph.createPolygon.nodeId", mode = Mode.WRITE)
    public void createOSMGraphGeometries(
            @Name("mainId") Long mainId,
            @Name(value = "proximityThreshold", defaultValue = "250") double proximityThreshold) {
        createOSMGraphGeometries(tx.getNodeById(mainId), proximityThreshold);
    }

    @Procedure(name = "spatial.osm.graph.createPolygon", mode = Mode.WRITE)
    public void createOSMGraphGeometries(
            @Name("main") Node main,
            @Name(value = "proximityThreshold", defaultValue = "250") double proximityThreshold) {
        long id = (long) main.getProperty("relation_osm_id");

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);
        tx.execute("MATCH (m:OSMRelation)-[:POLYGON_STRUCTURE*]->(p:Polygon) WHERE m.relation_osm_id = $id DETACH DELETE p", parameters);
        tx.execute("MATCH (m:OSMRelation)-[:POLYLINE_STRUCTURE*]->(p:Polyline) WHERE m.relation_osm_id = $id DETACH DELETE p", parameters);
        //TODO fix this by deleting id from array (NEXT_IN_... & END_OF_POLYLINE)
//        tx.execute("MATCH (:OSMWayNode)-[n:NEXT_IN_POLYGON]->(:OSMWayNode) DELETE n");
//        tx.execute("MATCH (:OSMWayNode)-[n:NEXT_IN_POLYLINE]->(:OSMWayNode) DELETE n");
//        tx.execute("MATCH (:OSMWayNode)-[n:END_OF_POLYLINE]->(:OSMWayNode) DELETE n");

        Pair<List<List<Node>>, List<List<Node>>> geometries = OSMTraverser.traverseOSMGraph(tx, main, proximityThreshold);
        List<List<Node>> polygons = geometries.first();
        List<List<Node>> polylines = geometries.other();

        // TODO: Old code would build from a superset of polygons and polylines, but this new code treats them separately - Verify!
        if (!polygons.isEmpty()) {
            log.info("Building " + polygons.size() + " polygons for node " + main + " with osm-id: " + id);
            try {
                new GraphPolygonBuilder(tx, main, polygons).build();
            } catch (Exception e) {
                log.error("Failed to build polygon/polyline structures for node id=" + main.getId() + ", osm-id=" + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (!polylines.isEmpty()) {
            log.info("Building " + polylines.size() + " polylines for node " + main + " with osm-id: " + id);
            try {
                // TODO: Can we not build polygons from multiple polylines?
                new GraphPolylineBuilder(tx, main, polylines).build();
            } catch (Exception e) {
                log.error("Failed to build polygon/polyline structures for node id=" + main.getId() + ", osm-id=" + id + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static MultiPolygon getGraphNodePolygon(Node main) {
        long relationId = (long) main.getProperty("relation_osm_id");
        MultiPolygon multiPolygon = new MultiPolygon();
        insertChildrenGraphNode(main, multiPolygon, relationId);

        return multiPolygon;
    }

    // TODO write tests
    @UserFunction(name = "spatial.osm.graph.polygonAsWKT")
    public String getGraphPolygonWKT(@Name("main") Node main) {
        return getGraphNodePolygon(main).toWKT();
    }

    public static MultiPolygon getArrayPolygon(Node main) {
        MultiPolygon multiPolygon = new MultiPolygon();
        insertChildrenArray(main, multiPolygon);

        return multiPolygon;
    }

    // TODO write tests
    @UserFunction(name = "spatial.osm.property.polygonAsWKT")
    public String getArrayPolygonWKT(@Name("main") Node main) {
        return getArrayPolygon(main).toWKT();
    }

    // TODO write tests
    @UserFunction(name = "spatial.osm.property.polygonShell")
    public List<Point> getArrayPolygonShell(@Name("main") Node main) {
        org.neo4j.spatial.core.Point[] mainPoints = getArrayPolygon(main).getShell().getPoints();
        return asNeo4jPoints(toNeo4jCRS(mainPoints[0].getCRS()), mainPoints);
    }

    // TODO write tests
    @UserFunction(name = "spatial.osm.graph.polygonShell")
    public List<Point> getGraphPolygonShell(@Name("main") Node main) {
        org.neo4j.spatial.core.Point[] mainPoints = getGraphNodePolygon(main).getShell().getPoints();
        return asNeo4jPoints(toNeo4jCRS(mainPoints[0].getCRS()), mainPoints);
    }

    public static MultiPolyline getArrayPolyline(Node main) {
        long relationId = (long) main.getProperty("relation_osm_id");
        MultiPolyline multiPolyline = new MultiPolyline();

        for (Relationship relationship : main.getRelationships(Direction.OUTGOING, Relation.POLYLINE_STRUCTURE)) {
            Node start = relationship.getEndNode();
            Polyline polyline = Neo4jArrayToInMemoryConverter.convertToInMemoryPolyline(start);
            multiPolyline.insertPolyline(polyline);
        }

        return multiPolyline;
    }

    public static MultiPolyline getGraphNodePolyline(Node main) {
        long relationId = (long) main.getProperty("relation_osm_id");
        MultiPolyline multiPolyline = new MultiPolyline();

        for (Relationship relationship : main.getRelationships(Direction.OUTGOING, Relation.POLYLINE_STRUCTURE)) {
            Node start = relationship.getEndNode().getSingleRelationship(Relation.POLYLINE_START, Direction.OUTGOING).getEndNode();
            Polyline polyline = new Neo4jSimpleGraphNodePolyline(start, relationId);
            multiPolyline.insertPolyline(polyline);
        }

        return multiPolyline;
    }

    @UserFunction(name = "spatial.osm.graph.polylineAsWKT")
    public String getGraphPolylineWKT(@Name("main") Node main) {
        return getGraphNodePolyline(main).toWKT();
    }

    public static void insertChildrenGraphNode(Node node, MultiPolygon multiPolygon, long relationId) {
        for (Relationship polygonStructure : node.getRelationships(Direction.OUTGOING, Relation.POLYGON_STRUCTURE)) {
            Node child = polygonStructure.getEndNode();
            Node start = child.getSingleRelationship(Relation.POLYGON_START, Direction.OUTGOING).getEndNode().getSingleRelationship(Relation.FIRST_NODE, Direction.OUTGOING).getEndNode();

            Polygon.SimplePolygon polygon = new Neo4jSimpleGraphNodePolygon(start, relationId);
            MultiPolygon.MultiPolygonNode childNode = new MultiPolygon.MultiPolygonNode(polygon);
            multiPolygon.addChild(childNode);

            insertChildrenGraphNode(child, childNode, relationId);
        }
    }

    public static void insertChildrenArray(Node node, MultiPolygon multiPolygon) {
        for (Relationship polygonStructure : node.getRelationships(Direction.OUTGOING, Relation.POLYGON_STRUCTURE)) {
            Node child = polygonStructure.getEndNode();

            Polygon.SimplePolygon polygon = Neo4jArrayToInMemoryConverter.convertToInMemoryPolygon(child);
            MultiPolygon.MultiPolygonNode childNode = new MultiPolygon.MultiPolygonNode(polygon);
            multiPolygon.addChild(childNode);

            insertChildrenArray(child, childNode);
        }
    }

    /*
    spatial.algo.intersection
    spatial.algo.property.intersection

    spatial.osm.graph.createGeometry
    spatial.osm.property.createGeometry

    -lower priority
    spatial.osm.graph.deleteGeometry
    spatial.osm.property.deleteGeometry
     */
    // TODO write tests
    @Procedure("spatial.osm.graph.intersection")
    public Stream<PointResult> intersectionGraphPolygonPolyline(@Name("polygonMain") Node polygonMain, @Name("polylineMain") Node polylineMain, @Name("variant") String variantString) {
        IntersectCalculator.AlgorithmVariant variant;
        if (variantString.equals("Naive")) {
            variant = IntersectCalculator.AlgorithmVariant.Naive;
        } else if (variantString.equals("MCSweepLine")) {
            variant = IntersectCalculator.AlgorithmVariant.MCSweepLine;
        } else {
            throw new IllegalArgumentException("Illegal algorithm variant. Choose 'Naive' or 'MCSweepLine'");
        }

        List<org.neo4j.spatial.core.Point> result = new ArrayList<>();
        Polygon polygon = getGraphNodePolygon(polygonMain);
        MultiPolyline multiPolyline = getGraphNodePolyline(polylineMain);

        Intersect calculator = IntersectCalculator.getCalculator(polygon, variant);

        for (Polyline polyline : multiPolyline.getChildren()) {
            Collections.addAll(result, calculator.intersect(polygon, polyline));
        }
        return result.stream().map(a -> new PointResult(asNeo4jPoint(a)));
    }

    @UserFunction("spatial.boundingBox")
    public Map<String, Point> boundingBoxFor(@Name("polygon") List<Point> polygon) {
        if (polygon == null || polygon.size() < 4) {
            throw new IllegalArgumentException("Invalid 'polygon', should be a list of at least 4, but was: " + (polygon == null ? "null" : polygon.size()));
        } else if (!polygon.get(0).equals(polygon.get(polygon.size() - 1))) {
            throw new IllegalArgumentException("Invalid 'polygon', first and last point should be the same, but were: " + polygon.get(0) + " and " + polygon.get(polygon.size() - 1));
        } else {
            CRS crs = polygon.get(0).getCRS();
            double[] min = asInMemoryPoint(polygon.get(0)).getCoordinate();
            double[] max = asInMemoryPoint(polygon.get(0)).getCoordinate();
            for (Point p : polygon) {
                double[] vertex = asInMemoryPoint(p).getCoordinate();
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
            bbox.put("min", asNeo4jPoint(crs, min));
            bbox.put("max", asNeo4jPoint(crs, max));
            return bbox;
        }
    }

    @UserFunction("spatial.algo.withinPolygon")
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
                Polygon.SimplePolygon geometry = Polygon.simple(asInMemoryPoints(polygon));
                return WithinCalculator.within(geometry, asInMemoryPoint(point));
            }
        }
    }

    @UserFunction("spatial.algo.convexHull")
    public List<Point> convexHullPoints(@Name("points") List<Point> points) {
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(asInMemoryPoints(points));

        return asNeo4jPoints(CoordinateReferenceSystem.WGS84, convexHull.getPoints());
    }

    // TODO: write tests
    @UserFunction("spatial.algo.property.convexHull")
    public List<Point> convexHullArray(@Name("main") Node main) {
        MultiPolygon multiPolygon = getArrayPolygon(main);
        Polygon.SimplePolygon convexHull = CartesianConvexHull.convexHull(multiPolygon);

        return asNeo4jPoints(CoordinateReferenceSystem.WGS84, convexHull.getPoints());
    }

    // TODO: write tests
    @UserFunction("spatial.algo.graph.convexHull")
    public List<Point> convexHullGraphNode(@Name("main") Node main) {
        MultiPolygon multiPolygon = getGraphNodePolygon(main);
        Polygon.SimplePolygon convexHull = WGS84ConvexHull.convexHull(multiPolygon);

        return asNeo4jPoints(CoordinateReferenceSystem.WGS84, convexHull.getPoints());
    }

    @UserFunction("spatial.algo.area")
    public double area(@Name("polygon") List<Point> polygon) {
        Polygon.SimplePolygon convertedPolygon = getSimplePolygon(polygon);
        Area area = AreaCalculator.getCalculator(convertedPolygon);
        return area.area(convertedPolygon);
    }

    @UserFunction("spatial.algo.distance")
    public double distance(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        Polygon.SimplePolygon convertedPolygon1 = getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = getSimplePolygon(polygon2);

        Distance distance = DistanceCalculator.getCalculator(convertedPolygon1);
        return distance.distance(convertedPolygon1, convertedPolygon2);
    }

    @UserFunction("spatial.algo.distance.ends")
    public Map<String, Object> distanceAndEndPoints(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        try {
            Polygon.SimplePolygon convertedPolygon1 = getSimplePolygon(polygon1);
            Polygon.SimplePolygon convertedPolygon2 = getSimplePolygon(polygon2);
            final CRS crs = polygon1.get(0).getCRS();

            Distance distance = DistanceCalculator.getCalculator(convertedPolygon1);
            Distance.DistanceResult dae = distance.distanceAndEndpoints(convertedPolygon1, convertedPolygon2);
            return dae.asMap(p -> asNeo4jPoint(crs, p));
        } catch (Exception e) {
            System.out.println("Failed to calculate polygon distance: " + e.getMessage());
            e.printStackTrace();
            return Distance.DistanceResult.NO_RESULT.withError(e).asMap();
        }
    }

    @UserFunction("spatial.algo.convexHull.distance")
    public double convexHullDistance(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        Polygon.SimplePolygon convexHull1 = CartesianConvexHull.convexHull(asInMemoryPoints(polygon1));
        Polygon.SimplePolygon convexHull2 = CartesianConvexHull.convexHull(asInMemoryPoints(polygon2));

        Distance distance = DistanceCalculator.getCalculator(convexHull1);
        return distance.distance(convexHull1, convexHull2);
    }

    @UserFunction("spatial.algo.convexHull.distance.ends")
    public Map<String, Object> convexHullDistanceAndEndPoints(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        try {
            Polygon.SimplePolygon convexHull1 = CartesianConvexHull.convexHull(asInMemoryPoints(polygon1));
            Polygon.SimplePolygon convexHull2 = CartesianConvexHull.convexHull(asInMemoryPoints(polygon2));
            final CRS crs = polygon1.get(0).getCRS();

            Distance distance = DistanceCalculator.getCalculator(convexHull1);
            Distance.DistanceResult dae = distance.distanceAndEndpoints(convexHull1, convexHull2);
            return dae.asMap(p -> asNeo4jPoint(crs, p));
        } catch (Exception e) {
            System.out.println("Failed to calculate polygon distance: " + e.getMessage());
            e.printStackTrace();
            return Distance.DistanceResult.NO_RESULT.withError(e).asMap();
        }
    }

    // TODO write tests
    @UserFunction("spatial.algo.intersection")
    public List<Point> naiveIntersectArray(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        validatePolygons(polygon1, polygon2);

        Polygon.SimplePolygon convertedPolygon1 = getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = getSimplePolygon(polygon2);

        org.neo4j.spatial.core.Point[] intersections = new CartesianNaiveIntersect().intersect(convertedPolygon1, convertedPolygon2);
        return asNeo4jPoints(polygon1.get(0).getCRS(), intersections);
    }
    private Polygon.SimplePolygon getSimplePolygon(@Name("polygon1") List<Point> polygon1) {
        org.neo4j.spatial.core.Point[] convertedPoints1 = asInMemoryPoints(polygon1);
        return Polygon.simple(convertedPoints1);
    }

    // TODO write tests
    @UserFunction("spatial.algo.intersection.sweepline")
    public List<Point> MCSweepLineIntersectArray(@Name("polygon1") List<Point> polygon1, @Name("polygon2") List<Point> polygon2) {
        validatePolygons(polygon1, polygon2);

        Polygon.SimplePolygon convertedPolygon1 = getSimplePolygon(polygon1);
        Polygon.SimplePolygon convertedPolygon2 = getSimplePolygon(polygon2);

        org.neo4j.spatial.core.Point[] intersections = new CartesianMCSweepLineIntersect().intersect(convertedPolygon1, convertedPolygon2);
        return asNeo4jPoints(polygon1.get(0).getCRS(), intersections);
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


    private org.neo4j.spatial.core.Point[] asInMemoryPoints(List<Point> polygon) {
        org.neo4j.spatial.core.Point[] points = new org.neo4j.spatial.core.Point[polygon.size()];
        for (int i = 0; i < points.length; i++) {
            points[i] = asInMemoryPoint(polygon.get(i));
        }
        return points;
    }

    private org.neo4j.spatial.core.Point asInMemoryPoint(Point point) {
        List<Double> coordinates = point.getCoordinate().getCoordinate();
        double[] coords = new double[coordinates.size()];
        for (int i = 0; i < coords.length; i++) {
            coords[i] = coordinates.get(i);
        }
        org.neo4j.spatial.core.CRS crs = CRSConverter.toInMemoryCRS(point.getCRS());
        return org.neo4j.spatial.core.Point.point(crs, coords);
    }

    private List<Point> asNeo4jPoints(CRS crs, org.neo4j.spatial.core.Point[] points) {
        List<Point> converted = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            converted.add(asNeo4jPoint(crs, points[i]));
        }
        return converted;
    }

    private Point asNeo4jPoint(CRS crs, org.neo4j.spatial.core.Point point) {
        return new Neo4jPoint(crs, new Coordinate(point.getCoordinate()));
    }

    private Point asNeo4jPoint(org.neo4j.spatial.core.Point point) {
        return new Neo4jPoint(toNeo4jCRS(point.getCRS()), new Coordinate(point.getCoordinate()));
    }

    private Point asNeo4jPoint(CRS crs, double[] coords) {
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

    public class PointResult {
        public Point point;

        private PointResult(Point point) {
            this.point = point;
        }
    }

    public class PointArraySizeResult {
        public long node_id;
        public long count;

        private PointArraySizeResult(long node_id, long count) {
            this.node_id = node_id;
            this.count = count;
        }
    }
}
