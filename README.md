# Spatial Algorithms

This project is intended to be a library of spatial algorithms for spatial data within the Neo4j graph database.
It is based on earlier work in the [Spatial-3D](https://github.com/craigtaverner/spatial-3d) by Craig Taverner, which had a different goal, with a smaller
set of algorithms, but supporting both 2D and 3D.

The new project has dropped support for 3D in favour of some other more ambitious goals:

* Support for a wider range of algorithms, both complex and simple
* Support for alternative models of storage of spatial geometries on the Neo4j database
 
What it still maintains in common with the original project is:

* It is based on the new spatial point support built into Neo4j 3.4
* It does not attempt to replicate the comprehensive spatial capabilities of the [Neo4j Spatial](https://github.com/neo4j-contrib/spatial),
  which gains a comprehensive set of features through reliance of JTS and Geotools
* It is a re-implementation of algorithms without the use of 3rd party libraries like JTS

## Coordinate systems

Each algorithm is written to support two types of coordinate systems:

* Cartesian (Euclidean)
* Geographic (approximated as spherical)

Some algorithm implementations are similar, while others differ quite a lot between cartesian and geographic.

## Algorithms

Currently there exist implementations of the following algorithms:

* Point in polygon
* Convex hull
* Area
* Distance (between point and geometry and between geometry and geometry)
* Linear referencing
* ?

## Data models

Neo4j 3.4 and above support only `Point` data types. In order to model complex geometries, like for example a `MultiPolygon` we need a way to map between
the graph of properties, nodes, relationships and sub-graphs to geometries. This project provides a few mappings based on concepts like:

* Sub-graphs for geometries where each point is a single node, and the graph structure is related to the geometry structure
* `Point[]` for coordinate sequences when the geometry is mapped to a single node or a smaller set of nodes than original geometry vertices

## Performance

One advantage of using a database like Neo4j is that it becomes theoretically possible to run algorithms over data sets that are too large for memory.
This is possible if the 

# Developing with Spatial Algorithms

## Building

```
git clone git@github.com:neo4j-contrib/spatial-algorithms.git
cd spatial-algorithms
mvn clean install 
```

## Using

...