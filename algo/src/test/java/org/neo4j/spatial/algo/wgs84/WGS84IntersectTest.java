package org.neo4j.spatial.algo.wgs84;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.Intersect;
import org.neo4j.spatial.algo.IntersectCalculator;
import org.neo4j.spatial.core.*;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(value = Parameterized.class)
public class WGS84IntersectTest {
    private Intersect calculator;

    @Parameterized.Parameters
    public static Collection data() {
        IntersectCalculator.AlgorithmVariant[] variants = new IntersectCalculator.AlgorithmVariant[]{IntersectCalculator.AlgorithmVariant.Naive, IntersectCalculator.AlgorithmVariant.MCSweepLine};
        return Arrays.asList(variants);
    }

    public WGS84IntersectTest(IntersectCalculator.AlgorithmVariant variant) {
        this.calculator = IntersectCalculator.getCalculator(CRS.WGS84, variant);
    }

    @Test
    public void intersect() {
        Point p = Point.point(CRS.WGS84, -50, 0);
        Point q = Point.point(CRS.WGS84, -30, 0);
        Point r = Point.point(CRS.WGS84, -40, -5);
        Point s = Point.point(CRS.WGS84, -40, 3);

        LineSegment a = LineSegment.lineSegment(p, q);
        LineSegment b = LineSegment.lineSegment(r, s);

        Point actual = calculator.intersect(a, b);
        Point expected = Point.point(CRS.WGS84, -40, 0);
        matchPoints(new Point[]{actual}, new Point[]{expected});
    }

