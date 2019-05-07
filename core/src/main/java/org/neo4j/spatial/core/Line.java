package org.neo4j.spatial.core;

public class Line {
    private double a;
    private double b;
    private boolean vertical = false;

    public Line(LineSegment ls) {
        this(ls.getPoints()[0], ls.getPoints()[1]);
    }

    public Line(Point p, Point q) {
        double dx = p.getCoordinate()[0] - q.getCoordinate()[0];
        double dy = p.getCoordinate()[1] - q.getCoordinate()[1];

        if (dx == 0) {
            vertical = true;
            return;
        }

        a = dy/dx;
        b = p.getCoordinate()[1] - (a * p.getCoordinate()[0]);
    }

    public double getA() {
        return a;
    }

    public double getB() {
        return b;
    }

    public boolean isVertical() {
        return vertical;
    }

    public double getY(double x) {
        return a*x + b;
    }

    public int getSign(Point p) {
        return (int) Math.signum(p.getCoordinate()[1] - a * p.getCoordinate()[0] - b);
    }
}
