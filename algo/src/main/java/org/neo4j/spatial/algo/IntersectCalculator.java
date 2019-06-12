package org.neo4j.spatial.algo;

import org.neo4j.spatial.algo.cartesian.intersect.CartesianIntersect;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianMCSweepLineIntersect;
import org.neo4j.spatial.algo.cartesian.intersect.CartesianNaiveIntersect;
import org.neo4j.spatial.algo.wgs84.intersect.WGS84Intersect;
import org.neo4j.spatial.algo.wgs84.intersect.WGS84MCSweepLineIntersect;
import org.neo4j.spatial.algo.wgs84.intersect.WGS84NaiveIntersect;
import org.neo4j.spatial.core.*;

public class IntersectCalculator {
    private static CartesianIntersect cartesianNaive;
    private static WGS84Intersect wgs84Naive;

    private static CartesianIntersect cartesianSweep;
    private static WGS84Intersect wgs84Sweep;

    private static Intersect getCartesianNaive() {
        if (cartesianNaive == null) {
            cartesianNaive = new CartesianNaiveIntersect();
        }
        return cartesianNaive;
    }

    private static Intersect getWGS84Naive() {
        if (wgs84Naive == null) {
            wgs84Naive = new WGS84NaiveIntersect();
        }
        return wgs84Naive;
    }

    private static Intersect getCartesianSweep() {
        if (cartesianSweep == null) {
            cartesianSweep = new CartesianMCSweepLineIntersect();
        }
        return cartesianSweep;
    }

    private static Intersect getWGS84Sweep() {
        if (wgs84Sweep == null) {
            wgs84Sweep = new WGS84MCSweepLineIntersect();
        }
        return wgs84Sweep;
    }

    /**
     * Given two polygons, returns all points for which the two polygons intersect.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    public static Point[] intersect(Polygon a, Polygon b) {
        return intersect(a, b, AlgorithmVariant.Naive);
    }

    /**
     * Given two polygons, returns all points for which the two polygons intersect.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    public static Point[] intersect(Polygon a, Polygon b, AlgorithmVariant variant) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive().intersect(a, b);
            } else {
                return getCartesianSweep().intersect(a, b);
            }
        } else {
            if (variant == AlgorithmVariant.MCSweepLine) {
                return getWGS84Naive().intersect(a, b);
            } else {
                return getWGS84Sweep().intersect(a, b);
            }
        }
    }

    /**
     * @param a
     * @param b
     * @return True iff the polygons a and b intersect in at least 1 point.
     */
    public static boolean doesIntersect(Polygon a, Polygon b) {
        return doesIntersect(a, b, AlgorithmVariant.Naive);
    }

    /**
     * @param a
     * @param b
     * @return True iff the polygons a and b intersect in at least 1 point.
     */
    public static boolean doesIntersect(Polygon a, Polygon b, AlgorithmVariant variant) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive().doesIntersect(a, b);
            } else {
                return getCartesianSweep().doesIntersect(a, b);
            }
        } else {
            if (variant == AlgorithmVariant.MCSweepLine) {
                return getWGS84Naive().doesIntersect(a, b);
            } else {
                return getWGS84Sweep().doesIntersect(a, b);
            }
        }
    }

    /**
     * Given a polygon and a polyline, returns all points for which the two intersect.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    public static Point[] intersect(Polygon a, Polyline b) {
        return intersect(a, b, AlgorithmVariant.Naive);
    }

    /**
     * Given a polygon and a polyline, returns all points for which the two intersect.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    public static Point[] intersect(Polygon a, Polyline b, AlgorithmVariant variant) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive().intersect(a, b);
            } else {
                return getCartesianSweep().intersect(a, b);
            }
        } else {
            if (variant == AlgorithmVariant.MCSweepLine) {
                return getWGS84Naive().intersect(a, b);
            } else {
                return getWGS84Sweep().intersect(a, b);
            }
        }
    }

    /**
     * Given two polylines, returns all points for which the two intersect.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    public static Point[] intersect(Polyline a, Polyline b) {
        return intersect(a, b, AlgorithmVariant.Naive);
    }

    /**
     * Given two polylines, returns all points for which the two intersect.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    public static Point[] intersect(Polyline a, Polyline b, AlgorithmVariant variant) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive().intersect(a, b);
            } else {
                return getCartesianSweep().intersect(a, b);
            }
        } else {
            if (variant == AlgorithmVariant.MCSweepLine) {
                return getWGS84Naive().intersect(a, b);
            } else {
                return getWGS84Sweep().intersect(a, b);
            }
        }
    }

    /**
     * Given a polyline and a line segment, returns all points for which the two intersect.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    public static Point[] intersect(Polyline a, LineSegment b) {
        return intersect(a, b, AlgorithmVariant.Naive);
    }

    /**
     * Given a polyline and a line segment, returns all points for which the two intersect.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    public static Point[] intersect(Polyline a, LineSegment b, AlgorithmVariant variant) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive().intersect(a, b);
            } else {
                return getCartesianSweep().intersect(a, b);
            }
        } else {
            if (variant == AlgorithmVariant.MCSweepLine) {
                return getWGS84Naive().intersect(a, b);
            } else {
                return getWGS84Sweep().intersect(a, b);
            }
        }
    }

    /**
     * Given two line segment returns the point of intersection if and only if it exists, else it will return null.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    public static Point intersect(LineSegment a, LineSegment b) {
        return intersect(a, b, AlgorithmVariant.Naive);
    }

    /**
     * Given two line segment returns the point of intersection if and only if it exists, else it will return null.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    public static Point intersect(LineSegment a, LineSegment b, AlgorithmVariant variant) {
        if (CRSChecker.check(a, b) == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive().intersect(a, b);
            } else {
                return getCartesianSweep().intersect(a, b);
            }
        } else {
            if (variant == AlgorithmVariant.MCSweepLine) {
                return getWGS84Naive().intersect(a, b);
            } else {
                return getWGS84Sweep().intersect(a, b);
            }
        }
    }

    public enum AlgorithmVariant {
        Naive, MCSweepLine
    }
}
