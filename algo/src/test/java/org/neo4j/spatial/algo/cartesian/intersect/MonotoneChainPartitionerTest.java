package org.neo4j.spatial.algo.cartesian.intersect;

import org.junit.Test;
import org.neo4j.spatial.core.CRS;
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
                Point.point(CRS.Cartesian, -18,-12),
                Point.point(CRS.Cartesian, -3,-3),
                Point.point(CRS.Cartesian, 10,-15),
                Point.point(CRS.Cartesian, 18,3),
                Point.point(CRS.Cartesian, -2,14),
                Point.point(CRS.Cartesian, -11,8),
                Point.point(CRS.Cartesian, -0,1),
                Point.point(CRS.Cartesian, -17,2),
                Point.point(CRS.Cartesian, -21,12),
                Point.point(CRS.Cartesian, -25,4),
                Point.point(CRS.Cartesian, -29,-3),
                Point.point(CRS.Cartesian, -22,-9),
                Point.point(CRS.Cartesian, -17,-6),
                Point.point(CRS.Cartesian, -27,-14),
                Point.point(CRS.Cartesian, -18,-12)
        );
    }
}