package org.neo4j.spatial.algo.wgs84;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.algo.wgs84.intersect.Intersect;
import org.neo4j.spatial.algo.wgs84.intersect.MCSweepLineIntersect;
import org.neo4j.spatial.algo.wgs84.intersect.NaiveIntersect;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

@RunWith(value = Parameterized.class)
public class IntersectTest {
    private Class impl;
    private Intersect polygonImpl;

    @Parameterized.Parameters
    public static Collection data() {
        Class[] classes = new Class[]{NaiveIntersect.class, MCSweepLineIntersect.class};
        return Arrays.asList(classes);
    }

    public IntersectTest(Class impl) {
        this.impl = impl;
    }

    @Before
    public void setup() throws IllegalAccessException, InstantiationException {
        polygonImpl = (Intersect) impl.newInstance();
    }

    @Test
    public void intersect() {
        Point p = Point.point(-50, 0);
        Point q = Point.point(-30, 0);
        Point r = Point.point(-40, -5);
        Point s = Point.point(-40, 3);

        LineSegment a = LineSegment.lineSegment(p, q);
        LineSegment b = LineSegment.lineSegment(r, s);

        Point actual = Intersect.intersect(a, b);
        Point expected = Point.point(-40, 0);
        matchPoints(new Point[]{actual}, new Point[]{expected});
    }

