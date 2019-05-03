package org.neo4j.spatial.core;

import java.util.List;
import java.util.Objects;

public class Vertex {
    public enum Type {
        LEFT_MOST, INTERNAL, RIGHT_MOST, INTERSECTION;
    }

    private Point point;
    private Type type;
    private List<MonotoneChain> monotoneChains;

    public Vertex(Point point) {
        this.point = point;
    }

    public Point getPoint() {
        return point;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public List<MonotoneChain> getMonotoneChains() {
        if (type != Type.INTERSECTION) {
            return null;
        }
        return monotoneChains;
    }

    public void setMonotoneChains(List<MonotoneChain> monotoneChains) {
        if (type != Type.INTERSECTION) {
            return;
        }
        this.monotoneChains = monotoneChains;
    }

    public boolean equals(Vertex other) {
        return this.getPoint().equals(other.getPoint());
    }

    @Override
    public int hashCode() {
        return Objects.hash(point, type, monotoneChains);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Vertex && this.equals((Vertex) other);
    }

    @Override
    public String toString() {
        return "Vertex(" + this.point.getCoordinate()[0] + ", " + this.point.getCoordinate()[1] + ")";
    }
}
