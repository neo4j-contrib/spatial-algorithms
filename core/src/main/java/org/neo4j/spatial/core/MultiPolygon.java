package org.neo4j.spatial.core;

import org.neo4j.spatial.algo.Within;

import java.util.*;

public class MultiPolygon implements Polygon {
    private List<MultiPolygonNode> children;

    public MultiPolygon() {
        this.children = new ArrayList<>();
    }

    public boolean insertPolygon(SimplePolygon polygon) {
        return insertMultiPolygonNode(new MultiPolygonNode(polygon));
    }

    public boolean insertMultiPolygonNode(MultiPolygonNode other) {
        for (MultiPolygonNode child : children) {
            boolean inserted = child.insertMultiPolygonNode(other);
            if (inserted) {
                return true;
            }
        }

        other.setType(PolygonType.SHELL);
        this.addChild(other);
        return true;
    }

    public List<MultiPolygonNode> getChildren() {
        return this.children;
    }

    void addChild(MultiPolygonNode other) {
        this.children.add(other);
        other.setParent(this);
    }

    void removeChild(MultiPolygonNode other) {
        this.children.remove(other);
    }

    @Override
    public String toWKT() {
        StringJoiner joiner = new StringJoiner(",", "MULTIPOLYGON(", ")");

        for (MultiPolygonNode child : children) {
            for (String s : child.getWKTPointString()) {
                joiner.add(s);
            }
        }

        return joiner.toString();
    }

    @Override
    public Polygon.SimplePolygon[] getShells() {
        List<Polygon.SimplePolygon> polygons = new ArrayList<>();
        for (MultiPolygonNode child : children) {
            polygons.addAll(Arrays.asList(child.getShells()));
        }
        return polygons.toArray(new Polygon.SimplePolygon[0]);
    }

    public Polygon.SimplePolygon[] getHoles() {
        List<Polygon.SimplePolygon> polygons = new ArrayList<>();
        for (MultiPolygonNode child : children) {
            polygons.addAll(Arrays.asList(child.getHoles()));
        }
        return polygons.toArray(new Polygon.SimplePolygon[0]);
    }

    @Override
    public int dimension() {
        return getChildren().get(0).dimension();
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    public static class MultiPolygonNode extends MultiPolygon {
        private Polygon.SimplePolygon polygon;
        private MultiPolygon parent;
        private PolygonType type;

        public MultiPolygonNode(Polygon.SimplePolygon polygon) {
            super();
            this.polygon = polygon;
            this.parent = null;
        }

        @Override
        public boolean insertMultiPolygonNode(MultiPolygonNode other) {
            //If other polygon encompasses this polygon, switch places
            if (Within.within(other.getPolygon(), this.getPolygon().getPoints()[0])) {
                this.parent.removeChild(this);
                this.parent.addChild(other);
                other.addChild(this);
                return true;
            }

            //Non-overlapping polygons should not be inserted in the same subtree
            if (!Within.within(this.getPolygon(), other.getPolygon().getPoints()[0])) {
                return false;
            }

            //Move children to other polygon
            List<MultiPolygonNode> containedInOther = new ArrayList<>();
            for (MultiPolygonNode child : super.children) {
                if (Within.within(child.getPolygon(), other.getPolygon().getPoints()[0])) {
                    child.insertMultiPolygonNode(other);
                    return true;
                } else if (Within.within(other.getPolygon(), child.getPolygon().getPoints()[0])) {
                    containedInOther.add(child);
                }
            }

            if (containedInOther.size() > 0) {
                for (MultiPolygonNode child : containedInOther) {
                    other.addChild(child);
                    this.removeChild(child);
                }
            }

            this.addChild(other);
            return true;
        }

        void setParent(MultiPolygon parent) {
            this.parent = parent;
        }

        public Polygon.SimplePolygon getPolygon() {
            return this.polygon;
        }

        @Override
        void addChild(MultiPolygonNode other) {
            super.addChild(other);
            other.setType(PolygonType.getOther(this.type));
        }

        private void setType(PolygonType type) {
            this.type = type;
        }

        public MultiPolygon getParent() {
            return parent;
        }

        public PolygonType getType() {
            return type;
        }

        @Override
        public Polygon.SimplePolygon[] getShells() {
            List<Polygon.SimplePolygon> polygons = new ArrayList<>();

            if (this.type == PolygonType.SHELL) {
                polygons.add(this.polygon);
            }

            for (MultiPolygonNode child : super.children) {
                polygons.addAll(Arrays.asList(child.getShells()));
            }
            return polygons.toArray(new Polygon.SimplePolygon[0]);
        }

        @Override
        public Polygon.SimplePolygon[] getHoles() {
            List<Polygon.SimplePolygon> polygons = new ArrayList<>();

            if (this.type == PolygonType.HOLE) {
                polygons.add(this.polygon);
            }

            for (MultiPolygonNode child : super.children) {
                polygons.addAll(Arrays.asList(child.getHoles()));
            }
            return polygons.toArray(new Polygon.SimplePolygon[0]);
        }

        @Override
        public int dimension() {
            return this.polygon.dimension();
        }

        List<String> getWKTPointString() {
            List<String> result = new LinkedList<>();

            for (MultiPolygonNode child : this.getChildren()) {
                result.addAll(child.getWKTPointString());
            }

            if (this.type == PolygonType.SHELL) {
                StringJoiner holes = new StringJoiner(",", "", ")");
                String shell = "(" + this.polygon.toWKTPointString();

                if (this.getChildren().size() > 0) {
                    for (MultiPolygonNode child : this.getChildren()) {
                        holes.add(child.getPolygon().toWKTPointString());
                    }

                    result.add(shell + ", " + holes.toString());
                } else {
                    result.add(shell + ")");
                }
            }

            return result;
        }
    }

    enum PolygonType {
        SHELL, HOLE;

        static PolygonType getOther(PolygonType o) {
            return o == PolygonType.SHELL ? PolygonType.HOLE : PolygonType.SHELL;
        }
    }
}
