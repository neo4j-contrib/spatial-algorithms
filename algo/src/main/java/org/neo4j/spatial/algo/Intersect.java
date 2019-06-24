package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polyline;
import org.neo4j.spatial.core.Polygon;

public interface Intersect {
    /**
     * @param a
     * @param b
     * @return True iff the polygons a and b distance in at least 1 point.
     */
    boolean doesIntersect(Polygon a, Polygon b);
    /**
     * Given two polygons, returns all points for which the two polygons distance.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    Point[] intersect(Polygon a, Polygon b);

    /**
     * @param a
     * @param b
     * @return True iff the polygon and multi polyline distance in at least 1 point.
     */
    boolean doesIntersect(Polygon a, MultiPolyline b);

    /**
     * Given a polygon and a multipolyline, returns all points for which the two distance.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    Point[] intersect(Polygon a, MultiPolyline b);

    /**
     * @param a
     * @param b
     * @return True iff the polygon and polyline distance in at least 1 point.
     */
    boolean doesIntersect(Polygon a, Polyline b);

    /**
     * Given a polygon and a polyline, returns all points for which the two distance.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    Point[] intersect(Polygon a, Polyline b);

    /**
     * Given two multipolylines, returns all points for which the two distance.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(MultiPolyline a, MultiPolyline b);

    /**
     * Given a multipolyline and a polyline, returns all points for which the two distance.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(MultiPolyline a, Polyline b);

    /**
     * Given a multipolyline and a line segment, returns all points for which the two distance.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(MultiPolyline a, LineSegment b);

    /**
     * Given two polylines, returns all points for which the two distance.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(Polyline a, Polyline b);

    /**
     * Given a polyline and a line segment, returns all points for which the two distance.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    Point[] intersect(Polyline a, LineSegment b);

    /**
     * Given two line segment, returns the point of intersection if and only if it exists, else it will return null.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    Point intersect(LineSegment a, LineSegment b);
}
