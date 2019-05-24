package org.neo4j.spatial.algo.Intersect;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.*;
import java.util.stream.Stream;

public class NaiveIntersect implements Intersect {
    @Override
    public Point[] intersect(Polygon a, Polygon b) {
        Polygon.SimplePolygon[] aPolygons = Stream.concat(Arrays.stream(a.getShells()), Arrays.stream(a.getHoles()))
                .toArray(Polygon.SimplePolygon[]::new);
        Polygon.SimplePolygon[] bPolygons = Stream.concat(Arrays.stream(b.getShells()), Arrays.stream(b.getHoles()))
                .toArray(Polygon.SimplePolygon[]::new);

        LineSegment[] aLS = Arrays.stream(aPolygons).map(Polygon.SimplePolygon::toLineSegments).flatMap(Stream::of).toArray(LineSegment[]::new);
        LineSegment[] bLS = Arrays.stream(bPolygons).map(Polygon.SimplePolygon::toLineSegments).flatMap(Stream::of).toArray(LineSegment[]::new);

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
