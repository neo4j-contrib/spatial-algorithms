package org.neo4j.spatial.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class MonotoneChain {
    private static long nextId = 0L;

    private List<Vertex> vertices;
    private Vertex frontVertex;
    private long id;

    public MonotoneChain() {
        this.vertices = new ArrayList<>();
        this.id = nextId++;
    }

    public static void resetId() {
        MonotoneChain.nextId = 0L;
    }

    public void add(LineSegment segment) {
        this.vertices.add(new Vertex(segment.getPoints()[0]));
        this.vertices.add(new Vertex(segment.getPoints()[1]));
    }

    public void add(MonotoneChain chain) {
        vertices.addAll(chain.getVertices());
    }

    public void initialize() {
        this.vertices.sort(Comparator.comparingDouble(a -> a.getPoint().getCoordinate()[0]));
        this.vertices = this.vertices.stream().distinct().collect(Collectors.toList());
        this.frontVertex = vertices.get(0);
        this.frontVertex.setType(Vertex.Type.LEFT_MOST);

        for (int i = 1; i <=vertices.size() - 1; i++) {
            vertices.get(i).setType(Vertex.Type.INTERNAL);
        }

        vertices.get(vertices.size() - 1).setType(Vertex.Type.RIGHT_MOST);
    }

    public String toWKT() {
        StringJoiner joiner = new StringJoiner(",", "LINESTRING(", ")");
        for (Vertex vertex: vertices) {
            joiner.add(vertex.getPoint().getCoordinate()[0] + " " + vertex.getPoint().getCoordinate()[1]);
        }
        return joiner.toString();
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<LineSegment> getLineSegments() {
        List<LineSegment> segments = new ArrayList<>();
        for (int i = 0; i < vertices.size() - 1; i++) {
            Point a = vertices.get(i).getPoint();
            Point b = vertices.get(i+1).getPoint();

            segments.add(LineSegment.lineSegment(a, b));
        }
        return segments;
    }

    public double getMinX() {
        double x1 = vertices.get(0).getPoint().getCoordinate()[0];
        double x2 = vertices.get(vertices.size() - 1).getPoint().getCoordinate()[0];
        return x1 < x2 ? x1 : x2;
    }

    public double getMaxX() {
        double x1 = vertices.get(0).getPoint().getCoordinate()[0];
        double x2 = vertices.get(vertices.size() - 1).getPoint().getCoordinate()[0];
        return x1 > x2 ? x1 : x2;
    }

    public Vertex getFrontVertex() {
        return this.frontVertex;
    }

    public Vertex getPrevious(Vertex vertex) {
        int index = vertices.indexOf(vertex);

        if (index <= 0) {
            return null;
        }

        return vertices.get(index - 1);
    }

    /**
     * Advance the monotone chain to the next point
     */
    public void advance() {
        int index = this.vertices.indexOf(this.frontVertex);
        if (index == this.vertices.size() - 1) {
            this.frontVertex = null;
            return;
        }
        this.frontVertex = vertices.get(index + 1);
    }

    public void insertFrontVertex(Vertex vertex) {
        int index = this.vertices.indexOf(this.frontVertex);
        if (this.frontVertex.getType() == Vertex.Type.RIGHT_MOST && vertex.getPoint().equals(this.frontVertex.getPoint())) {
            this.frontVertex = vertex;
            this.vertices.set(index, vertex);
        }
        if (index > 0) {
            this.vertices.add(index, vertex);
        } else {
            this.vertices.add(0, vertex);
        }
        this.frontVertex = vertex;
    }

    private Point[] getInterval(double x) {
        int i = 0;
        while (vertices.get(i).getPoint().getCoordinate()[0] <= x) {
            i++;
        }
        if (i >= vertices.size()) {
            i = vertices.size() - 1;
        }
        return new Point[]{vertices.get(i-1).getPoint(), vertices.get(i).getPoint()};
    }

    public double getY(double x) {
        Point[] interval = getInterval(x);
        Line line = new Line(interval[0], interval[1]);
        return line.getY(x);
    }

    public double getAngle(double x) {
        Point[] interval = getInterval(x);
        Line line = new Line(interval[0], interval[1]);
        return line.getA();
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "MC" + id + "(" + this.frontVertex + ")";
    }

    public boolean equals(MonotoneChain other) {
        return this.id == other.getId();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MonotoneChain && this.equals((MonotoneChain) other);
    }
}
