package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class Distance {
    /**
     * @param a
     * @param b
     * @return The minimum distance between two polygons. Returns 0 if one polygon is (partially) contained by the other
     */
    public abstract double distance(Polygon a, Polygon b);

    /**
     * @param a
     * @param b
     * @return The minimum distance between two polygons, and the closest points on those polygons. Returns 0 if one polygon is (partially) contained by the other
     */
    public abstract DistanceResult distanceAndEndpoints(Polygon a, Polygon b);

    /**
     * @param polygon
     * @param multiPolyline
     * @return The minimum distance between a polygon and multi polyline. Returns 0 if the multi polyline intersects with or is (partially) containted by the polygon
     */
    public abstract double distance(Polygon polygon, MultiPolyline multiPolyline);

    /**
     * @param polygon
     * @param polyline
     * @return The minimum distance between a polygon and polyline. Returns 0 if the polyline intersects with or is (partially) containted by the polygon
     */
    public abstract double distance(Polygon polygon, Polyline polyline);

    /**
     * @param polygon
     * @param lineSegment
     * @return The minimum distance between a polygon and line segment. Returns 0 if the line segment is (partially) contained by the polygon
     */
    public abstract double distance(Polygon polygon, LineSegment lineSegment);

    /**
     * @param polygon
     * @param point
     * @return The minimum distance between a polygon and point. Returns 0 if point is within the polygon
     */
    public abstract double distance(Polygon polygon, Point point);

    /**
     * @param a
     * @param b
     * @return The minimum distance between two polylines. Returns 0 if they distance
     */
    public abstract double distance(Polyline a, Polyline b);

    /**
     * @param a
     * @param b
     * @return The minimum distance between two multipolylines. Returns 0 if they distance
     */
    public abstract double distance(MultiPolyline a, MultiPolyline b);

    /**
     * @param a
     * @param b
     * @return The minimum distance between a multipolyline and an polyline. Returns 0 if they distance
     */
    public abstract double distance(MultiPolyline a, Polyline b);

    /**
     * @param a
     * @param b
     * @return The minimum distance between a multipolyline and a line segment. Returns 0 if they distance
     */
    public abstract double distance(MultiPolyline a, LineSegment b);

    /**
     * @param polyline
     * @param lineSegment
     * @return The minimum distance between a polyline and line segment. Returns 0 if they distance
     */
    public abstract double distance(Polyline polyline, LineSegment lineSegment);

    /**
     * @param polyline
     * @param point
     * @return The minimum distance between a polyline and point
     */
    public abstract double distance(Polyline polyline, Point point);

    /**
     * @param lineSegment
     * @return The distance between the two end points of a line segment
     */
    public abstract double distance(LineSegment lineSegment);

    /**
     * @param lineSegment
     * @param point
     * @return The minimum distance between a line segment and a point
     */
    public abstract double distance(LineSegment lineSegment, Point point);

    /**
     * @param a
     * @param b
     * @return The minimum distance between two line segments
     */
    public abstract double distance(LineSegment a, LineSegment b);

    /**
     * @param p1
     * @param p2
     * @return The minimum distance between two points
     */
    public abstract double distance(Point p1, Point p2);

    /**
     * @param p1
     * @param p2
     * @return The minimum distance between two coordinates
     */
    public abstract double distance(double[] p1, double[] p2);

    protected double getMinDistance(LineSegment[] aLS, LineSegment[] bLS) {
        double minDistance = Double.MAX_VALUE;

        for (LineSegment aLineSegment : aLS) {
            for (LineSegment bLineSegment : bLS) {
                double current = distance(aLineSegment, bLineSegment);
                if (current < minDistance) {
                    minDistance = current;
                }
            }
        }
        return minDistance;
    }

    protected DistanceResult getMinDistanceAndEndpoints(LineSegment[] aLS, LineSegment[] bLS) {
        DistanceResult minDistance = DistanceResult.NO_RESULT;

        for (LineSegment aLineSegment : aLS) {
            for (LineSegment bLineSegment : bLS) {
                minDistance = minDistance.min(distanceAndEndpoints(aLineSegment, bLineSegment));
            }
        }
        return minDistance;
    }

    protected abstract DistanceResult distanceAndEndpoints(LineSegment a, LineSegment b);

    public static class DistanceResult {
        public static double INVALID_DISTANCE = Double.NaN;
        private double distance;
        private Point start;
        private Point end;
        private String message;
        private String error;
        public static DistanceResult NO_RESULT = new DistanceResult(INVALID_DISTANCE, null, null);
        public static DistanceResult OVERLAP_RESULT = new DistanceResult(0, null, null);

        public DistanceResult(double distance, Point start, Point end) {
            this.distance = distance;
            this.start = start;
            this.end = end;
        }

        public DistanceResult withMessage(String message) {
            this.message = message;
            return this;
        }

        public DistanceResult withError(String error) {
            this.error = error;
            return this;
        }

        public DistanceResult withError(Exception e) {
            this.error = e.getMessage() + "\n" + e.getStackTrace()[0].toString();
            return this;
        }

        public DistanceResult min(DistanceResult other) {
            if (this.isEmpty()) return other;
            else if (other.isEmpty()) return this;
            else {
                if (this.distance < other.distance) {
                    return this;
                } else {
                    return other;
                }
            }
        }

        public boolean isEmpty() {
            return Double.isNaN(this.distance);
        }

        public Map<String, Object> asMap() {
            return asMap((v) -> v);
        }

        public Map<String, Object> asMap(Function<Point, Object> pointMapper) {
            HashMap<String, Object> result = new HashMap<>();
            result.put("distance", distance);
            if (start != null) {
                result.put("start", pointMapper.apply(start));
            } else {
                System.out.println("No start point was found for distance " + distance);
            }
            if (end != null) {
                result.put("end", pointMapper.apply(end));
            } else {
                System.out.println("No end point was found for distance " + distance);
            }
            if (message != null) {
                result.put("message", message);
            }
            if (error != null) {
                result.put("error", error);
            }
            return result;
        }
    }
}
