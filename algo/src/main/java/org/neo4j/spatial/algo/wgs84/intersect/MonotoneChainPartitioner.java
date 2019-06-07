package org.neo4j.spatial.algo.wgs84.intersect;

import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MonotoneChain;
import org.neo4j.spatial.core.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MonotoneChainPartitioner {
    private List<LineSegment> verticals;

    public MonotoneChainPartitioner() {
        verticals = new ArrayList<>();
    }

    /**
     * Partition the polygon in x-monotone chains.
     *
     * @param polygon
     * @return List of x-monotone chains which together create the input polygon
     */
    public List<MonotoneChain> partition(Polygon.SimplePolygon polygon) {
        List<MonotoneChain> result = new ArrayList<>();
        LineSegment[] lineSegments = polygon.toLineSegments();

        //Add line segments to the current chain until a line segment has a different x-direction compared to the chain
        MonotoneChain chain = new MonotoneChain();
        double lastIncreasing = getXDirection(lineSegments[0]);
        if (lastIncreasing == 0) {
            verticals.add(lineSegments[0]);
        } else {
            chain.add(lineSegments[0]);
        }
        for (int i = 1; i < lineSegments.length; i++) {
            double currentIncreasing = getXDirection(lineSegments[i]);

            if (currentIncreasing == 0.0) {
                verticals.add(lineSegments[i]);
                result.add(chain);
                chain = new MonotoneChain();
                lastIncreasing = currentIncreasing;
            } else if (lastIncreasing == 0.0 ||currentIncreasing == lastIncreasing) {
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
        if (currentIncreasing == 0.0 || lastIncreasing == 0.0) {

        } else if (currentIncreasing == lastIncreasing) {
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

    private double getXDirection(LineSegment lineSegment) {
        double dx = LineSegment.dX(lineSegment);
        return dx == 0 ? 0.0 : dx > 0 ? 1.0 : -1.0;
    }

    public List<LineSegment> getVerticals() {
        return verticals;
    }
}
