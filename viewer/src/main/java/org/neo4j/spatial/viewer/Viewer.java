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

        Driver driver = GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "Neo4j"));

        try (Session session = driver.session()) {
            int[] polygonIds = new int[]{
//                    54413,
//                    52834,
//                    941530,
//                    52832,
//                    54403,
//                    52826,
//                    54374,
//                    54417,
                    54412,
//                    52824,
//                    54409,
//                    54391,
//                    54386,
//                    54220,
//                    54223,
//                    52825,
//                    52827,
//                    54221,
//                    54367,
//                    54222,
//                    940675
            };

            int[] polylineIds = new int[]{
                    35969
            };

            Map<String, Object> parameters;
            for (int id : polygonIds) {
                parameters = new HashMap<>();
                parameters.put("id", id);
                addPolygonWKTFromDB("MATCH (r:OSMRelation) WHERE r.relation_osm_id=$id RETURN spatial.osm.graph.polygonAsWKT(r) AS WKT", parameters, viewer, session, color++);
                addPolygonFromDB("MATCH (r:OSMRelation) WHERE r.relation_osm_id=$id RETURN spatial.algo.graph.convexHull(r) AS locations", parameters, viewer, session, color++);
            }

            for (int id : polylineIds) {
                parameters = new HashMap<>();
                parameters.put("id", id);
                addPolylineWKTFromDB("MATCH (r:OSMRelation) WHERE r.relation_osm_id=$id RETURN spatial.osm.graph.polylineAsWKT(r) AS WKT", parameters, viewer, session, color++);
//                addPolylineWKTFromDB("MATCH (r:OSMRelation) WHERE r.relation_osm_id=$id RETURN spatial.osm.property.polylineAsWKT(r) AS WKT", parameters, viewer, session, color++);
            }
        }

        String[] polygons = new String[]{
//                "POLYGON((-121.82496764650902 38.12017017342189,-121.7677146483262 38.0974683239092,-121.72444575095368 38.16055129089121,-121.6371300951635 38.146893206673575,-121.62390913926394 38.06209479358153,-121.61115156350976 38.04860300425853,-121.53834054979424 38.12598705199173,-121.5355608201478 38.08920305678673,-121.49307540902878 38.087764511526814,-121.42094616086523 38.00775046140228,-121.35875641503777 37.975077025197926,-121.30793859274574 37.910328552459845,-121.28195292562076 37.86129489160115,-121.32150421469447 37.77733776892301,-121.26189811417423 37.72704885933369,-121.2900325171922 37.610388063314126,-121.295538780669 37.55740393874866,-121.27053306872395 37.57415790777541,-121.22907361084508 37.58014582717166,-121.2044784301068 37.51074005591544,-121.233709272809 37.437090295656795,-121.16576092449296 37.46562066065656,-121.1774125989073 37.45521840097954,-121.11932672987913 37.42782356306021,-121.09277969995651 37.42410374400365,-121.0597604437511 37.36311345928853,-120.93170218342561 37.38371671704961,-120.81074401174496 37.35391063847698,-120.76914153246106 37.279231133102506,-120.60186721366047 37.213875581928065,-120.53536855266853 37.14636103509345,-120.54414280555324 37.09453419244567,-120.5254435503044 36.99854101856188,-120.51734526765463 36.884799013528514,-120.6369533267741 36.858258374291786,-120.64662477577447 36.85008936347936,-120.60325734814818 36.78995520031742,-120.74713318540289 36.71845260277593,-120.86629752804876 36.70462667761684,-120.85478927641756 36.67039997017379,-120.94339476138248 36.695714363970495,-120.98515155713466 36.68006737151774,-121.0967546148721 36.65968721720738,-121.0841086412601 36.627393439301706,-121.18644999127983 36.612598072605586,-121.20902643541365 36.55749369216621,-121.2810644715623 36.539578734175386,-121.25806554906065 36.45933616427551,-121.26248598120884 36.411605692530784,-121.26961227715196 36.378265181440106,-121.28925485180685 36.31797033196365,-121.26682994681899 36.28465031462105,-121.29143130291864 36.21300937383575,-121.31164747601476 36.20558790302695,-121.36229248463385 36.138036687874724,-121.35899971451651 36.12732471038872,-121.41277378038072 36.05424891571208,-121.43105796356593 35.989842595561335,-121.47658883160851 35.99845973515406,-121.52438498036979 35.93297038624994,-121.61217116677521 35.92450802030824,-121.66589715325507 35.94280904956031,-121.71103664022338 35.871818135915625,-121.7774904959068 35.963087012670506,-121.84230068447172 35.902184199653185,-121.92305021980827 35.89396089710327,-121.97949562250922 35.94193640169695,-122.0472984752653 35.966477702789874,-122.07149114536864 35.91859640962773,-122.14566497438884 35.98817317893139,-122.18557080761835 36.02637901582751,-122.21359710279559 35.98639275393613,-122.24053003370004 36.019429212106346,-122.2997675187787 36.09767945795201,-122.3026275453715 36.135831282936216,-122.33522400934213 36.125601372358986,-122.31650067255643 36.176872926484506,-122.3264070149978 36.19398742909521,-122.33534971090911 36.29234736873415,-122.38695737925016 36.234448158194134,-122.34887968119916 36.35574322374262,-122.35390384728797 36.41034155461432,-122.40991031566388 36.36863469353311,-122.42192050840963 36.396007843727155,-122.4034861124657 36.4733348100932,-122.4019802050825 36.501313675074904,-122.40661531479343 36.577540883318136,-122.43933913580868 36.62528297112242,-122.51625351913943 36.60052897884913,-122.48976003207764 36.639884445649045,-122.51901478904163 36.64245327245836,-122.63777007474982 36.6330668025265,-122.66726864254075 36.647360973545986,-122.66063568681726 36.655029303777475,-122.69628335138272 36.64961796958072,-122.76578214130456 36.66280348471783,-122.83654916315982 36.696794385877965,-122.8681182172896 36.732435753513364,-122.94441595526479 36.741348884622056,-122.91456272763911 36.77092272383906,-123.06763150725565 36.83997837550904,-123.06418846374937 36.940832452976444,-123.12563245618792 36.96118941763823,-123.10076119782589 37.06979809450889,-123.07197676235194 37.085520661041954,-123.07768008077855 37.17665700095879,-123.01742065037888 37.21572297296322,-122.92518805531519 37.28361608904803,-122.85123911613252 37.32258497110937,-122.74125836380037 37.32370702789061,-122.71400680483603 37.37595880474556,-122.6830526096338 37.371082616225614,-122.62479935330515 37.4055355339383,-122.51014386371992 37.40520859337738,-122.50359582084425 37.40797526250235,-122.51383561117152 37.45768378701584,-122.40511216654549 37.43994831520047,-122.4470361905606 37.515110974268424,-122.39871606944324 37.49162413976108,-122.36150791778775 37.51925112729184,-122.38919009964152 37.571332041260675,-122.36519443806623 37.5880297722675,-122.38842915230951 37.645759136740196,-122.37284526323639 37.64862089113408,-122.38047796301453 37.686501185998985,-122.36065959800023 37.70658617009625,-122.36158227539865 37.823948882201854,-122.37171615283445 37.859369532029575,-122.32765615770937 37.93946696116086,-122.28286753140168 37.933276349029626,-122.23368914384174 37.9406098233677,-122.26142495512823 38.01613082840261,-122.23754072418197 38.02971183170201,-122.18838719274208 38.01506864477129,-122.16205229302321 38.0125543553976,-122.15498572660164 38.040289506619075,-122.09292128184228 38.11932440802116,-122.06172026853655 38.10452571171714,-122.01048023610005 38.09400672975512,-121.93301524553604 38.14353184038096,-121.92124025491124 38.0883456114964,-121.84232247855404 38.107895868611934,-121.82496764650902 38.12017017342189))",
//                "POLYGON((-121.51968072093143 38.25753415439765,-121.44552640612065 38.21833717129424,-121.42965640444629 38.2091731807655,-121.4071403283855 38.18105488857481,-121.37787666245495 38.202123891128515,-121.35733303316644 38.25428613094027,-121.27736598206856 38.174525374171616,-121.20242771145934 38.12408040090346,-121.1116204583413 38.11096779408424,-121.08791412870409 38.04630491279821,-121.0394255406965 38.113118077575486,-121.04764933772744 38.04103403789585,-121.00840040022315 38.084263981353224,-120.99716269351947 38.04077843284336,-120.95908754238891 38.010260481930196,-120.95482463478308 37.95460154957763,-120.92581859856134 37.96012164985841,-120.8505685202571 37.92265152261077,-120.78185888365138 37.87894025278913,-120.76128559615937 37.83423188890847,-120.74125722896198 37.771414865782546,-120.67094017452338 37.807404257237096,-120.6247029440907 37.73208578710224,-120.62006181580578 37.691885678943045,-120.56959675133375 37.70734310681171,-120.53821993882042 37.62164847045185,-120.45981216914754 37.62223257160506,-120.47958774357704 37.5722029647423,-120.42260819301114 37.51090665640701,-120.35197553632999 37.47573950785187,-120.28854968161207 37.41695732388667,-120.29588040069848 37.386432085335166,-120.2638511044189 37.28105259128045,-120.24460305260364 37.26612473439093,-120.2657262578528 37.197610896633925,-120.22052179633096 37.100165538428655,-120.20622070459859 37.03781517806326,-120.19770014582225 36.96731664708518,-120.29836686005879 36.93742201079002,-120.27128702798282 36.91164417717897,-120.27445031752548 36.837860164150484,-120.31113623049569 36.73926717201952,-120.3785571854214 36.69071863476441,-120.4351869568698 36.70710334342078,-120.37039623067626 36.669375376413626,-120.49940349514458 36.64023918860646,-120.5896978290729 36.58381214405512,-120.55726687811317 36.53927210343186,-120.64435565122527 36.506088043402265,-120.63957297061192 36.48340695425122,-120.69914638427598 36.42976894173998,-120.75086837430018 36.42437559976115,-120.80653654216758 36.40518691209524,-120.86608258249592 36.349427905451456,-120.93520336453845 36.31912361276963,-120.96244203386429 36.23610340340084,-120.99844151595404 36.200278155193544,-121.01833068848573 36.18302693304722,-121.01442203524479 36.154670916948334,-121.10137272989238 36.15899741665869,-121.12070484235538 36.09346693776001,-121.21093918377395 36.071990435903814,-121.2779071309687 36.05007263440112,-121.32197065101545 36.05857016387469,-121.38640223920297 36.000963752950526,-121.39747108588482 35.99318664166634,-121.44215919019976 35.9700613343175,-121.47743669364883 36.0467334447872,-121.55738077832477 36.01538740372271,-121.61009357137742 36.10054226438453,-121.67340844073141 36.04595364695525,-121.68972105062284 36.11504411241983,-121.73600315117021 36.13801262256257,-121.76554556631714 36.09287685936769,-121.77323398534392 36.20706445638422,-121.78125391457098 36.200179697983906,-121.80435417566342 36.239320385507035,-121.85458966894777 36.2153367209881,-121.87435057801706 36.20599375618432,-121.88778891422801 36.289612053748364,-121.90355848893176 36.35867861940564,-121.9121630338937 36.41220570175823,-121.94724307359587 36.45644001068134,-121.97340074594288 36.50306539286059,-122.0216741235242 36.444777479037576,-122.03408290348807 36.48691111167975,-122.01547985876756 36.55760360523458,-122.02269416321523 36.615424476983,-122.06686772275798 36.60133565407611,-122.09614976993822 36.62757750386178,-122.0710881791404 36.65591553832787,-122.15774061217738 36.637104777074704,-122.16061115464778 36.658612613107174,-122.16333386158826 36.69874889761992,-122.16799971592191 36.701733537236755,-122.1827686127562 36.768566967853175,-122.25577691462252 36.77109920451171,-122.2673651177813 36.840665008040695,-122.3136839141502 36.8859230039375,-122.31961105742192 36.916791581488624,-122.3792757548 36.9251238582008,-122.38868974567184 36.95136466115742,-122.40002304745776 36.98648381711797,-122.39951604399504 37.01288168577541,-122.42127878200115 37.0405558945761,-122.36453810050824 37.051922514519575,-122.39386899412976 37.11935451686719,-122.4346300779605 37.18334698377903,-122.35614989594215 37.24204724406364,-122.38148103252034 37.31909236614354,-122.33740649068729 37.376837254485075,-122.3356969824545 37.424730862452805,-122.24441689256503 37.45069071893756,-122.26113697957932 37.47982339967296,-122.18870728435085 37.45655931072133,-122.210865823189 37.48355324030658,-122.1403430008574 37.51037220023885,-122.15864500027861 37.5991304085263,-122.10130432733371 37.58245397081239,-122.0590330274593 37.587721977093686,-122.07268742076315 37.66456915687257,-122.06518596190806 37.7074189095374,-121.98468928789418 37.70110070258504,-121.96593650941118 37.768719789857876,-121.94768432983227 37.81253365648449,-121.9334052375884 37.84986250205743,-121.93417371690799 37.86618334298804,-121.91295955435427 37.89551116243743,-121.92258215550522 37.94634751365086,-121.88980192131304 37.912483034950604,-121.86325813295856 37.95314446521377,-121.8126690909782 38.0444751183024,-121.7860880119477 38.12195819951969,-121.76008184198011 38.06093376302419,-121.71311285605724 38.08792752669445,-121.68728320837202 38.14987251827074,-121.64091541864502 38.17469543791752,-121.57359337830952 38.21322640284977,-121.51968072093143 38.25753415439765))"
        };

        String[] polyline = new String[]{
        };

        String[] points = new String[]{
                "POINT(14.370287083278079 56.94074629063927)",
                "POINT(15.405119130622467 56.6617096318315)"
        };

        for (String s : polygons) {
            viewer.addPolygon(s, color++);
        }

        for (String s : polyline) {
            viewer.addLineString(s, color++);
        }

        for (String s : points) {
            viewer.addPoint(s);
        }

//        viewer.addPoint("POINT(-22.0 -18.0)");

        viewer.view();
    }

    private static void addPolygonWKTFromDB(String query, Map<String, Object> parameters, Viewer viewer, Session session, int colorId) {
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

    private static void addPolylineWKTFromDB(String query, Map<String, Object> parameters, Viewer viewer, Session session, int colorId) {
        StatementResult result = session.run(query, parameters);
        if (!result.hasNext()) {
            System.out.println("No result found for parameters: " + parameters);
            return;
        }

        while (result.hasNext()) {
            Record next = result.next();
            String WKT = (String) next.asMap().get("WKT");
            System.out.println(WKT);
            viewer.addLineString(WKT, colorId);
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
