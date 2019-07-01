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

    public static Intersect getCalculator(CRS crs) {
        return getCalculator(crs, AlgorithmVariant.Naive);
    }

    public static Intersect getCalculator(CRS crs, AlgorithmVariant variant) {
        if (crs == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive();
            } else {
                return getCartesianSweep();
            }
        } else {
            if (variant == AlgorithmVariant.Naive) {
                return getWGS84Naive();
            } else {
                return getWGS84Sweep();
            }
        }
    }

    public static Intersect getCalculator(Polygon a) {
        return getCalculator(a, AlgorithmVariant.Naive);
    }

    public static Intersect getCalculator(Polygon a, AlgorithmVariant variant) {
        if (a.getCRS() == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive();
            } else {
                return getCartesianSweep();
            }
        } else {
            if (variant == AlgorithmVariant.Naive) {
                return getWGS84Naive();
            } else {
                return getWGS84Sweep();
            }
        }
    }

    public static Intersect getCalculator(MultiPolyline a) {
        return getCalculator(a, AlgorithmVariant.Naive);
    }

    public static Intersect getCalculator(MultiPolyline a, AlgorithmVariant variant) {
        if (a.getCRS() == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive();
            } else {
                return getCartesianSweep();
            }
        } else {
            if (variant == AlgorithmVariant.Naive) {
                return getWGS84Naive();
            } else {
                return getWGS84Sweep();
            }
        }
    }

    public static Intersect getCalculator(Polyline a) {
        return getCalculator(a, AlgorithmVariant.Naive);
    }

    public static Intersect getCalculator(Polyline a, AlgorithmVariant variant) {
        if (a.getCRS() == CRS.Cartesian) {
            if (variant == AlgorithmVariant.Naive) {
                return getCartesianNaive();
            } else {
                return getCartesianSweep();
            }
        } else {
            if (variant == AlgorithmVariant.Naive) {
                return getWGS84Naive();
            } else {
                return getWGS84Sweep();
            }
        }
    }

    public enum AlgorithmVariant {
        Naive, MCSweepLine
    }
}
