package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.RelationshipType;

public enum Relation implements RelationshipType {
    NEXT, NEXT_IN_POLYGON, FIRST_NODE, MEMBER, NODE, POLYGON_STRUCTURE, POLYGON_START, POLYLINE_STRUCTURE, POLYLINE_START, NEXT_IN_POLYLINE, END_OF_POLYLINE
}
