package org.neo4j.spatial.algo.wgs84;

import org.neo4j.spatial.algo.cartesian.CartesianConvexHull;
import org.neo4j.spatial.core.CRS;
import org.neo4j.spatial.core.MultiPolygon;
import org.neo4j.spatial.core.Point;
import org.neo4j.spatial.core.Polygon;
import org.neo4j.spatial.core.Vector;

import java.net.CacheRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;

public class WGS84ConvexHull {
    /**
     * Computes the convex hull of a multipolygon using Graham's scan
     *
     * @param polygon
     * @return A polygon which is the convex hull of the input polygon
     */
    public static Polygon.SimplePolygon convexHull(MultiPolygon polygon) {
        Polygon.SimplePolygon[] convexHulls = new Polygon.SimplePolygon[polygon.getChildren().size()];

        for (int i = 0; i < polygon.getChildren().size(); i++) {
            convexHulls[i] = convexHull(polygon.getChildren().get(i).getPolygon());
        }

        return convexHull(Stream.of(convexHulls).map(Polygon.SimplePolygon::getPoints).flatMap(Stream::of).toArray(Point[]::new));
    }

    /**
     * Computes the convex hull of a polyline polygon using Graham's scan
     *
     * @param polygon
     * @return A polygon which is the convex hull of the input polygon
     */
    public static Polygon.SimplePolygon convexHull(Polygon.SimplePolygon polygon) {
        return convexHull(polygon.getPoints());
    }

    /**
     * Computes the convex hull of a set
     *
     * @param points
     * @return A polygon which is the convex hull of the input points
     */
    public static Polygon.SimplePolygon convexHull(Point[] points) {
        Vector[] vectors = new Vector[points.length];
        for (int i = 0; i < points.length; i++) {
            vectors[i] = new Vector(points[i]);
        }

        Vector pole = getPoleOfHemisphere(vectors);

        if (pole == null) {
            throw new IllegalArgumentException("Points do not lie all on the same hemisphere");
        }

        Vector[] rotatedVectors = rotatePoints(pole, vectors);

        Map<Vector, Integer> mappingToIndex = new HashMap<>();

        for (int i = 0; i < rotatedVectors.length; i++) {
            mappingToIndex.put(rotatedVectors[i], i);
        }

        Arrays.sort(rotatedVectors, (a, b) -> {
            double cosThetaA = Math.acos(a.getCoordinate(2) / a.magnitude());
            double cosThetaB = Math.acos(b.getCoordinate(2) / b.magnitude());

            double tanPhiA = Math.atan2(a.getCoordinate(1), a.getCoordinate(0));
            double tanPhiB = Math.atan2(b.getCoordinate(1), b.getCoordinate(0));

            int compareAzimuth = Double.compare(tanPhiA, tanPhiB);
            int comparePolar = Double.compare(cosThetaA, cosThetaB);

            return compareAzimuth != 0 ? compareAzimuth : comparePolar;
        });

        int start = 0;
        double minTheta = Double.MAX_VALUE;
        double maxTheta = 0;

        for (int i = 0; i < rotatedVectors.length; i++) {
            Vector v = rotatedVectors[i];
            double cosTheta = Math.acos(v.getCoordinate(2) / v.magnitude());

            if (maxTheta < cosTheta) {
                start = i;
                maxTheta = cosTheta;
            }

            if (minTheta > cosTheta) {
                minTheta = cosTheta;
            }
        }

        if (maxTheta - minTheta < 0.1) {
            return CartesianConvexHull.convexHull(points);
        }

        Stack<Vector> stack = new Stack<>();

        for (int i = 0; i < rotatedVectors.length + 1; i++) {
            Vector v = rotatedVectors[(i + start) % rotatedVectors.length];

            //Remove last point from the stack if it does not cross the edge (stack.size()-2), v)
            while (stack.size() > 1 && WGSUtil.intersect(stack.get(stack.size()-2), v, stack.peek(), WGSUtil.NORTH_POLE) == null) {
                stack.pop();
            }
            stack.push(v);
        }

        return Polygon.simple(stack.stream().map(v -> points[mappingToIndex.get(v)]).toArray(Point[]::new));
    }

    /**
     * Rotate the points such that pole is the north pole
     *
     * @param pole The pole of the hemisphere
     * @param vectors The points represented as n-vectors
     * @return Array of rotated points represented as vectors
     */
    public static Vector[] rotatePoints(Vector pole, Vector[] vectors) {
        Vector zAxis = pole.normalize();
        Vector xAxis;
        if (pole.equals(WGSUtil.NORTH_POLE) || pole.equals(WGSUtil.SOUTH_POLE)) {
            xAxis = new Vector(1, 0, 0);
        } else {
            xAxis = pole.cross(WGSUtil.NORTH_POLE).normalize();
        }
        Vector yAxis = zAxis.cross(xAxis).normalize();

        double[][] rotationMatrix = new double[][]{
                xAxis.getCoordinates(),
                yAxis.getCoordinates(),
                zAxis.getCoordinates()
        };


        Vector[] rotatedVectors = new Vector[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            double x = vectors[i].getCoordinate(0);
            double y = vectors[i].getCoordinate(1);
            double z = vectors[i].getCoordinate(2);

            //Ignore z-component
            rotatedVectors[i] = new Vector(
                    x * rotationMatrix[0][0] + y * rotationMatrix[0][1] + z * rotationMatrix[0][2],
                    x * rotationMatrix[1][0] + y * rotationMatrix[1][1] + z * rotationMatrix[1][2],
                    x * rotationMatrix[2][0] + y * rotationMatrix[2][1] + z * rotationMatrix[2][2]
            );
        }
        return rotatedVectors;
    }

    /**
     * Compute the central pole for all the points
     *
     * @param vectors The points represented by n-vectors
     * @return The pole represented as a n-vector. Returns null if not all the points reside on the same hemisphere
     */
    public static Vector getPoleOfHemisphere(Vector[] vectors) {
        Stack<Vector> poles = new Stack<>();

        outer:
        for (int i = 0; i < vectors.length; i++) {
            for (int j = 0; j < vectors.length; j++) {
                if (i == j) {
                    continue;
                }

                Vector[] intersections = new Vector[]{
                        vectors[i].cross(vectors[j]),
                        vectors[j].cross(vectors[i])
                };

                boolean[] flags = new boolean[2];

                for (int l = 0; l < 2; l++) {
                    for (int k = 0; k < vectors.length; k++) {
                        if (k == i || k == j) {
                            continue;
                        }
                        if (intersections[l].dot(vectors[k]) < 0) {
                            //Angle between pole and point is greater than 90 degrees
                            flags[l] = true;
                            break;
                        }
                    }
                }

                for (int l = 0; l < 2; l++) {
                    if (!flags[l]) {
                        poles.add(intersections[l]);
                    }
                }
            }
        }

        if (poles.empty()) {
            return null;
        }

        Vector center = poles.pop();
        while (!poles.empty()) {
            center = center.add(poles.pop());
        }

        return center.normalize();
    }
}
