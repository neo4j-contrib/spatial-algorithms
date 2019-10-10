package org.neo4j.spatial.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class MultiPolyline implements HasCRS {
    private List<Polyline> children;

    public MultiPolyline() {
        this.children = new ArrayList<>();
    }

    public boolean insertPolyline(Polyline polyline) {
        if (!children.isEmpty() && getCRS() != polyline.getCRS()) {
            return false;
        }

        children.add(polyline);
        return true;
    }

    public Polyline[] getChildren() {
        return children.toArray(new Polyline[0]);
    }

    public LineSegment[] toLineSegments() {
        List<LineSegment> lineSegments = new ArrayList<>();

        for (Polyline child : children) {
            Collections.addAll(lineSegments, child.toLineSegments());
        }

        return lineSegments.toArray(new LineSegment[0]);
    }

    @Override
    public CRS getCRS() {
        return children.isEmpty() ? CRS.Cartesian : children.get(0).getCRS();
    }

    public int dimension() {
        return children.isEmpty() ? 0 : children.get(0).dimension();
    }

    public String toWKT() {
        StringJoiner joiner = new StringJoiner(",", "MULTILINESTRING(", ")");

        for (Polyline child : children) {
            joiner.add(child.toWKTPointString());
        }

        return joiner.toString();
    }
}
