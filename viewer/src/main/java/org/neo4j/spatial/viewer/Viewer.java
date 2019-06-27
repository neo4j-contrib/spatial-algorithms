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

        int color = 0;

        Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "abc"));

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

            Map<String, Object> parameters;
            for (int id : ids) {
                parameters = new HashMap<>();
                parameters.put("id", id);
                addWKTFromDB("MATCH (r:OSMRelation) WHERE r.relation_osm_id=$id RETURN spatial.osm.graph.polygonAsWKT(r) AS WKT", parameters, viewer, session, color++);
//                addWKTFromDB("MATCH (r:OSMRelation) WHERE r.relation_osm_id=$id RETURN spatial.osm.property.polygonAsWKT(r) AS WKT", parameters, viewer, session, color++);
            }
        }

        String[] polygons = new String[]{
                //"POLYGON((0.23306181510094326 -0.1658925289826501,-0.16633821591583559 -0.15286471138490676,-0.18722097218865746 -0.1454790830825448,-0.268991127297881 -0.08710049990082208,-0.3295179725890604 0.08297835964413663,-0.29485479735199543 0.14143608770049493,-0.1666122850949554 0.17879740780903264,0.11678630901000576 0.18644764846696849,0.29368885640605624 0.1535555312924335,0.30839532726428276 0.14886692235083526,0.31224235934754047 0.11890173271874749,0.2930714012115545 0.026009786538145285,0.23306181510094326 -0.1658925289826501))"
        };

        String[] points = new String[]{
                /*"POINT(0.2930714012115545 0.026009786538145285)",
        "POINT(0.19642097155328753 0.14015186842620214)",
        "POINT(0.17853008634847028 -0.061668790305009435)",
        "POINT(-0.10926169278233791 -0.07580738878083287)",
        "POINT(0.02791488598573373 -0.06922137263404954)",
        "POINT(0.19653850136602757 -0.10391773348047145)",
        "POINT(0.24393239604799466 -0.025961500553939088)",
        "POINT(0.11316488162012477 -0.05449884935797489)",
        "POINT(-0.18320210831936246 0.159180569755)",
        "POINT(0.28555719640949984 0.0433125649102033)",
        "POINT(-0.21873619595863153 0.008382699926845427)",
        "POINT(0.1851015693660491 0.16022617498844405)",
        "POINT(-0.16633821591583559 -0.15286471138490676)",
        "POINT(-0.2595309801440219 0.01370158144876471)",
        "POINT(-0.14727676632621448 0.16588848892031371)",
        "POINT(0.0663462271671937 0.11000403800417857)",
        "POINT(0.027829339865052045 -0.08171464706326576)",
        "POINT(0.10077535458806701 -0.12540694227966132)",
        "POINT(0.26087518743989885 0.08299600597720447)",
        "POINT(0.29368885640605624 0.1535555312924335)",
        "POINT(-0.007163496969285421 0.04067452398313676)",
        "POINT(-0.1985776453871253 0.027135109823636427)",
        "POINT(0.04030551378068137 0.17581754259962068)",
        "POINT(0.1742909070996327 0.0765724292932552)",
        "POINT(-0.18688923052036197 0.10871626302328091)",
        "POINT(-0.10516260775173852 6.702212277587827E-4)",
        "POINT(-0.29485479735199543 0.14143608770049493)",
        "POINT(0.18858765127021343 -0.04226379393811036)",
        "POINT(0.13961354941761123 0.0761144553916418)",
        "POINT(-0.0020888813394823666 -0.04716918596083214)",
        "POINT(0.14687635701409069 -0.10825609206103404)",
        "POINT(0.1859413078606743 -0.047522964917581345)",
        "POINT(-0.006899038891576607 0.06493879355360849)",
        "POINT(-0.03050037503201654 0.06488400671721417)",
        "POINT(0.187927827783899 -0.09334774431400383)",
        "POINT(-0.040376556263823596 0.016674526225386455)",
        "POINT(-0.11853857951900947 0.02181689708876594)",
        "POINT(0.2707086887844813 0.024983666837220697)",
        "POINT(-0.08567166592700573 0.13635052100077427)",
        "POINT(-0.18264965989513945 -0.08030143146618124)",
        "POINT(-0.16655858548146074 0.12639694716913968)",
        "POINT(0.31224235934754047 0.11890173271874749)",
        "POINT(-0.18986435272496335 -0.030568072513925293)",
        "POINT(-0.30498299944397966 0.03181997386248958)",
        "POINT(-0.05298406025714351 -0.047972039981782066)",
        "POINT(-0.1884532109665219 0.018745269144525678)",
        "POINT(0.24820539909044964 0.07639903135805975)",
        "POINT(0.12518482603394362 -0.1344672634240685)",
        "POINT(-0.22932941934676485 0.05139814475564369)",
        "POINT(-0.1801573522081721 -0.12103513259539406)",
        "POINT(-0.11908097106968157 0.15829800689438606)",
        "POINT(-0.24179478537828603 -0.059684151901318205)",
        "POINT(-0.266533943586763 -0.050985067279926966)",
        "POINT(0.11814923639941424 0.10679179047580278)",
        "POINT(-0.042766580300886856 -0.06832796841123634)",
        "POINT(-0.05458717167695416 0.03981607900395573)",
        "POINT(0.19639494191739285 -0.09741076510886887)",
        "POINT(0.20683823634660076 -0.10993580204750103)",
        "POINT(0.30839532726428276 0.14886692235083526)",
        "POINT(-0.29950180774690033 0.10752094289229902)",
        "POINT(-0.040446771720826606 0.06373523091378219)",
        "POINT(-0.18293828545652974 0.031146396829652256)",
        "POINT(0.24281361383058597 0.05233929889899547)",
        "POINT(0.11030399397919599 -0.08615852219554998)",
        "POINT(0.18639338376431375 0.1489553686436444)",
        "POINT(-0.21121079553176303 -0.05066703440768405)",
        "POINT(0.12444213912731844 -0.08371831349482206)",
        "POINT(-0.10377396838171109 0.11544223049245017)",
        "POINT(-0.1730670149131309 -0.09857288638738543)",
        "POINT(0.11037747548588533 -0.12354126866990156)",
        "POINT(-0.04628267107140467 -0.041608591992687294)",
        "POINT(0.15875814929478693 -0.014027077372561758)",
        "POINT(-0.268991127297881 -0.08710049990082208)",
        "POINT(0.043177095870199404 -0.05561709995780478)",
        "POINT(0.02314121458211353 0.1396883171823346)",
        "POINT(-0.18722097218865746 -0.1454790830825448)",
        "POINT(-0.003666399088193932 0.06243075807253812)",
        "POINT(0.047123597704515505 0.0036481520651636123)",
        "POINT(-0.19910396619778203 -0.11799928923771752)",
        "POINT(-0.047318959362423856 -0.031563831445417356)",
        "POINT(0.2357642770821466 0.10126104554638826)",
        "POINT(-0.1666122850949554 0.17879740780903264)",
        "POINT(0.30667830355174114 0.1142627835342897)",
        "POINT(0.1551922334845179 -0.14260767965131987)",
        "POINT(-0.10447140064704424 -0.05324390315721894)",
        "POINT(-0.24236661814626476 0.1085498935416142)",
        "POINT(-0.31424822282775683 0.06794490355195743)",
        "POINT(-0.13171310542837872 0.15763320118201507)",
        "POINT(0.11678630901000576 0.18644764846696849)",
        "POINT(-0.24750625901124423 -0.09135783861438107)",
        "POINT(-0.1601039661207915 -0.04782678227597231)",
        "POINT(0.06800883788828133 0.010749816316555638)",
        "POINT(0.14654158204212214 0.12188994702881029)",
        "POINT(0.241397313065492 0.0600237425211218)",
        "POINT(-0.3295179725890604 0.08297835964413663)",
        "POINT(-0.16574223496481805 0.07797453220696243)",
        "POINT(0.23306181510094326 -0.1658925289826501)",
        "POINT(0.051466707835884556 -0.04661493637411568)",
        "POINT(-0.1806172067859032 0.08723666459797902)",
        "POINT(0.004023341841465078 -0.0604218315798955)",*/
        };

        for (String s : polygons) {
            viewer.addPolygon(s, color++);
        }

        for (String s : points) {
            viewer.addPoint(s);
        }

//        viewer.addPoint("POINT(-22.0 -18.0)");

        viewer.view();
    }

    private static void addWKTFromDB(String query, Map<String, Object> parameters, Viewer viewer, Session session, int colorId) {
        StatementResult result = session.run(query, parameters);
        if (!result.hasNext()) {
            System.out.println("No result found for parameters: " + parameters);
            return;
        }

        while (result.hasNext()) {
            Record next = result.next();
            String WKT = (String) next.asMap().get("WKT");
            System.out.println(WKT);
            viewer.addPolygon(WKT, colorId);
        }
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
        this.map.addLayer(new FeatureLayer(featureCollection, SLD.createPointStyle("Circle", Color.BLACK, Color.BLACK, 1, 15)));
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
