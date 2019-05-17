package org.neo4j.spatial.algo.Intersect;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.*;
import java.util.stream.Stream;

public class NaiveIntersect implements Intersect {
    //TODO remove this function
    public Point[] intersect(Polygon.SimplePolygon a, Polygon.SimplePolygon b) {
        LineSegment[] aLS = Polygon.SimplePolygon.toLineSegments(a);
        LineSegment[] bLS = Polygon.SimplePolygon.toLineSegments(b);

        return compareLineSegments(aLS, bLS);
    }

    @Override
    public Point[] intersect(Polygon a, Polygon b) {
        LineSegment[] aLS = Arrays.stream(a.getShells()).map(Polygon.SimplePolygon::toLineSegments).flatMap(Stream::of).toArray(LineSegment[]::new);
        LineSegment[] bLS = Arrays.stream(b.getShells()).map(Polygon.SimplePolygon::toLineSegments).flatMap(Stream::of).toArray(LineSegment[]::new);

        return compareLineSegments(aLS, bLS);
    }

    private Point[] compareLineSegments(LineSegment[] aLS, LineSegment[] bLS) {
        Set<Point> intersections = new HashSet<>();
        for (int i = 0; i < aLS.length; i++) {
            for (int j = 0; j < bLS.length; j++) {
                Point intersection = Intersect.intersect(aLS[i], bLS[j]);
                if (intersection != null) {
                    intersections.add(intersection);
                }
            }
        }

        return intersections.toArray(new Point[0]);
    }
}
