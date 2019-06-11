package org.neo4j.spatial.algo.cartesian.intersect;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MonotoneChain;
import org.neo4j.spatial.core.Polyline;
import org.neo4j.spatial.core.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MonotoneChainPartitioner {
    /**
     * Partition the polygon in x-monotone chains.
     *
     * @param polygon
     * @return List of x-monotone chains which together create the input polygon
     */
    public static List<MonotoneChain> partition(Polygon.SimplePolygon polygon) {
        List<MonotoneChain> result = new ArrayList<>();
        LineSegment[] lineSegments = polygon.toLineSegments();

        //Add line segments to the current chain until a line segment has a different x-direction compared to the chain
        MonotoneChain chain = new MonotoneChain();
        double lastIncreasing = getXDirection(lineSegments[0]);
        chain.add(lineSegments[0]);
        for (int i = 1; i < lineSegments.length; i++) {
            double currentIncreasing = getXDirection(lineSegments[i]);

            if (currentIncreasing == lastIncreasing) {
                chain.add(lineSegments[i]);
            } else {
                result.add(chain);
                chain = new MonotoneChain();
                chain.add(lineSegments[i]);
                lastIncreasing = currentIncreasing;
            }
        }

        //Compare the last chain with the first chain to maybe combine them
        int lastIndex = lineSegments.length - 1;
        double firstIncreasing = getXDirection(lineSegments[0]);
        double currentIncreasing = getXDirection(lineSegments[lastIndex]);
        if (currentIncreasing == lastIncreasing) {
            chain.add(lineSegments[lastIndex]);

            if (currentIncreasing == firstIncreasing) {
                chain.add(result.get(0));
                result.remove(0);
                result.add(chain);
            } else {
                result.add(chain);
            }
        } else {
            result.add(chain);
            chain = new MonotoneChain();
            chain.add(lineSegments[lastIndex]);

            if (currentIncreasing == firstIncreasing) {
                chain.add(result.get(0));
                result.remove(0);
                result.add(chain);
            } else {
                result.add(chain);
            }
        }

        for (MonotoneChain monotoneChain : result) {
            monotoneChain.initialize();
        }

        return result;
    }

    /**
     * Partition the polyline in x-monotone chains.
     *
     * @param polyline
     * @return List of x-monotone chains which together create the input polyline
     */
    public static List<MonotoneChain> partition(Polyline polyline) {
        List<MonotoneChain> result = new ArrayList<>();
        LineSegment[] lineSegments = polyline.toLineSegments();

        //Add line segments to the current chain until a line segment has a different x-direction compared to the chain
        MonotoneChain chain = new MonotoneChain();
        boolean lastIncreasing = LineSegment.dX(lineSegments[0]) > 0;
        chain.add(lineSegments[0]);
        for (int i = 1; i < lineSegments.length; i++) {
            boolean currentIncreasing = LineSegment.dX(lineSegments[i]) > 0;
            if (currentIncreasing == lastIncreasing) {
                chain.add(lineSegments[i]);
            } else {
                result.add(chain);
                chain = new MonotoneChain();
                chain.add(lineSegments[i]);
                lastIncreasing = currentIncreasing;
            }
        }

        result.add(chain);

        for (MonotoneChain monotoneChain : result) {
            monotoneChain.initialize();
        }

        return result;
    }

    private static double getXDirection(LineSegment lineSegment) {
        double dx = LineSegment.dX(lineSegment);
        return dx == 0 ? 0.0 : dx > 0 ? 1.0 : -1.0;
    }
}
