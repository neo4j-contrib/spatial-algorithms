package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.algo.CCW;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public class WGS84CCW implements CCW {
    @Override
    public boolean isCCW(Polygon.SimplePolygon polygon) {
        return WGSUtil.courseDelta(polygon.getPoints()) > 270;
    }

    @Override
    public boolean isCCW(Point[] points) {
        return WGSUtil.courseDelta(points) > 270;
    }
}
