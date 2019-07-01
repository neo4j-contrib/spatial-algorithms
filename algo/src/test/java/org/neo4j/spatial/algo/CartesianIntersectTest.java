package org.neo4j.spatial.algo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.neo4j.spatial.core.*;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;

@RunWith(value = Parameterized.class)
public class CartesianIntersectTest {
    private Intersect calculator;

    @Parameterized.Parameters
    public static Collection data() {
        IntersectCalculator.AlgorithmVariant[] variants = new IntersectCalculator.AlgorithmVariant[]{IntersectCalculator.AlgorithmVariant.Naive, IntersectCalculator.AlgorithmVariant.MCSweepLine};
        return Arrays.asList(variants);
    }

    public CartesianIntersectTest(IntersectCalculator.AlgorithmVariant variant) {
        this.calculator = IntersectCalculator.getCalculator(CRS.Cartesian, variant);
    }

    @Test
    public void shouldFindIntersectionBetweenLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 10,10));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 10), Point.point(CRS.Cartesian, 10,0));

        assertThat(calculator.intersect(a, b), equalTo(Point.point(CRS.Cartesian, 5,5)));

        a = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 10,10));
        b = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 10), Point.point(CRS.Cartesian, 10,10));
        assertThat(calculator.intersect(a, b), equalTo(Point.point(CRS.Cartesian, 10,10)));

        a = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 0,10));
        b = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 5), Point.point(CRS.Cartesian, 0,15));
        assertThat(calculator.intersect(a, b), equalTo(Point.point(CRS.Cartesian, 0,5)));

        a = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 5,5));
        b = LineSegment.lineSegment(Point.point(CRS.Cartesian, -5, -5), Point.point(CRS.Cartesian, 10,10));
        assertThat(calculator.intersect(a, b), equalTo(Point.point(CRS.Cartesian, 0,0)));
    }

    @Test
    public void shouldFindIntersectionBetweenPolylineAndLineSegment() {
        Polyline a = Polyline.polyline(
                Point.point(CRS.Cartesian, -5, -5),
                Point.point(CRS.Cartesian, -5, 0),
                Point.point(CRS.Cartesian, -2.5, 2.5),
                Point.point(CRS.Cartesian, 2.5, 2.5)
        );
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 0, 10));

        Point[] actual = calculator.intersect(a, b);
        Point[] expected = new Point[]{Point.point(CRS.Cartesian, 0, 2.5)};
        matchPoints(actual, expected);

        a = Polyline.polyline(
                Point.point(CRS.Cartesian, -5, -5),
                Point.point(CRS.Cartesian, -5, 0),
                Point.point(CRS.Cartesian, -2.5, 2.5),
                Point.point(CRS.Cartesian, 2.5, 2.5),
                Point.point(CRS.Cartesian, 2.5, 5),
                Point.point(CRS.Cartesian, -2.5, 5)
        );
        b = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 0, 10));

        actual = calculator.intersect(a, b);
        expected = new Point[]{
                Point.point(CRS.Cartesian, 0, 2.5),
                Point.point(CRS.Cartesian, 0, 5)
        };
        matchPoints(actual, expected);
    }

    @Test
    public void shouldFindIntersectionBetweenLineSegmentsWithAccuracy() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 1, 1));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 1e-2), Point.point(CRS.Cartesian, 1, 0.5));

        double[] actual = calculator.intersect(a, b).getCoordinate();
        double[] test = Point.point(CRS.Cartesian, 0.0196078, 0.0196078).getCoordinate();
        assertThat(actual[0], closeTo(test[0], 1e-6));
        assertThat(actual[1], closeTo(test[1], 1e-6));
    }

    @Test
    public void shouldNotFindIntersectionBetweenLineSegments() {
        LineSegment a = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 10, 10));
        LineSegment b = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 10), Point.point(CRS.Cartesian, -10, 0));

        assertThat(calculator.intersect(a, b), is(nullValue()));

        a = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0, 0), Point.point(CRS.Cartesian, 1,1));
        b = LineSegment.lineSegment(Point.point(CRS.Cartesian, 0,25e-9), Point.point(CRS.Cartesian, 1,25+1e-9));
        assertThat(calculator.intersect(a, b), is(nullValue()));
    }

    @Test
    public void shouldFindIntersectionsBetweenPolylines() {
        Polyline a = Polyline.polyline(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 20, 10),
                Point.point(CRS.Cartesian, -0, 10)
        );
        Polyline b = Polyline.polyline(
                Point.point(CRS.Cartesian, -15, 0),
                Point.point(CRS.Cartesian, 25, 0),
                Point.point(CRS.Cartesian, 26, 15),
                Point.point(CRS.Cartesian, -14, 15)
        );

        Point[] actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.Cartesian, 15, 0)});

        a = Polyline.polyline(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );
        b = Polyline.polyline(
                Point.point(CRS.Cartesian, -5, -5),
                Point.point(CRS.Cartesian, 15, -5),
                Point.point(CRS.Cartesian, 15, 15),
                Point.point(CRS.Cartesian, -5, 15)
        );

        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.Cartesian, 10, -5)});
    }

    @Test
    public void shouldFindIntersectionsBetweenPolygonAndPolyline() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 20, 10),
                Point.point(CRS.Cartesian, -0, 10)
        );
        Polyline b = Polyline.polyline(
                Point.point(CRS.Cartesian, -15, 0),
                Point.point(CRS.Cartesian, 25, 0),
                Point.point(CRS.Cartesian, 26, 15),
                Point.point(CRS.Cartesian, -14, 15)
        );

        Point[] actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.Cartesian, 15, 0), Point.point(CRS.Cartesian, -5, 0)});

        a = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );
        b = Polyline.polyline(
                Point.point(CRS.Cartesian, -5, -5),
                Point.point(CRS.Cartesian, 15, -5),
                Point.point(CRS.Cartesian, 15, 15),
                Point.point(CRS.Cartesian, -5, 15)
        );

        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.Cartesian, 10, -5)});
    }

    @Test
    public void shouldFindIntersectionsBetweenSimplePolygons() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 20, 10),
                Point.point(CRS.Cartesian, -0, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(CRS.Cartesian, -15, 0),
                Point.point(CRS.Cartesian, 25, 0),
                Point.point(CRS.Cartesian, 26, 15),
                Point.point(CRS.Cartesian, -14, 15)
        );

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        Point[] actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.Cartesian, 15, 0), Point.point(CRS.Cartesian, -5, 0)});

        a = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );
        b = Polygon.simple(
                Point.point(CRS.Cartesian, -5, -5),
                Point.point(CRS.Cartesian, 15, -5),
                Point.point(CRS.Cartesian, 15, 15),
                Point.point(CRS.Cartesian, -5, 15)
        );

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.Cartesian, -5, 10), Point.point(CRS.Cartesian, 10, -5)});

        a = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 0, 5)
        );
        b = Polygon.simple(
                Point.point(CRS.Cartesian, -10, 10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, 0, -5)
        );

        assertThat(calculator.doesIntersect(a, b), equalTo(true));
        actual = calculator.intersect(a, b);
        matchPoints(actual, new Point[]{Point.point(CRS.Cartesian, 3.3333333333333335, 0), Point.point(CRS.Cartesian, -3.3333333333333335, 0)});

        a = Polygon.simple(
                Point.point(CRS.Cartesian, -3.073942953027313, -0.3631908536811643),
                Point.point(CRS.Cartesian, 3.957307046972687, 0.33992971981693054),
                Point.point(CRS.Cartesian, 4.001252359472687, -3.5250298989084747),
                Point.point(CRS.Cartesian, -0.8327320155273128, -2.7132528509732756),
                Point.point(CRS.Cartesian, 1.2107250157226872, -1.044287806764632),
                Point.point(CRS.Cartesian, -3.930876546777313, -1.110194121461247),
                Point.point(CRS.Cartesian, -5.051482015527313, 1.262681056229549),
                Point.point(CRS.Cartesian, -2.898161703027313, 0.8452721446430446),
                Point.point(CRS.Cartesian, -3.073942953027313, -0.3631908536811643)
        );
        b = Polygon.simple(
                Point.point(CRS.Cartesian, 0.4197093907226872,3.0411394778853147),
                Point.point(CRS.Cartesian, -3.403532796777313,-0.6488239497102567),
                Point.point(CRS.Cartesian, 2.045685953222687,-4.511356587129941),
                Point.point(CRS.Cartesian, 7.165314859472687,-2.8888240176242217),
                Point.point(CRS.Cartesian, 6.901642984472687,0.7134484325376148),
                Point.point(CRS.Cartesian, 4.220978921972687,-1.7471997776342576),
                Point.point(CRS.Cartesian, 0.5295726719726872,-2.4059429401676002),
                Point.point(CRS.Cartesian, 3.891389078222687,0.09823244716972954),
                Point.point(CRS.Cartesian, 6.132600015722687,1.6580596189713295),
                Point.point(CRS.Cartesian, 3.627717203222687,1.877681460711756),
                Point.point(CRS.Cartesian, 1.9358226719726872,-0.011630785875617151),
                Point.point(CRS.Cartesian, -0.5251148280273128,-1.3738038176337573),
                Point.point(CRS.Cartesian, -1.1842945155273128,0.12020506336406792),
                Point.point(CRS.Cartesian, 1.2986156407226872,0.7134484325376148),
                Point.point(CRS.Cartesian, 1.4304515782226872,2.075317616513406),
                Point.point(CRS.Cartesian, 2.660920328222687,3.43601485945374),
                Point.point(CRS.Cartesian, 4.330842203222687,3.3263428389465592),
                Point.point(CRS.Cartesian, 3.122346109472687,4.115684615656987),
                Point.point(CRS.Cartesian, 0.4197093907226872,3.0411394778853147)
        );

        Point[] expected = new Point[]{
                Point.point(CRS.Cartesian, -0.3849795856968765, -2.788444477072308),
                Point.point(CRS.Cartesian, -0.6739799509139212, -2.583594315225338),
                Point.point(CRS.Cartesian, 0.04315920168874296, -1.0592539528250269),
                Point.point(CRS.Cartesian, 3.9595160761077635, 0.14564714636480858),
                Point.point(CRS.Cartesian, -0.6599227091525858, -1.0682662298578034),
                Point.point(CRS.Cartesian, -1.059875977104757, -0.1617854240386056),
                Point.point(CRS.Cartesian, 3.981523964154006, -1.7899312749322576),
                Point.point(CRS.Cartesian, -2.773565440956476, -1.0953594227754488),
                Point.point(CRS.Cartesian, -3.0684490440254777, -0.32542126366585933),
                Point.point(CRS.Cartesian, 2.082783957901631, 0.15247859101009514)
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
                    Point.point(CRS.Cartesian, -10, -10),
                    Point.point(CRS.Cartesian, 10, -10),
                    Point.point(CRS.Cartesian, 10, 10),
                    Point.point(CRS.Cartesian, -10, 10),
                    Point.point(CRS.Cartesian, -10, -10)
                },
                {
                    Point.point(CRS.Cartesian, -9, -9),
                    Point.point(CRS.Cartesian, 9, -9),
                    Point.point(CRS.Cartesian, 9, 9),
                    Point.point(CRS.Cartesian, -9, 9),
                    Point.point(CRS.Cartesian, -9, -9)
                },
                {
                    Point.point(CRS.Cartesian, -8, -8),
                    Point.point(CRS.Cartesian, 8, -8),
                    Point.point(CRS.Cartesian, 8, 8),
                    Point.point(CRS.Cartesian, -8, 8)
                },
                {
                    Point.point(CRS.Cartesian, -7, -7),
                    Point.point(CRS.Cartesian, 7, -7),
                    Point.point(CRS.Cartesian, 7, 7),
                    Point.point(CRS.Cartesian, -7, 7)
                }
        };

        for (Point[] points : polygonsA) {
            a.insertPolygon(Polygon.simple(points));
        }

        MultiPolygon b = new MultiPolygon();
        Point[][] polygonsB = new Point[][]{
                {
                        Point.point(CRS.Cartesian, 10 + -10, -10 + 10),
                        Point.point(CRS.Cartesian, 10 + 10, -10 + 10),
                        Point.point(CRS.Cartesian, 10 + 10, 10 + 10),
                        Point.point(CRS.Cartesian, 10 + -10, 10 + 10),
                        Point.point(CRS.Cartesian, 10 + -10, -10 + 10)
                },
                {
                        Point.point(CRS.Cartesian, 10 + -9, -9 + 10),
                        Point.point(CRS.Cartesian, 10 + 9, -9 + 10),
                        Point.point(CRS.Cartesian, 10 + 9, 9 + 10),
                        Point.point(CRS.Cartesian, 10 + -9, 9 + 10),
                        Point.point(CRS.Cartesian, 10 + -9, -9 + 10)
                },
                {
                        Point.point(CRS.Cartesian, 10 + -8, -8 + 10),
                        Point.point(CRS.Cartesian, 10 + 8, -8 + 10),
                        Point.point(CRS.Cartesian, 10 + 8, 8 + 10),
                        Point.point(CRS.Cartesian, 10 + -8, 8 + 10)
                },
                {
                        Point.point(CRS.Cartesian, 10 + -7, -7 + 10),
                        Point.point(CRS.Cartesian, 10 + 7, -7 + 10),
                        Point.point(CRS.Cartesian, 10 + 7, 7 + 10),
                        Point.point(CRS.Cartesian, 10 + -7, 7 + 10)
                }
        };

        for (Point[] points : polygonsB) {
            b.insertPolygon(Polygon.simple(points));
        }

        Point[] actual = calculator.intersect(a, b);

        Point[] expected = new Point[]{
                Point.point(CRS.Cartesian, 10.0, 1.0),
                Point.point(CRS.Cartesian, 9.0, 1.0),
                Point.point(CRS.Cartesian, 8.0, 1.0),
                Point.point(CRS.Cartesian, 7.0, 1.0),
                Point.point(CRS.Cartesian, 3.0, 7.0),
                Point.point(CRS.Cartesian, 3.0, 8.0),
                Point.point(CRS.Cartesian, 3.0, 9.0),
                Point.point(CRS.Cartesian, 10.0, 0.0),
                Point.point(CRS.Cartesian, 10.0, 2.0),
                Point.point(CRS.Cartesian, 3.0, 10.0),
                Point.point(CRS.Cartesian, 0.0, 7.0),
                Point.point(CRS.Cartesian, 2.0, 7.0),
                Point.point(CRS.Cartesian, 9.0, 0.0),
                Point.point(CRS.Cartesian, 9.0, 2.0),
                Point.point(CRS.Cartesian, 8.0, 0.0),
                Point.point(CRS.Cartesian, 8.0, 2.0),
                Point.point(CRS.Cartesian, 0.0, 8.0),
                Point.point(CRS.Cartesian, 2.0, 8.0),
                Point.point(CRS.Cartesian, 0.0, 9.0),
                Point.point(CRS.Cartesian, 2.0, 9.0),
                Point.point(CRS.Cartesian, 10.0, 3.0),
                Point.point(CRS.Cartesian, 0.0, 10.0),
                Point.point(CRS.Cartesian, 2.0, 10.0),
                Point.point(CRS.Cartesian, 7.0, 0.0),
                Point.point(CRS.Cartesian, 7.0, 2.0),
                Point.point(CRS.Cartesian, 9.0, 3.0),
                Point.point(CRS.Cartesian, 8.0, 3.0),
                Point.point(CRS.Cartesian, 7.0, 3.0),
                Point.point(CRS.Cartesian, 1.0, 7.0),
                Point.point(CRS.Cartesian, 1.0, 8.0),
                Point.point(CRS.Cartesian, 1.0, 9.0),
                Point.point(CRS.Cartesian, 1.0, 10.0)
        };

        matchPoints(actual, expected);
    }

    @Test
    public void shouldFindIntersectionsBetweenSharedVertex() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 0, 5)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(CRS.Cartesian, -10, 10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, 0, 5)
        );

        matchPoints(calculator.intersect(a, b), new Point[]{Point.point(CRS.Cartesian, 0, 5)});
    }

    @Test
    public void shouldNotFindIntersectionsBetweenSimplePolygons() {
        Polygon.SimplePolygon a = Polygon.simple(
                Point.point(CRS.Cartesian, -10, -10),
                Point.point(CRS.Cartesian, 10, -10),
                Point.point(CRS.Cartesian, 10, 10),
                Point.point(CRS.Cartesian, -10, 10)
        );
        Polygon.SimplePolygon b = Polygon.simple(
                Point.point(CRS.Cartesian, -100, -150),
                Point.point(CRS.Cartesian, 150, -150),
                Point.point(CRS.Cartesian, 150, 150)
        );

        assertThat(calculator.intersect(a, b), org.hamcrest.Matchers.emptyArray());
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