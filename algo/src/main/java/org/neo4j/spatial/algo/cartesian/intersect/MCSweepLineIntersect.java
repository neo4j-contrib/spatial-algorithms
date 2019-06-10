package org.neo4j.spatial.algo.cartesian.intersect;

import org.neo4j.spatial.algo.AlgoUtil;
import org.neo4j.spatial.core.*;

import java.util.*;
import java.util.stream.Stream;

public class MCSweepLineIntersect extends Intersect {
    private List<MonotoneChain> activeChainList;
    private List<MonotoneChain> sweepingChainList;
    private List<Point> outputList;

    private double sweepAngle;

    //This variable is used to determine the origin of the monotone chains
    private long splitId;

    public MCSweepLineIntersect() {
        initialize();
    }

    private void initialize() {
        MonotoneChain.resetId();
        this.activeChainList = new ArrayList<>();
        this.sweepingChainList = new ArrayList<>();
        this.outputList = new ArrayList<>();
    }


    @Override
    public boolean doesIntersect(Polygon a, Polygon b) {
        return intersect(a, b, true).length > 0;
    }

    @Override
    public Point[] intersect(Polygon a, Polygon b) {
        return intersect(a, b, false);
    }

    /**
     * A monotone chain sweep line algorithm based on:
     * Park S.C., Shin H., Choi B.K. (2001) A sweep line algorithm for polygonal chain intersection and its applications.
     * In: Kimura F. (eds) Geometric Modelling. GEO 1998. IFIP — The International Federation for Information Processing, vol 75. Springer, Boston, MA
     *
     * @param a
     * @param b
     * @return An array of points at which the two input polygons intersect
     */
    public Point[] intersect(Polygon a, Polygon b, boolean shortcut) {
        initialize();

        Polygon.SimplePolygon[] aPolygons = Stream.concat(Arrays.stream(a.getShells()), Arrays.stream(a.getHoles()))
                .toArray(Polygon.SimplePolygon[]::new);
        Polygon.SimplePolygon[] bPolygons = Stream.concat(Arrays.stream(b.getShells()), Arrays.stream(b.getHoles()))
                .toArray(Polygon.SimplePolygon[]::new);

        for (int i = 0; i < aPolygons.length; i++) {
            aPolygons[i] = filterCollinear(aPolygons[i]);
        }

        for (int i = 0; i < bPolygons.length; i++) {
            bPolygons[i] = filterCollinear(bPolygons[i]);
        }

        computeSweepDirection(aPolygons, bPolygons);

        List<MonotoneChain> inputList = new ArrayList<>();
        for (int i = 0; i < aPolygons.length; i++) {
            Polygon.SimplePolygon rotatedPolygon = createRotatedPolygon(aPolygons[i]);
            List<MonotoneChain> partitioned = MonotoneChainPartitioner.partition(rotatedPolygon);
            inputList.addAll(partitioned);
        }

        for (int i = 0; i < bPolygons.length; i++) {
            Polygon.SimplePolygon rotatedPolygon = createRotatedPolygon(bPolygons[i]);
            List<MonotoneChain> partitioned = MonotoneChainPartitioner.partition(rotatedPolygon);
            inputList.addAll(partitioned);

            if (i == 0) {
                splitId = partitioned.get(0).getId();
            }
        }

        for (MonotoneChain monotoneChain : inputList) {
            insertMonotoneChainInACL(monotoneChain);
        }

        Vertex v;
        MonotoneChain MCa;
        while (!this.activeChainList.isEmpty()) {
            MCa = this.activeChainList.get(0);
            v = MCa.getFrontVertex();
            MCa.advance();
            insertMonotoneChainInACL(MCa);

            switch (v.getType()) {
                case LEFT_MOST:
                    insertInSCL(MCa, v.getPoint().getCoordinate()[0]);
                    findIntersection(MCa, getPrevious(this.sweepingChainList, MCa));
                    findIntersection(MCa, getNext(this.sweepingChainList, MCa));
                    break;
                case INTERNAL:
                    findIntersection(MCa, getPrevious(this.sweepingChainList, MCa));
                    findIntersection(MCa, getNext(this.sweepingChainList, MCa));
                    break;
                case RIGHT_MOST:
                    MonotoneChain MCp = getPrevious(this.sweepingChainList, MCa);
                    MonotoneChain MCn = getNext(this.sweepingChainList, MCa);
                    this.sweepingChainList.remove(MCa);
                    this.activeChainList.remove(MCa);
                    findIntersection(MCp, MCn);
                    break;
                case INTERSECTION:
                    MonotoneChain finalMCa = MCa;
                    MonotoneChain MCb = v.getMonotoneChains().stream().filter(c -> !c.equals(finalMCa)).findFirst().get();
                    MCb.advance();
                    insertMonotoneChainInACL(MCb);
                    swapAccordingToSCL(new ArrayList<>(Arrays.asList(MCa, MCb)), v.getPoint().getCoordinate()[0]);
                    MonotoneChain previous = getPrevious(sweepingChainList, MCb);
                    if (previous != null && MCa.equals(previous)) {
                        findIntersection(MCa, getPrevious(sweepingChainList, MCa));
                        findIntersection(MCb, getNext(sweepingChainList, MCb));
                    } else {
                        findIntersection(MCb, getPrevious(sweepingChainList, MCb));
                        findIntersection(MCa, getNext(sweepingChainList, MCa));
                    }
                    addToOutput(v.getPoint());
                    break;
            }
            if (shortcut && outputList.size() > 0) {
                return outputList.toArray(new Point[0]);
            }
        }

        return outputList.toArray(new Point[0]);
    }

