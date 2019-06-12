package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.core.Polygon;

public class CCW implements org.neo4j.spatial.algo.CCW {
    @Override
    public boolean isCCW(Polygon.SimplePolygon polygon) {
        return WGSUtil.courseDelta(polygon) > 270;
    }
}