    @Test
    public void shouldFindIntersectionsBetweenSharedVertex() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 0, 5)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(CRS.WGS84, -10, 10),
                Point.point(CRS.WGS84, 0, 5),
                Point.point(CRS.WGS84, 10, 10)
        );

        matchPoints(calculator.intersect(a, b), new Point[]{Point.point(CRS.WGS84, 0, 5)});
    }

    @Test
    public void shouldFindIntersectionBetweenPolylineAndLineSegment() {
        Polyline a = Polyline.polyline(
                Point.point(CRS.WGS84, -60, 0),
                Point.point(CRS.WGS84, -50, 0),
                Point.point(CRS.WGS84, -30, 0),
                Point.point(CRS.WGS84, -20, 0),
                Point.point(CRS.WGS84, -20, 1),
                Point.point(CRS.WGS84, -50, 1)

        );
        LineSegment b = LineSegment.lineSegment(
                Point.point(CRS.WGS84, -40, -5),
                Point.point(CRS.WGS84, -40, 3));

        Point[] actual = calculator.intersect(a, b);
        Point[] expected = new Point[]{Point.point(CRS.Cartesian, -40, 0), Point.point(CRS.Cartesian, -40, 1.0313299764)};
        matchPoints(actual, expected);
    }

    @Test
    public void shouldFindIntersectionsBetweenPolylines() {
        Polyline a = Polyline.polyline(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 20, 10),
                Point.point(CRS.WGS84, -0, 10)
        );
        Polyline b = Polyline.polyline(
                Point.point(CRS.WGS84, -15, 0),
                Point.point(CRS.WGS84, 25, 0),
                Point.point(CRS.WGS84, 26, 15),
                Point.point(CRS.WGS84, -14, 15)
        );

        Point[] actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, 15, 0)});

        a = Polyline.polyline(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );
        b = Polyline.polyline(
                Point.point(CRS.WGS84, -5, -5),
                Point.point(CRS.WGS84, 15, -5),
                Point.point(CRS.WGS84, 15, 15),
                Point.point(CRS.WGS84, -5, 15)
        );

        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, 10.0, -5.0575148968282075)});
    }

    @Test
    public void shouldFindIntersectionsBetweenPolygonAndPolyline() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 20, 10),
                Point.point(CRS.WGS84, -0, 10)
        );
        Polyline b = Polyline.polyline(
                Point.point(CRS.WGS84, -15, 0),
                Point.point(CRS.WGS84, 25, 0),
                Point.point(CRS.WGS84, 26, 15),
                Point.point(CRS.WGS84, -14, 15)
        );

        Point[] actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, 15, 0), Point.point(CRS.WGS84, -5, 0)});

        a = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );
        b = Polyline.polyline(
                Point.point(CRS.WGS84, -5, -5),
                Point.point(CRS.WGS84, 15, -5),
                Point.point(CRS.WGS84, 15, 15),
                Point.point(CRS.WGS84, -5, 15)
        );

        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, 10.0, -5.0575148968282075)});
    }

    @Test
    public void shouldNotFindIntersectionsBetweenSimplePolygons() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(CRS.WGS84, -100, -150),
                Point.point(CRS.WGS84, 150, 150),
                Point.point(CRS.WGS84, 150, -150)
        );

        assertThat(calculator.intersect(a, b), org.hamcrest.Matchers.emptyArray());
    }

    @Test
    public void shouldFindIntersectionsBetweenSimplePolygons() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 20, 10),
                Point.point(CRS.WGS84, -0, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(CRS.WGS84, -15, 0),
                Point.point(CRS.WGS84, 25, 0),
                Point.point(CRS.WGS84, 26, 15),
                Point.point(CRS.WGS84, -14, 15)
        );

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        Point[] actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, 15, 0), Point.point(CRS.WGS84, -5, 0)});

        a = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );
        b = Polygon.simple(
                Point.point(CRS.WGS84, -5, -5),
                Point.point(CRS.WGS84, 15, -5),
                Point.point(CRS.WGS84, 15, 15),
                Point.point(CRS.WGS84, -5, 15)
        );

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, 10.0, -5.0575148968282075), Point.point(CRS.WGS84, -4.999999999999999, 10.113252586707066)});

        a = Polygon.simple(
                Point.point(CRS.WGS84, -10, -10),
                Point.point(CRS.WGS84, 10, -10),
                Point.point(CRS.WGS84, 0, 5)
        );
        b = Polygon.simple(
                Point.point(CRS.WGS84, 0, -5),
                Point.point(CRS.WGS84, 10, 10),
                Point.point(CRS.WGS84, -10, 10)
        );

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, 3.3124813211817257, 0), Point.point(CRS.WGS84, -3.3124813211817257, 0)});

        a = Polygon.simple(
                Point.point(CRS.WGS84, -3.073942953027313, -0.3631908536811643),
                Point.point(CRS.WGS84, -2.898161703027313, 0.8452721446430446),
                Point.point(CRS.WGS84, -5.051482015527313, 1.262681056229549),
                Point.point(CRS.WGS84, -3.930876546777313, -1.110194121461247),
                Point.point(CRS.WGS84, 1.2107250157226872, -1.044287806764632),
                Point.point(CRS.WGS84, -0.8327320155273128, -2.7132528509732756),
                Point.point(CRS.WGS84, 4.001252359472687, -3.5250298989084747),
                Point.point(CRS.WGS84, 3.957307046972687, 0.33992971981693054)
        );
        b = Polygon.simple(
                Point.point(CRS.WGS84, 0.4197093907226872,3.0411394778853147),
                Point.point(CRS.WGS84, -3.403532796777313,-0.6488239497102567),
                Point.point(CRS.WGS84, 2.045685953222687,-4.511356587129941),
                Point.point(CRS.WGS84, 7.165314859472687,-2.8888240176242217),
                Point.point(CRS.WGS84, 6.901642984472687,0.7134484325376148),
                Point.point(CRS.WGS84, 4.220978921972687,-1.7471997776342576),
                Point.point(CRS.WGS84, 0.5295726719726872,-2.4059429401676002),
                Point.point(CRS.WGS84, 3.891389078222687,0.09823244716972954),
                Point.point(CRS.WGS84, 6.132600015722687,1.6580596189713295),
                Point.point(CRS.WGS84, 3.627717203222687,1.877681460711756),
                Point.point(CRS.WGS84, 1.9358226719726872,-0.011630785875617151),
                Point.point(CRS.WGS84, -0.5251148280273128,-1.3738038176337573),
                Point.point(CRS.WGS84, -1.1842945155273128,0.12020506336406792),
                Point.point(CRS.WGS84, 1.2986156407226872,0.7134484325376148),
                Point.point(CRS.WGS84, 1.4304515782226872,2.075317616513406),
                Point.point(CRS.WGS84, 2.660920328222687,3.43601485945374),
                Point.point(CRS.WGS84, 4.330842203222687,3.3263428389465592),
                Point.point(CRS.WGS84, 3.122346109472687,4.115684615656987)
        );

        Point[] expected = new Point[]{
                Point.point(CRS.WGS84, -3.068389645976, -0.325012193039),
                Point.point(CRS.WGS84, -2.775189790165, -1.096140943706),
                Point.point(CRS.WGS84, -1.059857818944, -0.161880498908),
                Point.point(CRS.WGS84, -0.677674056166, -2.586831429144),
                Point.point(CRS.WGS84, -0.659524248473, -1.069263100502),
                Point.point(CRS.WGS84, -0.394238446406, -2.787821990105),
                Point.point(CRS.WGS84, 0.042154730523, -1.060025023954),
                Point.point(CRS.WGS84, 2.082767597071, 0.152541643688),
                Point.point(CRS.WGS84, 3.959513274862, 0.145673350408),
                Point.point(CRS.WGS84, 3.981505774021, -1.790199797499)
        };

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        actual = calculator.intersect(a, b);
        matchPoints(actual, expected);
    }

    @Test
    public void shouldFindIntersectionsBetweenMultiPolygons() {
        MultiPolygon a = new MultiPolygon();
        Point[][] polygonsA = new Point[][]{
                {
                        Point.point(CRS.WGS84, -10, -10),
                        Point.point(CRS.WGS84, 10, -10),
                        Point.point(CRS.WGS84, 10, 10),
                        Point.point(CRS.WGS84, -10, 10),
                        Point.point(CRS.WGS84, -10, -10)
                },
                {
                        Point.point(CRS.WGS84, -9, -9),
                        Point.point(CRS.WGS84, 9, -9),
                        Point.point(CRS.WGS84, 9, 9),
                        Point.point(CRS.WGS84, -9, 9),
                        Point.point(CRS.WGS84, -9, -9)
                },
                {
                        Point.point(CRS.WGS84, -8, -8),
                        Point.point(CRS.WGS84, 8, -8),
                        Point.point(CRS.WGS84, 8, 8),
                        Point.point(CRS.WGS84, -8, 8)
                },
                {
                        Point.point(CRS.WGS84, -7, -7),
                        Point.point(CRS.WGS84, 7, -7),
                        Point.point(CRS.WGS84, 7, 7),
                        Point.point(CRS.WGS84, -7, 7)
                }
        };

        for (Point[] points : polygonsA) {
            a.insertPolygon(Polygon.simple(points));
        }

        MultiPolygon b = new MultiPolygon();
        Point[][] polygonsB = new Point[][]{
                {
                        Point.point(CRS.WGS84, 10 + -10, -10 + 10),
                        Point.point(CRS.WGS84, 10 + 10, -10 + 10),
                        Point.point(CRS.WGS84, 10 + 10, 10 + 10),
                        Point.point(CRS.WGS84, 10 + -10, 10 + 10),
                        Point.point(CRS.WGS84, 10 + -10, -10 + 10)
                },
                {
                        Point.point(CRS.WGS84, 10 + -9, -9 + 10),
                        Point.point(CRS.WGS84, 10 + 9, -9 + 10),
                        Point.point(CRS.WGS84, 10 + 9, 9 + 10),
                        Point.point(CRS.WGS84, 10 + -9, 9 + 10),
                        Point.point(CRS.WGS84, 10 + -9, -9 + 10)
                },
                {
                        Point.point(CRS.WGS84, 10 + -8, -8 + 10),
                        Point.point(CRS.WGS84, 10 + 8, -8 + 10),
                        Point.point(CRS.WGS84, 10 + 8, 8 + 10),
                        Point.point(CRS.WGS84, 10 + -8, 8 + 10)
                },
                {
                        Point.point(CRS.WGS84, 10 + -7, -7 + 10),
                        Point.point(CRS.WGS84, 10 + 7, -7 + 10),
                        Point.point(CRS.WGS84, 10 + 7, 7 + 10),
                        Point.point(CRS.WGS84, 10 + -7, 7 + 10)
                }
        };

        for (Point[] points : polygonsB) {
            b.insertPolygon(Polygon.simple(points));
        }

        Point[] actual = calculator.intersect(a, b);

        Point[] expected = new Point[]{
                Point.point(CRS.WGS84, 9.999999999999998, -0.0),
                Point.point(CRS.WGS84, 9.999999999999998, 2.0196389469533202),
                Point.point(CRS.WGS84, 9.999999999999998, 1.0124625470327113),
                Point.point(CRS.WGS84, 9.999999999999998, 3.022487857997217),
                Point.point(CRS.WGS84, -0.0, 10.151081711048132),
                Point.point(CRS.WGS84, 2.0, 10.145026423873857),
                Point.point(CRS.WGS84, 0.9999999999999999, 10.149567795444106),
                Point.point(CRS.WGS84, 3.0, 10.137458722167835),
                Point.point(CRS.WGS84, 7.999999999999999, -0.0),
                Point.point(CRS.WGS84, 7.999999999999999, 2.018409655412533),
                Point.point(CRS.WGS84, 7.999999999999999, 1.011845910472504),
                Point.point(CRS.WGS84, 8.0, 3.020650050888661),
                Point.point(CRS.WGS84, -0.0, 8.077587908101195),
                Point.point(CRS.WGS84, 2.0, 8.072732142956646),
                Point.point(CRS.WGS84, 0.9999999999999999, 8.076373885311927),
                Point.point(CRS.WGS84, 3.0, 8.06666365908837),
                Point.point(CRS.WGS84, 9.000000000000002, -0.0),
                Point.point(CRS.WGS84, 9.000000000000002, 2.0193316008373126),
                Point.point(CRS.WGS84, 9.000000000000002, 1.0123083761741596),
                Point.point(CRS.WGS84, 9.000000000000002, 3.0220283718125405),
                Point.point(CRS.WGS84, -0.0, 9.110316041908312),
                Point.point(CRS.WGS84, 2.0, 9.104859269874307),
                Point.point(CRS.WGS84, 0.9999999999999999, 9.108951760627866),
                Point.point(CRS.WGS84, 3.0000000000000004, 9.098039628965156),
                Point.point(CRS.WGS84, 7.0, -0.0),
                Point.point(CRS.WGS84, 7.0, 2.016873389422308),
                Point.point(CRS.WGS84, 7.0, 1.0110752905357023),
                Point.point(CRS.WGS84, 7.0, 3.0183533080804676),
                Point.point(CRS.WGS84, -0.0, 7.052041415458704),
                Point.point(CRS.WGS84, 2.0, 7.047788717973051),
                Point.point(CRS.WGS84, 1.0, 7.050978167437823),
                Point.point(CRS.WGS84, 3.0000000000000004, 7.042473950845834)
        };

        matchPoints(actual, expected);
    }

    @Test
    public void shouldFindIntersectionsBetweenSimplePolygonsOverDateTimeLine() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.WGS84, 170, -10),
                Point.point(CRS.WGS84, 190, -10),
                Point.point(CRS.WGS84, 200, 10),
                Point.point(CRS.WGS84, 180, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(CRS.WGS84, 165, 0),
                Point.point(CRS.WGS84, 205, 0),
                Point.point(CRS.WGS84, 206, 15),
                Point.point(CRS.WGS84, 166, 15)
        );

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        Point[] actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, -165, 0), Point.point(CRS.WGS84, 175, 0)});

        a = Polygon.simple(
                Point.point(CRS.WGS84, -170, -10),
                Point.point(CRS.WGS84, -190, -10),
                Point.point(CRS.WGS84, -200, 10),
                Point.point(CRS.WGS84, -180, 10)
        );
        b = Polygon.simple(
                Point.point(CRS.WGS84, -165, 0),
                Point.point(CRS.WGS84, -205, 0),
                Point.point(CRS.WGS84, -206, 15),
                Point.point(CRS.WGS84, -166, 15)
        );

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.WGS84, 165, 0), Point.point(CRS.WGS84, -175, 0)});
    }

    private void matchPoints(Point[] actual, Point[] expected) {
        assertThat(actual.length, equalTo(expected.length));

        for (int i = 0; i < expected.length; i++) {
            boolean flag = false;
            for (int j = 0; j < actual.length; j++) {
                if (AlgoUtil.equal(actual[j].getCoordinate()[0], expected[i].getCoordinate()[0]) &&
                        AlgoUtil.equal(actual[j].getCoordinate()[1], expected[i].getCoordinate()[1])) {
                    flag = true;
                    break;
                }
            }
            assertThat("Point " + i + " is not present", flag, is(true));
        }
    }
}