    private void addToOutput(Point rotatedPoint) {
        Point point = Point.point(CRS.Cartesian, AlgoUtil.rotate(rotatedPoint, -this.sweepAngle));
        for (Point inList : outputList) {
            if (AlgoUtil.equal(point, inList)) {
                return;
            }
        }
        this.outputList.add(point);
    }

    /**
     * @param polygon The input polygon
     * @return A new polygon which is the input polygon, but rotated to the sweep angle
     */
    private Polygon.SimplePolygon createRotatedPolygon(Polygon.SimplePolygon polygon) {
        Point[] rotatedPoints = Arrays.stream(polygon.getPoints()).map(p -> new RotatedPoint(p, this.sweepAngle)).toArray(RotatedPoint[]::new);
        return Polygon.simple(rotatedPoints);
    }

    /**
     * Compute an angle for which no vertical line segments exist
     * @param aShells
     * @param bShells
     */
    private void computeSweepDirection(Polygon.SimplePolygon[] aShells, Polygon.SimplePolygon[] bShells) {
        Set<Double> angleSet = new HashSet<>();

        angleSet.addAll(computeAngles(aShells));
        angleSet.addAll(computeAngles(bShells));

        List<Double> angles = new ArrayList<>(angleSet);
        Collections.sort(angles);

        double angle = -1;
        for (int i = 0; i < angles.size() - 1; i++) {
            double currentAngle = (angles.get(i+1) + angles.get(i)) / 2d;

            boolean flag = false;
            double vertical = angle + (Math.PI/2);

            if (vertical < 0) {
                vertical -= Math.PI;
            }

            for (Double toCheck : angles) {
                if (AlgoUtil.equal(toCheck, vertical)) {
                    flag = true;
                }
            }

            if (!flag) {
                angle = currentAngle;
                break;
            }
        }

        if (angle == -1) {
            throw new IllegalArgumentException("Vertical line segments found for every possible sweep direction");
        }

        this.sweepAngle = angle;
    }

    /**
     * @param polygons All shells and holes of a polygon
     * @return Compute the angles of the line segments
     */
    private Set<Double> computeAngles(Polygon.SimplePolygon[] polygons) {
        Set<Double> angles = new HashSet<>();

        for (Polygon.SimplePolygon polygon : polygons) {
            for (LineSegment segment : polygon.toLineSegments()) {
                Point p = segment.getPoints()[0];
                Point q = segment.getPoints()[1];

                double dx = q.getCoordinate()[0] - p.getCoordinate()[0];
                double dy = q.getCoordinate()[1] - p.getCoordinate()[1];

                double angle = Math.atan2(dy, dx);

                if (angle < -0) {
                    angle += Math.PI;
                }

                angles.add(angle);
            }
        }

        return angles;
    }

    /**
     * Removes all successive collinear points of the given polygon
     *
     * @param polygon
     * @return New polygon without successive collinear points
     */
    private Polygon.SimplePolygon filterCollinear(Polygon.SimplePolygon polygon) {
        List<Point> filteredPoints = new ArrayList<>(Arrays.asList(polygon.getPoints()));

        List<Integer> toDelete = new LinkedList<>();
        for (int i = 1; i < filteredPoints.size() - 1; i++) {
            Point a = filteredPoints.get(i - 1);
            Point b = filteredPoints.get(i);
            Point c = filteredPoints.get(i + 1);

            if (AlgoUtil.ccw(a, b, c) == 0) {
                toDelete.add(i - toDelete.size());
            }
        }
        for (Integer index : toDelete) {
            filteredPoints.remove((int) index);
        }

        return Polygon.simple(filteredPoints.toArray(new Point[0]));
    }

