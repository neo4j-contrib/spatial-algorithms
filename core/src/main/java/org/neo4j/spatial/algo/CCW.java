package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Polygon;

public interface CCW {
    /**
     * @param polygon
     * @return True iff the points of the given polygon are in CCW order
     */
    boolean isCCW(Polygon.SimplePolygon polygon);
}
