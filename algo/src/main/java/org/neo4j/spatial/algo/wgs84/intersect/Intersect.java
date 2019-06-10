package org.neo4j.spatial.algo.wgs84.intersect;

import org.neo4j.spatial.algo.wgs84.WGSUtil;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public abstract class Intersect implements org.neo4j.spatial.algo.Intersect {
    @Override
    abstract public Point[] intersect(Polygon a, Polygon b);

    @Override
    abstract public boolean doesIntersect(Polygon a, Polygon b);

    @Override
    public Point intersect(LineSegment a, LineSegment b) {
        return lineSegmentIntersect(a, b);
    }

    public static Point lineSegmentIntersect(LineSegment a, LineSegment b) {
        return WGSUtil.intersect(a, b);
    }
}
