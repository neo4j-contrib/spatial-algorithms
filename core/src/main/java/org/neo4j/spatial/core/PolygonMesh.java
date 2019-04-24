package org.neo4j.spatial.core;

import java.lang.reflect.Array;
import java.util.*;

import static java.lang.String.format;

public class PolygonMesh implements Polygon {
    private Point[] vertices;
    private Edge[] edges;
    private Face[] faces;

    private PolygonMesh(Point[] vertices, Edge[] edges, Face[] faces) {
        this.vertices = vertices;
        this.edges = edges;
        this.faces = faces;
    }

    @Override
    public int dimension() {
        return 3;
    }

    @Override
    public Point[] getPoints() {
        return vertices;
    }

    public Edge[] getEdges() {
        return edges;
    }

    public Face[] getFaces() {
        return faces;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public SimplePolygon[] getShells() {
        return new SimplePolygon[0];
    }

    @Override
    public SimplePolygon[] getHoles() {
        return new SimplePolygon[0];
    }

    @Override
    public MultiPolygon withShell(Polygon shell) {
        return null;
    }

    @Override
    public MultiPolygon withHole(Polygon hole) {
        return null;
    }

    public static PolygonMeshBuilder start() {
        return new PolygonMeshBuilder();
    }

    public static class Edge {
        int startVertex;
        int endVertex;

        Edge(int startVertex, int endVertex) {
            if (startVertex < endVertex) {
                this.startVertex = startVertex;
                this.endVertex = endVertex;
            } else {
                this.startVertex = endVertex;
                this.endVertex = startVertex;
            }
        }

        @Override
        public int hashCode() {
            return 31 * startVertex + endVertex;
        }

        public boolean equals(Edge other) {
            return this.startVertex == other.startVertex && this.endVertex == other.endVertex;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Edge && equals((Edge) other);
        }

        @Override
        public String toString() {
            return format("Edge[%d,%d]", startVertex, endVertex);
        }
    }

    public static class Face {
        int[] edges;

        private Face(int[] edges) {
            this.edges = edges;
            Arrays.sort(this.edges);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(edges);
        }

        public boolean equals(Face other) {
            return Arrays.equals(this.edges, other.edges);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Face && equals((Face) other);
        }

        @Override
        public String toString() {
            return "Face" + Arrays.toString(edges);
        }
    }

    public static class PolygonMeshBuilder {
        LinkedHashMap<Point, Integer> vertices = new LinkedHashMap<>();
        LinkedHashMap<Edge, Integer> edges = new LinkedHashMap<>();
        LinkedHashMap<Face, Integer> faces = new LinkedHashMap<>();

        public PolygonMeshBuilder addFace(SimplePolygon simple) {
            Point previous = null;
            Integer previousId = 0;
            Point[] points = simple.getPoints();
            int[] faceEdges = new int[points.length - 1];
            int edgeIndex = 0;
            for (Point vertex : simple.getPoints()) {
                Integer vertexId = vertices.get(vertex);
                if (vertexId == null) {
                    vertexId = vertices.size();
                    vertices.put(vertex, vertexId);
                }
                if (previous != null) {
                    Edge edge = new Edge(previousId, vertexId);
                    Integer edgeId = edges.get(edge);
                    if (edgeId == null) {
                        edgeId = edges.size();
                        edges.put(edge, edgeId);
                    }
                    faceEdges[edgeIndex] = edgeId;
                    edgeIndex += 1;
                }
                previous = vertex;
                previousId = vertexId;
            }
            Face face = new Face(faceEdges);
            Integer faceId = faces.get(face);
            if (faceId == null) {
                faceId = faces.size();
                faces.put(face, faceId);
            }
            return this;
        }

        public PolygonMesh build() {
            Point[] v = new MakeArray<>(Point.class).fromMap(vertices);
            Edge[] e = new MakeArray<>(Edge.class).fromMap(edges);
            Face[] f = new MakeArray<>(Face.class).fromMap(faces);
            return new PolygonMesh(v, e, f);
        }

        private static class MakeArray<T> {
            private Class<T> c;

            private MakeArray(Class<T> c) {
                this.c = c;
            }

            private T[] fromMap(HashMap<T, Integer> map) {
                @SuppressWarnings("unchecked")
                T[] array = (T[]) Array.newInstance(c, map.size());
                for (Map.Entry<T, Integer> entry : map.entrySet()) {
                    array[entry.getValue()] = entry.getKey();
                }
                return array;
            }
        }
    }
}
