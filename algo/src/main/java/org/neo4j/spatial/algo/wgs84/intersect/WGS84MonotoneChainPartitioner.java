package org.neo4j.spatial.algo.wgs84.intersect;

import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.LineSegment;
import org.neo4j.spatial.core.MonotoneChain;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Polyline;

import java.util.ArrayList;
import java.util.List;

public class WGS84MonotoneChainPartitioner {
    private List<LineSegment> verticals;

    public WGS84MonotoneChainPartitioner() {
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

            if (lineSegments[i].getPoints()[0].equals(Point.point(CRS.WGS84, 14.3337373, 57.0188833)) || lineSegments[i].getPoints()[1].equals(Point.point(CRS.WGS84, 14.3337373, 57.0188833))) {
                System.out.println(i);
            }

            if (currentIncreasing == 0.0) {
                verticals.add(lineSegments[i]);

                if (!chain.getVertices().isEmpty()) {
                    result.add(chain);
                    chain = new MonotoneChain();
                }

                lastIncreasing = currentIncreasing;
            } else if (lastIncreasing == 0.0 ||currentIncreasing == lastIncreasing) {
                chain.add(lineSegments[i]);
                lastIncreasing = currentIncreasing;
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
            //Skip
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

    /**
     * Partition the polyline in x-monotone chains.
     *
     * @param polyline
     * @return List of x-monotone chains which together create the input polyline
     */
    public List<MonotoneChain> partition(Polyline polyline) {
        List<MonotoneChain> result = new ArrayList<>();
        LineSegment[] lineSegments = polyline.toLineSegments();

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

        result.add(chain);

        for (MonotoneChain monotoneChain : result) {
            monotoneChain.initialize();
        }

        return result;
    }

    public static double getXDirection(LineSegment lineSegment) {
        double dx = LineSegment.dX(lineSegment);
        return dx == 0 ? 0.0 : dx > 0 ? 1.0 : -1.0;
    }

    public List<LineSegment> getVerticals() {
        return verticals;
    }
}
