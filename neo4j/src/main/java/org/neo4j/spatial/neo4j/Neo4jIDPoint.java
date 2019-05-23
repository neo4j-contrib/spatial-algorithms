package org.neo4j.spatial.neo4j;

import org.neo4j.internal.kernel.api.NodeCursor;
import org.neo4j.internal.kernel.api.PropertyCursor;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.impl.api.TokenAccess;
import org.neo4j.spatial.core.Point;

import java.util.Arrays;

import static java.lang.String.format;

class Neo4jIDPoint implements Point {
    private final long nodeId;
    private final KernelTransaction ktx;

    private static int propertyId = -1;
    private final static String property = "location";

    public Neo4jIDPoint(Long nodeId, KernelTransaction ktx) {
        this.nodeId = nodeId;
        this.ktx = ktx;

        if (propertyId == -1) {
            getPropertyId();
        }
    }

    public boolean equals(Point other) {
        return Arrays.equals(this.getCoordinate(), other.getCoordinate());
    }

    public boolean equals(Object other) {
        return other instanceof Point && this.equals((Point) other);
    }

    @Override
    public double[] getCoordinate() {
        double[] coordinates = new double[2];
        try ( NodeCursor nodeCursor = ktx.cursors().allocateNodeCursor();
              PropertyCursor propertyCursor = ktx.cursors().allocatePropertyCursor() ) {
            ktx.dataRead().singleNode(nodeId, nodeCursor);
            outer:
            while (nodeCursor.next()) {
                nodeCursor.properties(propertyCursor);
                while (propertyCursor.next()) {
                    if (propertyCursor.propertyKey() == propertyId) {
                        org.neo4j.graphdb.spatial.Point point = (org.neo4j.graphdb.spatial.Point) propertyCursor.propertyValue();
                        coordinates = point.getCoordinate().getCoordinate().stream().mapToDouble(d -> d).toArray();
                        break outer;
                    }
                }
            }
        }
        return coordinates;
    }

    private void getPropertyId() {
        String[] properties = TokenAccess.PROPERTY_KEYS.inUse(ktx).stream().toArray(String[]::new);
        for (int i = 0; i < properties.length; i++) {
            if (properties[i].equals(property)) {
                propertyId = i;
                return;
            }
        }
    }

    public String toString() {
        return format("Neo4jPoint%s", Arrays.toString(getCoordinate()));
    }
}