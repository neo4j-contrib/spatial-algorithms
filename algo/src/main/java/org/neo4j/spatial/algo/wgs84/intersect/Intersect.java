package org.neo4j.spatial.algo.wgs84.intersect;

import org.neo4j.spatial.algo.wgs84.WGSUtil;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public interface Intersect {
    /**
     * Given two polygons, returns all points for which the two polygons intersect.
     *
     * @param a
     * @param b
     * @return Array of intersections
     */
    Point[] intersect(Polygon a, Polygon b);

    /**
     * @param a
     * @param b
     * @return True iff the polygons a and b intersect in at least 1 point.
     */
    boolean doesIntersect(Polygon a, Polygon b);

    /**
     * Given two line segment returns the point of intersection if and only if it exists, else it will return null.
     *
     * @param a
     * @param b
     * @return Point of intersection if it exists, else null
     */
    static Point intersect(LineSegment a, LineSegment b) {
        return WGSUtil.intersect(a, b);
    }
}
