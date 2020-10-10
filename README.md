# Spatial Algorithms

This project is intended to be a library of spatial algorithms for spatial data within the Neo4j graph database.
It is based on earlier work in the [Spatial-3D](https://github.com/craigtaverner/spatial-3d) by Craig Taverner, which had a different goal, with a smaller
set of algorithms, but supporting both 2D and 3D.

The new project has dropped support for 3D in favour of some other more ambitious goals:

* Support for a wider range of algorithms, both complex and simple
* Support for alternative models of storage of spatial geometries on the Neo4j database
* Use of polar coordinates and vector algebra for geographic coordinate systems
 
What it still maintains in common with the original project is:

* It is based on the new spatial point support built into Neo4j 3.4
* It does not attempt to replicate the comprehensive spatial capabilities of the [Neo4j Spatial Plugin](https://github.com/neo4j-contrib/spatial),
  which obtains much of its feature set through a reliance on JTS and Geotools
* It is a clean room re-implementation of algorithms without the use of 3rd party libraries like JTS

## Coordinate systems

Each algorithm is written to support two types of coordinate systems:

* Cartesian (Euclidean)
* Geographic (approximated as spherical)

Some algorithm implementations are similar, while others differ quite a lot between cartesian and geographic.
In particular we have implemented the geographic algorithms in polar coordinates through the use of vector algebra.
It is anticipated that this will have better performance than projecting to a plane and then using cartesian algebra.

## Algorithms

Currently there exist implementations of the following algorithms:

* Point in polygon
* Convex hull
* Area
* Distance (between point and geometry and between geometry and geometry)
* Linear referencing
* Intersection (including complex geometries)

## Data models

Neo4j 3.4 and above support only `Point` data types. In order to model complex geometries, like for example a `MultiPolygon` we need a way to map between
the graph of properties, nodes, relationships and sub-graphs to geometries. This project provides a few mappings based on concepts like:

* Sub-graphs for geometries where each point is a single node, and the graph structure is related to the geometry structure
* `Point[]` for coordinate sequences when the geometry is mapped to a single node or a smaller set of nodes than original geometry vertices

## Performance

One advantage of using a database like Neo4j is that it becomes theoretically possible to run algorithms over data sets that are too large for memory.
This is possible if the mapping from graph structure to data suitable for the algorithms is done on the fly during the algorithm.
It does not work if the algorithm needs to stream the same data multiple times, in which case the overhead of repeated conversion would be too much.

# Developing with Spatial Algorithms

## Building

```
git clone git@github.com:neo4j-contrib/spatial-algorithms.git
cd spatial-algorithms
mvn clean install 
```

## Using

The algorithms are designed to be used in two ways:
 * either as a java library by developers of custom spatial procedures
 * or as a set of procedures directly in a running Neo4j server
 
The built-in procedures can be used as examples if you wish to build your own.
Otherwise simply copy the file named something like `dist/target/spatial-algorithms-dist-0.2.3-neo4j-4.1.3.jar`
into the plugins folder of your Neo4j installation.

## Using the library in your Java project with Maven ##

Add the following repositories and dependency to your project's pom.xml:

~~~xml
    <repositories>
        <repository>
            <id>neo4j-contrib-releases</id>
            <url>https://raw.github.com/neo4j-contrib/m2/master/releases</url>
            <releases>
                <enabled>true</enabled>
            </releases>
            <snapshots>
                <enabled>false</enabled>
            </snapshots>
        </repository>
        <repository>
            <id>neo4j-contrib-snapshots</id>
            <url>https://raw.github.com/neo4j-contrib/m2/master/snapshots</url>
            <releases>
                <enabled>false</enabled>
            </releases>
            <snapshots>
                <enabled>true</enabled>
            </snapshots>
        </repository>
    </repositories>
    [...]
    <dependency>
        <groupId>org.neo4j</groupId>
        <artifactId>spatial-algorithms-core</artifactId>
        <version>0.2.3-neo4j-4.1.3</version>
    </dependency>
    <dependency>
        <groupId>org.neo4j</groupId>
        <artifactId>spatial-algorithms-algo</artifactId>
        <version>0.2.3-neo4j-4.1.3</version>
    </dependency>
~~~

The version specified on the version line can be changed to match the version you wish to work with (based on the version of Neo4j itself you are using).
To see which versions are available see the list at [Spatial Algorithms Releases](https://github.com/neo4j-contrib/m2/tree/master/releases/org/neo4j/spatial-algorithms-algo).
