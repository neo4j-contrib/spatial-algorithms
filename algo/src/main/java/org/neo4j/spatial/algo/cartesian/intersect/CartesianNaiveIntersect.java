package org.neo4j.spatial.algo.cartesian.intersect;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MultiPolyline;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polyline;
import org.neo4j.spatial.core.Polygon;

import java.util.ArrayList;
import java.util.List;

public class CartesianNaiveIntersect extends CartesianIntersect {
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
    public boolean doesIntersect(Polygon a, MultiPolyline b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, true).length > 0;
    }

    @Override
    public Point[] intersect(Polygon a, MultiPolyline b) {
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
    public Point[] intersect(MultiPolyline a, MultiPolyline b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, false);
    }

    @Override
    public Point[] intersect(MultiPolyline a, Polyline b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, false);
    }

    @Override
    public Point[] intersect(MultiPolyline a, LineSegment b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = new LineSegment[]{b};

        return compareLineSegments(aLS, bLS, false);
    }

    @Override
    public Point[] intersect(Polyline a, Polyline b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = b.toLineSegments();

        return compareLineSegments(aLS, bLS, false);
    }

    @Override
    public Point[] intersect(Polyline a, LineSegment b) {
        LineSegment[] aLS = a.toLineSegments();
        LineSegment[] bLS = new LineSegment[]{b};

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
        for (LineSegment aL : aLS) {
            for (LineSegment bL : bLS) {
                Point newIntersection = super.intersect(aL, bL);
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
