package org.neo4j.spatial.algo;

import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

public interface Within {
    boolean within(Polygon polygon, Point point);

    boolean within(Polygon.SimplePolygon polygon, Point point);
}
