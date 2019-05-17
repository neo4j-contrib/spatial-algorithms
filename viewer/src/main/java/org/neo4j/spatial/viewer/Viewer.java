package org.neo4j.spatial.viewer;

import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.jts.LiteCoordinateSequence;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Grids;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.SLD;
import org.geotools.swing.JMapFrame;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.neo4j.driver.v1.*;
import org.neo4j.driver.v1.types.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class Viewer {
    public static void main(String[] args) throws Exception {
        Viewer viewer = new Viewer();

        Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "Neo4j"));

        int color = 0;

        try (Session session = driver.session()) {
            int[] ids = new int[]{
                    54413,
                    52834,
                    941530,
                    52832,
                    54403,
                    52826,
                    54374,
                    54417,
                    54412,
                    52824,
                    54409,
                    54391,
                    54386,
                    54220,
                    54223,
                    52825,
                    52827,
                    54221,
                    54367,
                    54222,
                    940675
            };
            ids = new int[]{54367,54374};

            Map<String, Object> parameters;
            for (int id : ids) {
                parameters = new HashMap<>();
                parameters.put("id", id);
                addPolygonFromDB("MATCH (r:OSMRelation)-[:POLYGON_STRUCTURE]->(s:SHELL) WHERE r.relation_osm_id=$id RETURN s.polygon AS locations", parameters, viewer, session, color++);
            }
            parameters = new HashMap<>();
            addPolygonFromDB("MATCH (h:HOLE) RETURN h.polygon AS locations", parameters, viewer, session, -1);
        }

        viewer.view();
    }

    private static void addPolygonFromDB(String query, Map<String, Object> parameters, Viewer viewer, Session session, int colorId) {
        StatementResult result = session.run(query, parameters);
        if (!result.hasNext()) {
            System.out.println("No result found for parameters: " + parameters);
            return;
        }

        while (result.hasNext()) {
            Record next = result.next();
            List<Point> locations = (List<Point>) next.asMap().get("locations");

            if (locations.size() < 3) {
                return;
            }

            StringJoiner joiner;
            joiner = new StringJoiner(",", "POLYGON((", "))");

            for (Point point : locations) {
                joiner.add(point.x() + " " + point.y());
            }
            joiner.add(locations.get(0).x() + " " + locations.get(0).y());


            viewer.addPolygon(joiner.toString(), colorId);
        }
    }

    private MapContent map;

    public Viewer() {
        this.map = new MapContent();
        this.map.setTitle("Viewer");

//        createGrid();
    }

    public void addLineString(String WKTString, int colorId) {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );

        WKTReader reader = new WKTReader( geometryFactory );
        Geometry point;
        try {
            point = reader.read(WKTString);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        SimpleFeatureType TYPE;
        try {
            TYPE = DataUtilities.createType("test", "Element "+colorId, "the_geom:LineString");
        } catch (SchemaException e) {
            e.printStackTrace();
            return;
        }
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        featureBuilder.add(point);
        SimpleFeature feature = featureBuilder.buildFeature("Element "+colorId);


        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        featureCollection.add(feature);
        this.map.addLayer(new FeatureLayer(featureCollection, SLD.createLineStyle(getColorByIndex(colorId), 3)));
    }

    public void addPolygon(String WKTString, int colorId) {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );

        WKTReader reader = new WKTReader( geometryFactory );
        Geometry point;
        try {
            point = reader.read(WKTString);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        SimpleFeatureType TYPE;
        try {
            TYPE = DataUtilities.createType("test", "Element "+colorId, "the_geom:MultiPolygon");
        } catch (SchemaException e) {
            e.printStackTrace();
            return;
        }
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        featureBuilder.add(point);
        SimpleFeature feature = featureBuilder.buildFeature("Element "+colorId);


        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        featureCollection.add(feature);
        this.map.addLayer(new FeatureLayer(featureCollection, SLD.createPolygonStyle(getColorByIndex(colorId), getColorByIndex(colorId), 1f)));
    }

    public void addPoint(String WKTString) {
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );

        WKTReader reader = new WKTReader( geometryFactory );
        Geometry point;
        try {
            point = reader.read(WKTString);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        SimpleFeatureType TYPE;
        try {
            TYPE = DataUtilities.createType("test", "Element", "the_geom:Point");
        } catch (SchemaException e) {
            e.printStackTrace();
            return;
        }
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        featureBuilder.add(point);
        SimpleFeature feature = featureBuilder.buildFeature("Element");


        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        featureCollection.add(feature);
        this.map.addLayer(new FeatureLayer(featureCollection, SLD.createPointStyle("Circle", Color.BLACK, Color.BLACK, 1, 25)));
    }

    private void createGrid() {
        double min = -20;
        double max = 20;

        ReferencedEnvelope gridBounds =
                new ReferencedEnvelope(min, max, min, max, DefaultGeographicCRS.WGS84);

        SimpleFeatureSource grid = Grids.createSquareGrid(gridBounds, 1.0);

        try {
            this.map.addLayer(new FeatureLayer(grid.getFeatures(), SLD.createLineStyle(Color.gray, 1)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        GeometryFactory factory = JTSFactoryFinder.getGeometryFactory( null );
        LineString xaxis = factory.createLineString(new LiteCoordinateSequence(min, 0, max, 0));
        LineString yaxis = factory.createLineString(new LiteCoordinateSequence(0, min, 0, max));

        SimpleFeatureType TYPE, TYPE2;
        try {
            TYPE = DataUtilities.createType("test", "x-axis", "the_geom:LineString");
            TYPE2 = DataUtilities.createType("test", "y-axis", "the_geom:LineString");
        } catch (SchemaException e) {
            e.printStackTrace();
            return;
        }
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(TYPE);
        featureBuilder.add(xaxis);
        SimpleFeatureBuilder featureBuilder2 = new SimpleFeatureBuilder(TYPE2);
        featureBuilder2.add(yaxis);
        SimpleFeature feature = featureBuilder.buildFeature("x-axis");
        SimpleFeature feature2 = featureBuilder2.buildFeature("y-axis");


        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        featureCollection.add(feature);
        featureCollection.add(feature2);
        this.map.addLayer(new FeatureLayer(featureCollection, SLD.createLineStyle(Color.darkGray, 3)));
    }

    public void view() {
        JMapFrame mapFrame = new JMapFrame(this.map);
        mapFrame.enableToolBar(true);
        mapFrame.enableStatusBar(true);
        mapFrame.enableLayerTable(true);

        // Display the map frame. When it is closed the application will exit
        mapFrame.setSize(1800, 1000);
        mapFrame.setVisible(true);
    }

    private Color getColorByIndex(int index) {
        if (index == -1) {
            return Color.white;
        }
        Color[] colors = new Color[]{
                Color.blue,
                Color.red,
                Color.cyan,
                Color.black,
                Color.green,
                Color.orange,
                Color.magenta,
                Color.pink
        };

        return colors[index % colors.length];
    }

    public String getCurrentColorName(int id) {
        if (id == -1) {
            return "white";
        }
        String[] colors = new String[]{
                "blue",
                "red",
                "cyan",
                "black",
                "green",
                "orange",
                "magenta",
                "pink"
        };

        return colors[id % colors.length];
    }
}