    /**
     * Insert the monotone chain into the active chain list based on x-values of the front vertices.
     *
     * @param chain The monotone chain to be inserted
     */
    private void insertMonotoneChainInACL(MonotoneChain chain) {
        this.activeChainList.remove(chain);

        if (chain.getFrontVertex() == null || this.activeChainList.isEmpty()) {
            this.activeChainList.add(chain);
            return;
        }

        int i = 0;
        Vertex current = chain.getFrontVertex();
        Vertex other = this.activeChainList.get(i).getFrontVertex();
        while (other.getPoint().getCoordinate()[0] < current.getPoint().getCoordinate()[0]) {
            i++;
            if (i >= this.activeChainList.size()) {
                this.activeChainList.add(chain);
                return;
            }
            other = this.activeChainList.get(i).getFrontVertex();
        }

        this.activeChainList.add(i, chain);
    }

    /**
     * Find the intersection between two monotone chains (if it exists) and create a new INTERSECTION vertex
     * if the intersection point is not a shared point of the two chains.
     *
     * @param a
     * @param b
     */
    private void findIntersection(MonotoneChain a, MonotoneChain b) {
        if (a == null || b == null) {
            return;
        }

        LineSegment aSegment = LineSegment.lineSegment(a.getFrontVertex().getPoint(), a.getPrevious(a.getFrontVertex()).getPoint());
        LineSegment bSegment = LineSegment.lineSegment(b.getFrontVertex().getPoint(), b.getPrevious(b.getFrontVertex()).getPoint());

        Point sharedPoint = LineSegment.sharedPoint(aSegment, bSegment);
        if (sharedPoint != null) {
            //Check if point isn't already in the output and the two chains are from different polygons by comparing signs
            if (!outputList.contains(LineSegment.sharedPoint(aSegment, bSegment)) &&
                    ((a.getId() - splitId ^ b.getId() - splitId) < 0)) {
                addToOutput(sharedPoint);
            }
            return;
        }

        Point intersect = super.intersect(aSegment, bSegment);

        if (intersect == null) {
            return;
        }

        Vertex intersectVertex = new Vertex(intersect);

        intersectVertex.setType(Vertex.Type.INTERSECTION);
        intersectVertex.setMonotoneChains(new ArrayList<>(Arrays.asList(a, b)));

        a.insertFrontVertex(intersectVertex);
        insertMonotoneChainInACL(a);
        b.insertFrontVertex(intersectVertex);
        insertMonotoneChainInACL(b);
    }

    /**
     * For all the elements in the input list, which currently intersect the sweep line,
     * sort them in the sweeping chain list based on their angle at the sweep line.
     *
     * @param toSort The list of chains which will be re-sorted in the sweeping chain list
     * @param x      The x-coordinate of the sweep line
     */
    private void swapAccordingToSCL(List<MonotoneChain> toSort, double x) {
        toSort.sort((a, b) -> {
            double angleA = a.getAngle(x);
            double angleB = b.getAngle(x);

            return Double.compare(angleA, angleB);
        });

        int index = this.sweepingChainList.size();
        for (MonotoneChain chain : toSort) {
            int i = this.sweepingChainList.indexOf(chain);
            if (i < index) {
                index = i;
            }
        }
        this.sweepingChainList.removeAll(toSort);
        for (int i = 0; i < toSort.size(); i++) {
            this.sweepingChainList.add(index + i, toSort.get(i));
        }

    }

    /**
     * Insert the monotone chain into the sweeping chain list based on its y-value at the sweep line.
     * If this y-value coincides with another chain in the list, sort them by their angle.
     *
     * @param chain The chain to be inserted.
     * @param x     The x-coordinate of the sweep line
     */
    private void insertInSCL(MonotoneChain chain, double x) {
        this.sweepingChainList.add(chain);
        this.sweepingChainList.sort((a, b) -> {
            double aY = a.getY(x);
            double bY = b.getY(x);

            if (AlgoUtil.equal(aY, bY)) {
                double angleA = a.getAngle(x);
                double angleB = b.getAngle(x);

                return Double.compare(angleA, angleB);
            }

            return Double.compare(aY, bY);
        });
    }

    /**
     * Get the next monotone chain from the list based on the position of the input chain.
     *
     * @param list  The list of monotone chains to search
     * @param chain The input chain
     * @return The next chain in the list based on the input chain, and null if no next chain exists.
     */
    private MonotoneChain getNext(List<MonotoneChain> list, MonotoneChain chain) {
        int index = list.indexOf(chain);

        if (index == -1 || index == list.size() - 1) {
            return null;
        }

        return list.get(index + 1);
    }

    /**
     * Get the previous monotone chain from the list based on the position of the input chain.
     *
     * @param list  The list of monotone chains to search
     * @param chain The input chain
     * @return The previous chain in the list based on the input chain, and null if no previous chain exists.
     */
    private MonotoneChain getPrevious(List<MonotoneChain> list, MonotoneChain chain) {
        int index = list.indexOf(chain);

        if (index <= 0) {
            return null;
        }

        return list.get(index - 1);
    }
}
