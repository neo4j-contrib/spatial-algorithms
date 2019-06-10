package org.neo4j.spatial.neo4j;

import org.neo4j.values.storable.CoordinateReferenceSystem;

public class CRSConverter {
    public static org.neo4j.spatial.core.CRS toInMemoryCRS(org.neo4j.graphdb.spatial.CRS neo4jCRS) {
        if (neo4jCRS.equals(CoordinateReferenceSystem.Cartesian)) {
            return org.neo4j.spatial.core.CRS.Cartesian;
        } else if (neo4jCRS.equals(CoordinateReferenceSystem.WGS84)) {
            return org.neo4j.spatial.core.CRS.WGS84;
        } else {
            throw new IllegalArgumentException("Unsupported Coordinate Reference System");
        }
    }
}
