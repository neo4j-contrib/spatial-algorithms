package org.neo4j.spatial.algo.wgs84.intersect;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polyline;
import org.neo4j.spatial.core.Polygon;

import java.util.ArrayList;
import java.util.List;

public class NaiveIntersect extends Intersect {
    @Override
    public boolean doesIntersect(Polygon a, Polygon b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, true).length > 0;
    }
    @Override
    public Point[] intersect(Polygon a, Polyline b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, false);
    }

    @Override
    public boolean doesIntersect(Polygon polygon, Polyline polyline) {
        LineSegment[] aLS = polygon.toLineSegments();
        LineSegment[] bLS = polyline.toLineSegments();

        return compareLineSegments(aLS, bLS, true).length > 0;
    }

    @Override
    public Point[] intersect(Polyline a, Polyline b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, false);
    }

    @Override
    public Point[] intersect(Polyline polyline, LineSegment lineSegment) {
        LineSegment[] aLS = new LineSegment[]{lineSegment};
        LineSegment[] bLS = polyline.toLineSegments();

        return compareLineSegments(aLS, bLS, false);
    }

    @Override
    public Point[] intersect(Polygon a, Polygon b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, false);
    }

    private Point[] compareLineSegments(LineSegment[] aLS, LineSegment[] bLS, boolean shortcut) {
        List<Point> intersections = new ArrayList<>();
        for (int i = 0; i < aLS.length; i++) {
            for (int j = 0; j < bLS.length; j++) {
                Point newIntersection = super.intersect(aLS[i], bLS[j]);
                if (newIntersection != null) {
                    addPoint(intersections, newIntersection);
                    if (shortcut) {
                        return intersections.toArray(new Point[0]);
                    }
                }
            }
        }

        return intersections.toArray(new Point[0]);
    }

    private void addPoint(List<Point> intersections, Point newIntersection) {
        boolean flag = false;
        for (Point intersection : intersections) {
            if (AlgoUtil.equal(intersection, newIntersection)) {
                flag = true;
                break;
            }
        }

        if (!flag) {
            intersections.add(newIntersection);
        }
    }
}
