package org.neo4j.spatial.algo.Intersect;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.*;

public class NaiveIntersect implements Intersect {
    @Override
    public Point[] intersect(Polygon.SimplePolygon a, Polygon.SimplePolygon b) {
        Set<Point> intersectionSet = new HashSet<>();
        LineSegment[] aLS = Polygon.SimplePolygon.toLineSegments(a);
        LineSegment[] bLS = Polygon.SimplePolygon.toLineSegments(b);

        for (int i = 0; i < aLS.length; i++) {
            for (int j = 0; j < bLS.length; j++) {
                Point intersection = Intersect.intersect(aLS[i], bLS[j]);
                if (intersection != null) {
                    intersectionSet.add(intersection);
                }
            }
        }

        return intersectionSet.toArray(new Point[0]);
    }
}
