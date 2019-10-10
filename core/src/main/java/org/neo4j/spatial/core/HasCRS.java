package org.neo4j.spatial.core;

public interface HasCRS {
    CRS getCRS();
    int dimension();
}
