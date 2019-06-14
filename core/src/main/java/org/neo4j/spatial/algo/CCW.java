package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public interface CCW {
    /**
     * @param polygon
     * @return True iff the points of the given polygon are in CartesianCCW order
     */
    boolean isCCW(Polygon.SimplePolygon polygon);


    /**
     * @param points
     * @return True iff the points are in CCW order
     */
    boolean isCCW(Point[] points);
}
