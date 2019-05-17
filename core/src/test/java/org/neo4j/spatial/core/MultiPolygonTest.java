//package org.neo4j.spatial.core;
//
//import org.junit.Test;
//
//import static org.hamcrest.CoreMatchers.equalTo;
//import static org.junit.Assert.*;
//
//public class MultiPolygonTest {
//
//    @Test
//    public void shouldMakeMultiPolygon() {
//        MultiPolygon multiPolygon = makeMultiPolygon();
//
//        assertThat(multiPolygon.dimension(), equalTo(2));
//        assertThat("Should have 3 children", multiPolygon.getChildren().size(), equalTo(3));
//        assertThat("Child 1 should have 2 children", multiPolygon.getChildren().get(0).getChildren().size(), equalTo(2));
//        assertThat("Child 1-1 should have 3 children", multiPolygon.getChildren().get(0).getChildren().get(0).getChildren().size(), equalTo(3));
//        assertThat("Child 1-1-1 should have no children", multiPolygon.getChildren().get(0).getChildren().get(0).getChildren().get(0).getChildren().size(), equalTo(0));
//        assertThat("Child 1-1-2 should have no children", multiPolygon.getChildren().get(0).getChildren().get(0).getChildren().get(1).getChildren().size(), equalTo(0));
//        assertThat("Child 1-2 should have no children", multiPolygon.getChildren().get(0).getChildren().get(1).getChildren().size(), equalTo(0));
//        assertThat("Child 2 child should have 1 child", multiPolygon.getChildren().get(1).getChildren().size(), equalTo(1));
//        assertThat("Child 3 child should have no children", multiPolygon.getChildren().get(2).getChildren().size(), equalTo(0));
//
//        assertThat("Should have 5 shells", multiPolygon.getShells().length, equalTo(5));
//        assertThat("Should have 5 holes", multiPolygon.getHoles().length, equalTo(3));
//    }
//
//    private MultiPolygon makeMultiPolygon() {
//        Polygon.SimplePolygon[] polygons = new Polygon.SimplePolygon[5];
//
//        Point[][] input = new Point[][]{
//                {
//                        Point.point(0,2),
//                        Point.point(1,4),
//                        Point.point(0,7),
//                        Point.point(0,9),
//                        Point.point(1,12),
//                        Point.point(4,13),
//                        Point.point(6,7),
//                        Point.point(6,4),
//                        Point.point(5,3),
//                        Point.point(5,0),
//                        Point.point(2,0)
//                },
//                {
//                        Point.point(0.5,7),
//                        Point.point(0.5,9),
//                        Point.point(3,12),
//                        Point.point(5,7),
//                        Point.point(5,4),
//                        Point.point(2,4)
//                },
//                {
//                        Point.point(4.5, 4.5),
//                        Point.point(2, 5),
//                        Point.point(2, 6),
//                        Point.point(4.5, 6)
//                },
//                {
//                        Point.point(1.5,7.5),
//                        Point.point(2,10),
//                        Point.point(4,7)
//                },
//                {
//                        Point.point(2,0.6),
//                        Point.point(1,2),
//                        Point.point(4,3),
//                        Point.point(4.5,0.5)
//                }
//
//        };
//
//        for (int i = 0; i < polygons.length; i++) {
//            polygons[i] = Polygon.simple(input[i]);
//        }
//
//        MultiPolygon multiPolygon = new MultiPolygon();
//        for (Polygon.SimplePolygon polygon : polygons) {
//            multiPolygon.insertPolygon(polygon);
//        }
//
//        return multiPolygon;
//    }
//}