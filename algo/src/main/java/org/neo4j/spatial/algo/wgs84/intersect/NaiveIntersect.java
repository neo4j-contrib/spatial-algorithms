package org.neo4j.spatial.algo.wgs84.intersect;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.HashSet;
import java.util.Set;

public class NaiveIntersect implements Intersect {
    @Override
    public boolean doesIntersect(Polygon a, Polygon b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, true).length > 0;
    }

    @Override
    public Point[] intersect(Polygon a, Polygon b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, false);
    }

    private Point[] compareLineSegments(LineSegment[] aLS, LineSegment[] bLS, boolean shortcut) {
        Set<Point> intersections = new HashSet<>();
        for (int i = 0; i < aLS.length; i++) {
            for (int j = 0; j < bLS.length; j++) {
                Point intersection = Intersect.intersect(aLS[i], bLS[j]);
                if (intersection != null) {
                    intersections.add(intersection);
                    if (shortcut) {
                        return intersections.toArray(new Point[0]);
                    }
                }
            }
        }

        return intersections.toArray(new Point[0]);
    }
}
