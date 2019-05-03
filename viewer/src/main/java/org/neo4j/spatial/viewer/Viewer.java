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
import org.neo4j.spatial.algo.Intersect.MCSweepLineIntersect;
import org.neo4j.spatial.core.MonotoneChain;
import org.neo4j.spatial.algo.Intersect.MonotoneChainPartitioner;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.io.IOException;
import java.util.List;

public class Viewer {
    public static void main(String[] args) throws Exception {
        Viewer viewer = new Viewer();

        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(-3.073942953027313, -0.3631908536811643),
                Point.point(3.957307046972687, 0.33992971981693054),
                Point.point(4.001252359472687, -3.5250298989084747),
                Point.point(-0.8327320155273128, -2.7132528509732756),
                Point.point(1.2107250157226872, -1.044287806764632),
                Point.point(-3.930876546777313, -1.110194121461247),
                Point.point(-5.051482015527313, 1.262681056229549),
                Point.point(-2.898161703027313, 0.8452721446430446),
                Point.point(-3.073942953027313, -0.3631908536811643)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(0.4197093907226872,3.0411394778853147),
                Point.point(-3.403532796777313,-0.6488239497102567),
                Point.point(2.045685953222687,-4.511356587129941),
                Point.point(7.165314859472687,-2.8888240176242217),
                Point.point(6.901642984472687,0.7134484325376148),
                Point.point(4.220978921972687,-1.7471997776342576),
                Point.point(0.5295726719726872,-2.4059429401676002),
                Point.point(3.891389078222687,0.09823244716972954),
                Point.point(6.132600015722687,1.6580596189713295),
                Point.point(3.627717203222687,1.877681460711756),
                Point.point(1.9358226719726872,-0.011630785875617151),
                Point.point(-0.5251148280273128,-1.3738038176337573),
                Point.point(-1.1842945155273128,0.12020506336406792),
                Point.point(1.2986156407226872,0.7134484325376148),
                Point.point(1.4304515782226872,2.075317616513406),
                Point.point(2.660920328222687,3.43601485945374),
                Point.point(4.330842203222687,3.3263428389465592),
                Point.point(3.122346109472687,4.115684615656987),
                Point.point(0.4197093907226872,3.0411394778853147)
        );

//        viewer.addPolyLine(a.toWKT());
//        viewer.addPolyLine(b.toWKT());

        List<MonotoneChain> chains = MonotoneChainPartitioner.partition(a);
        chains.addAll(MonotoneChainPartitioner.partition(b));

        for (MonotoneChain chain : chains) {
            System.out.printf("MC%s: %s %s\n", chain.getId(), viewer.getCurrentColorName((int) chain.getId()), chain.getVertices());
            viewer.addPolyLine(chain.toWKT(), (int) chain.getId());
        }

        Point[] intersections = new MCSweepLineIntersect().intersect(a, b);

        for (Point intersection : intersections) {
            viewer.addPoint(intersection.toWKT());
        }

        viewer.view();
    }

    private MapContent map;


    public Viewer() {
        this.map = new MapContent();
        this.map.setTitle("Viewer");
    }

    public void addPolyLine(String WKTString, int colorId) {
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
        createGrid();

        JMapFrame mapFrame = new JMapFrame(this.map);
        mapFrame.enableToolBar(true);
        mapFrame.enableStatusBar(true);
        mapFrame.enableLayerTable(true);

        // Display the map frame. When it is closed the application will exit
        mapFrame.setSize(1800, 1000);
        mapFrame.setVisible(true);
    }

    private Color getColorByIndex(int index) {
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