    @Test
    public void shouldFindIntersectionsBetweenSharedVertex() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(0, 5)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(-10, 10),
                Point.point(0, 5),
                Point.point(10, 10)
        );

        matchPoints(polygonImpl.intersect(a, b), new Point[]{Point.point(0, 5)});
    }

    @Test
    public void shouldNotFindIntersectionsBetweenSimplePolygons() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(10, 10),
                Point.point(-10, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(-100, -150),
                Point.point(150, 150),
                Point.point(150, -150)
        );

        assertThat(polygonImpl.intersect(a, b), org.hamcrest.Matchers.emptyArray());
    }

    @Test
    public void shouldFindIntersectionsBetweenSimplePolygons() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(20, 10),
                Point.point(-0, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(-15, 0),
                Point.point(25, 0),
                Point.point(26, 15),
                Point.point(-14, 15)
        );

        assertThat(polygonImpl.doesIntersect(a, b), equalTo(true));
        Point[] actual = polygonImpl.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(15, 0), Point.point(-5, 0)});

        a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(10, 10),
                Point.point(-10, 10)
        );
        b = Polygon.simple(
                Point.point(-5, -5),
                Point.point(15, -5),
                Point.point(15, 15),
                Point.point(-5, 15)
        );

        assertThat(polygonImpl.doesIntersect(a, b), equalTo(true));
        actual = polygonImpl.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(10.0, -5.0575148968282075), Point.point(-4.999999999999999, 10.113252586707066)});

        a = Polygon.simple(
                Point.point(-10, -10),
                Point.point(10, -10),
                Point.point(0, 5)
        );
        b = Polygon.simple(
                Point.point(0, -5),
                Point.point(10, 10),
                Point.point(-10, 10)
        );

        assertThat(polygonImpl.doesIntersect(a, b), equalTo(true));
        actual = polygonImpl.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(3.3124813211817257, 0), Point.point(-3.3124813211817257, 0)});

        a = Polygon.simple(
                Point.point(-3.073942953027313, -0.3631908536811643),
                Point.point(-2.898161703027313, 0.8452721446430446),
                Point.point(-5.051482015527313, 1.262681056229549),
                Point.point(-3.930876546777313, -1.110194121461247),
                Point.point(1.2107250157226872, -1.044287806764632),
                Point.point(-0.8327320155273128, -2.7132528509732756),
                Point.point(4.001252359472687, -3.5250298989084747),
                Point.point(3.957307046972687, 0.33992971981693054)
        );
        b = Polygon.simple(
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
                Point.point(3.122346109472687,4.115684615656987)
        );

        Point[] expected = new Point[]{
                Point.point(-3.068389645976, -0.325012193039),
                Point.point(-2.775189790165, -1.096140943706),
                Point.point(-1.059857818944, -0.161880498908),
                Point.point(-0.677674056166, -2.586831429144),
                Point.point(-0.659524248473, -1.069263100502),
                Point.point(-0.394238446406, -2.787821990105),
                Point.point(0.042154730523, -1.060025023954),
                Point.point(2.082767597071, 0.152541643688),
                Point.point(3.959513274862, 0.145673350408),
                Point.point(3.981505774021, -1.790199797499)
        };

        assertThat(polygonImpl.doesIntersect(a, b), equalTo(true));
        actual = polygonImpl.intersect(a, b);
        matchPoints(actual, expected);
    }

    @Test
    public void shouldFindIntersectionsBetweenMultiPolygons() {
        MultiPolygon a = new MultiPolygon();
        Point[][] polygonsA = new Point[][]{
                {
                        Point.point(-10, -10),
                        Point.point(10, -10),
                        Point.point(10, 10),
                        Point.point(-10, 10),
                        Point.point(-10, -10)
                },
                {
                        Point.point(-9, -9),
                        Point.point(9, -9),
                        Point.point(9, 9),
                        Point.point(-9, 9),
                        Point.point(-9, -9)
                },
                {
                        Point.point(-8, -8),
                        Point.point(8, -8),
                        Point.point(8, 8),
                        Point.point(-8, 8)
                },
                {
                        Point.point(-7, -7),
                        Point.point(7, -7),
                        Point.point(7, 7),
                        Point.point(-7, 7)
                }
        };

        for (Point[] points : polygonsA) {
            a.insertPolygon(Polygon.simple(points));
        }

        MultiPolygon b = new MultiPolygon();
        Point[][] polygonsB = new Point[][]{
                {
                        Point.point(10 + -10, -10 + 10),
                        Point.point(10 + 10, -10 + 10),
                        Point.point(10 + 10, 10 + 10),
                        Point.point(10 + -10, 10 + 10),
                        Point.point(10 + -10, -10 + 10)
                },
                {
                        Point.point(10 + -9, -9 + 10),
                        Point.point(10 + 9, -9 + 10),
                        Point.point(10 + 9, 9 + 10),
                        Point.point(10 + -9, 9 + 10),
                        Point.point(10 + -9, -9 + 10)
                },
                {
                        Point.point(10 + -8, -8 + 10),
                        Point.point(10 + 8, -8 + 10),
                        Point.point(10 + 8, 8 + 10),
                        Point.point(10 + -8, 8 + 10)
                },
                {
                        Point.point(10 + -7, -7 + 10),
                        Point.point(10 + 7, -7 + 10),
                        Point.point(10 + 7, 7 + 10),
                        Point.point(10 + -7, 7 + 10)
                }
        };

        for (Point[] points : polygonsB) {
            b.insertPolygon(Polygon.simple(points));
        }

        Point[] actual = polygonImpl.intersect(a, b);

        Point[] expected = new Point[]{
                Point.point(9.999999999999998, -0.0),
                Point.point(9.999999999999998, 2.0196389469533202),
                Point.point(9.999999999999998, 1.0124625470327113),
                Point.point(9.999999999999998, 3.022487857997217),
                Point.point(-0.0, 10.151081711048132),
                Point.point(2.0, 10.145026423873857),
                Point.point(0.9999999999999999, 10.149567795444106),
                Point.point(3.0, 10.137458722167835),
                Point.point(7.999999999999999, -0.0),
                Point.point(7.999999999999999, 2.018409655412533),
                Point.point(7.999999999999999, 1.011845910472504),
                Point.point(8.0, 3.020650050888661),
                Point.point(-0.0, 8.077587908101195),
                Point.point(2.0, 8.072732142956646),
                Point.point(0.9999999999999999, 8.076373885311927),
                Point.point(3.0, 8.06666365908837),
                Point.point(9.000000000000002, -0.0),
                Point.point(9.000000000000002, 2.0193316008373126),
                Point.point(9.000000000000002, 1.0123083761741596),
                Point.point(9.000000000000002, 3.0220283718125405),
                Point.point(-0.0, 9.110316041908312),
                Point.point(2.0, 9.104859269874307),
                Point.point(0.9999999999999999, 9.108951760627866),
                Point.point(3.0000000000000004, 9.098039628965156),
                Point.point(7.0, -0.0),
                Point.point(7.0, 2.016873389422308),
                Point.point(7.0, 1.0110752905357023),
                Point.point(7.0, 3.0183533080804676),
                Point.point(-0.0, 7.052041415458704),
                Point.point(2.0, 7.047788717973051),
                Point.point(1.0, 7.050978167437823),
                Point.point(3.0000000000000004, 7.042473950845834)
        };

        matchPoints(actual, expected);
    }

    @Test
    public void shouldFindIntersectionsBetweenSimplePolygonsOverDateTimeLine() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(170, -10),
                Point.point(190, -10),
                Point.point(200, 10),
                Point.point(180, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(165, 0),
                Point.point(205, 0),
                Point.point(206, 15),
                Point.point(166, 15)
        );

        assertThat(polygonImpl.doesIntersect(a, b), equalTo(true));
        Point[] actual = polygonImpl.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(-165, 0), Point.point(175, 0)});

        a = Polygon.simple(
                Point.point(-170, -10),
                Point.point(-190, -10),
                Point.point(-200, 10),
                Point.point(-180, 10)
        );
        b = Polygon.simple(
                Point.point(-165, 0),
                Point.point(-205, 0),
                Point.point(-206, 15),
                Point.point(-166, 15)
        );

        assertThat(polygonImpl.doesIntersect(a, b), equalTo(true));
        actual = polygonImpl.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(165, 0), Point.point(-175, 0)});
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