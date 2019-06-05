package org.neo4j.spatial.algo.cartesian.intersect;

import org.junit.Test;
import org.neo4j.spatial.core.MonotoneChain;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;

import java.util.List;

public class MonotoneChainPartitionerTest {
    @Test
    public void shouldPartitionPolygon() {
        Polygon.SimplePolygon testPolygon = makeTestPolygon();
        List<MonotoneChain> actual = MonotoneChainPartitioner.partition(testPolygon);
    }

    private Polygon.SimplePolygon makeTestPolygon() {
        return Polygon.simple(
                Point.point(-18,-12),
                Point.point(-3,-3),
                Point.point(10,-15),
                Point.point(18,3),
                Point.point(-2,14),
                Point.point(-11,8),
                Point.point(-0,1),
                Point.point(-17,2),
                Point.point(-21,12),
                Point.point(-25,4),
                Point.point(-29,-3),
                Point.point(-22,-9),
                Point.point(-17,-6),
                Point.point(-27,-14),
                Point.point(-18,-12)
        );
    }
}