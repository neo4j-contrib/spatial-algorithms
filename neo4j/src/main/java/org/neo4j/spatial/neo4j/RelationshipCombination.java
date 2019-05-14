package org.neo4j.spatial.neo4j;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.RelationshipType;

public class RelationshipCombination {
    private RelationshipType type;
    private Direction direction;

    public RelationshipCombination(RelationshipType type) {
        this.type = type;
        this.direction = Direction.BOTH;
    }

    public RelationshipCombination(RelationshipType type, Direction direction) {
        this.type = type;
        this.direction = direction;
    }

    public RelationshipType getType() {
        return type;
    }

    public Direction getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return type.toString() + "; " + direction.toString();
    }
}
