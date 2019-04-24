package org.neo4j.spatial.core;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PolygonMeshTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldHaveCorrectHashCode() {
        assertThat("Should have same hashcode", new PolygonMesh.Edge(1, 2).hashCode(), equalTo(new PolygonMesh.Edge(2, 1).hashCode()));
    }

    @Test
    public void shouldWorkWithBox() {
        PolygonMesh box = makeBox(new Point(0, 0, 0), new double[]{10, 10, 10});
        debugPolygonMesh("Box", box);
        assertThat("Box should have six faces", box.getFaces().length, equalTo(6));
        assertThat("Box should have eight vertices", box.getPoints().length, equalTo(8));
        assertThat("Box should have twelve edges", box.getEdges().length, equalTo(12));
    }

    @Test
    public void shouldWorkWithIcosohedron() {
        PolygonMesh icosohedron = makeIcosohedron(new Point(0, 0, 0), 10);
        debugPolygonMesh("Icosohedron", icosohedron);
        assertThat("Icosohedron should have eight faces", icosohedron.getFaces().length, equalTo(8));
        assertThat("Icosohedron should have six vertices", icosohedron.getPoints().length, equalTo(6));
        assertThat("Icosohedron should have twelve edges", icosohedron.getEdges().length, equalTo(12));
    }

    private void debugPolygonMesh(String name, PolygonMesh mesh) {
        System.out.println(name + " has " + mesh.getPoints().length + " vertices");
        int index = 0;
        Point[] vertices = mesh.getPoints();
        for (Point p : vertices) {
            System.out.println("\t" + index + ":\t" + p);
            index++;
        }
        System.out.println(name + " has " + mesh.getEdges().length + " edges");
        index = 0;
        for (PolygonMesh.Edge e : mesh.getEdges()) {
            System.out.println(format("\t%d:\t%s\t[%s, %s]", index, e, vertices[e.startVertex], vertices[e.endVertex]));
            index++;
        }
        System.out.println(name + " has " + mesh.getFaces().length + " faces");
        index = 0;
        for (PolygonMesh.Face f : mesh.getFaces()) {
            System.out.println(format("\t%d:\t%s", index, f));
            index++;
        }

    }

    private PolygonMesh makeBox(Point p000, double[] width) {
        Point p100 = p000.withShift(width[0], 0, 0);
        Point p110 = p000.withShift(width[0], width[1], 0);
        Point p010 = p000.withShift(0, width[1], 0);
        Point p001 = p000.withShift(0, 0, width[2]);
        Point p101 = p100.withShift(0, 0, width[2]);
        Point p111 = p110.withShift(0, 0, width[2]);
        Point p011 = p010.withShift(0, 0, width[2]);
        return PolygonMesh.start()
                .addFace(Polygon.simple(p000, p100, p110, p010))    // back face
                .addFace(Polygon.simple(p001, p101, p111, p011))    // front face
                .addFace(Polygon.simple(p000, p001, p011, p010))    // left side
                .addFace(Polygon.simple(p100, p101, p111, p110))    // right side
                .addFace(Polygon.simple(p010, p011, p111, p110))    // top
                .addFace(Polygon.simple(p000, p001, p101, p100))    // bottom
                .build();
    }

    private PolygonMesh makeIcosohedron(Point p000, double radius) {
        Point pp00 = p000.withShift(radius, 0, 0);
        Point p0p0 = p000.withShift(0, radius, 0);
        Point pn00 = p000.withShift(-radius, 0, 0);
        Point p0n0 = p000.withShift(0, -radius, 0);
        Point p00p = p000.withShift(0, 0, radius);
        Point p00n = p000.withShift(0, 0, -radius);
        return PolygonMesh.start()
                .addFace(Polygon.simple(p00p, pp00, p0p0))    // upper +x+y
                .addFace(Polygon.simple(p00p, pn00, p0p0))    // upper -x+y
                .addFace(Polygon.simple(p00p, pn00, p0n0))    // upper -x-y
                .addFace(Polygon.simple(p00p, pp00, p0n0))    // upper +x-y
                .addFace(Polygon.simple(p00n, pp00, p0p0))    // lower +x+y
                .addFace(Polygon.simple(p00n, pn00, p0p0))    // lower -x+y
                .addFace(Polygon.simple(p00n, pn00, p0n0))    // lower -x-y
                .addFace(Polygon.simple(p00n, pp00, p0n0))    // lower +x-y
                .build();
    }
}
