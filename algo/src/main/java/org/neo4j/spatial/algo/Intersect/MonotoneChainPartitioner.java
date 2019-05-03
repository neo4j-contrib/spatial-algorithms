package org.neo4j.spatial.algo.Intersect;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MonotoneChain;
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
        LineSegment[] lineSegments = Polygon.SimplePolygon.toLineSegments(polygon);

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

        //Compare the last chain with the first chain to maybe combine them
        int lastIndex = lineSegments.length - 1;
        boolean firstIncreasing = LineSegment.dX(lineSegments[0]) > 0;
        boolean currentIncreasing = LineSegment.dX(lineSegments[lastIndex]) > 0;
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
}
