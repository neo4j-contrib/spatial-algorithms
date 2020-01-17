package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import java.util.List;

public abstract class GraphBuilder {
    protected Node main;
    protected List<List<Node>> polylines;
    protected Transaction tx;

    public GraphBuilder(Transaction tx, Node main, List<List<Node>> polylines) {
        this.tx = tx;
        this.main = main;
        this.polylines = polylines;
    }

    abstract void build();
}
