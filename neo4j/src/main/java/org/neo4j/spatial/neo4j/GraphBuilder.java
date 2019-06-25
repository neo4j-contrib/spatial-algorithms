package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;

import java.util.List;

public abstract class GraphBuilder {
    protected Node main;
    protected List<List<Node>> polylines;
    protected GraphDatabaseService db;


    public GraphBuilder(Node main, List<List<Node>> polylines) {
        this.main = main;
        this.polylines = polylines;

        this.db = main.getGraphDatabase();
    }

    abstract void build();
}
