package com.example.myapplication.data

import com.google.android.gms.maps.model.LatLng

class TriangleMesh(val nodes: List<TriangleNode>, val edges: List<TriangleEdge>) {
}

class TriangleNode(val points: Triple<LatLng, LatLng, LatLng>,
                   var neighbours: List<TriangleNode> = listOf()) {
    fun addNeighbour(node: TriangleNode): TriangleNode{
        neighbours = neighbours + listOf(node)
        return this
    }
}

class TriangleEdge(val nodes: Pair<TriangleNode?, TriangleNode?>, val points: Pair<LatLng, LatLng>) {

}

class TriangleMeshBuilder {
    private var edges = mutableListOf<TriangleEdge>()
    private var nodes = mutableListOf<TriangleNode>()
    private val edgeIndexMap = mutableMapOf<Pair<LatLng, LatLng>, Int>()

    private fun getOrMakeEdge(a: LatLng, b: LatLng): Int {
        val sorted = listOf(a, b).sortedWith(compareBy({it.latitude}, {it.longitude}))
        val key = Pair(sorted[0], sorted[1])
        return edgeIndexMap.getOrPut(key) {
            val idx = edges.size
            edges.add(TriangleEdge(Pair(null, null), key))
            idx
        }
    }

    private fun neighbourOnEdge(edgeIdx: Int, node: TriangleNode): TriangleNode? {
        val edge = edges[edgeIdx]
        val (n1, n2) = edge.nodes
        var otherTriangle: TriangleNode? = null

        when {
            n1 == null -> {
                otherTriangle = n2
            }
            n2 == null -> {
                otherTriangle = n1
            }
            else -> throw Exception(
                "Edge $edgeIdx already has 2 nodes"
            )
        }

        return otherTriangle
    }

    private fun pointToNode(edgeIdx: Int, node: TriangleNode) {
        val edge = edges[edgeIdx]
        val (n1, n2) = edge.nodes

        edges[edgeIdx] = when {
            n1 == null -> {
                TriangleEdge(Pair(node, n2), edge.points)
            }
            n2 == null -> {
                TriangleEdge(Pair(n1, node), edge.points)
            }
            else -> throw Exception(
                "Edge $edgeIdx already has 2 nodes"
            )
        }
    }

    fun addTriangle(a: LatLng, b: LatLng, c: LatLng) {
        val sortedPoints = listOf(a, b, c).sortedWith(compareBy({it.latitude}, {it.longitude}))
        var outTriangle = TriangleNode(Triple(sortedPoints[0], sortedPoints[1], sortedPoints[2]))

        val e0 = getOrMakeEdge(a, b)
        val e1 = getOrMakeEdge(b, c)
        val e2 = getOrMakeEdge(c, a)

        val neighbours = mutableListOf<TriangleNode>()

        neighbourOnEdge(e0, outTriangle)?.let { neighbours.add(it) }
        neighbourOnEdge(e1, outTriangle)?.let { neighbours.add(it) }
        neighbourOnEdge(e2, outTriangle)?.let { neighbours.add(it) }

        outTriangle = neighbours.fold(outTriangle, TriangleNode::addNeighbour)
        neighbours.forEach { it.addNeighbour(outTriangle) }

        pointToNode(e0, outTriangle)
        pointToNode(e1, outTriangle)
        pointToNode(e2, outTriangle)

        nodes.add(outTriangle)
    }

    fun build(): TriangleMesh {
        return TriangleMesh(nodes.toList(), edges.toList())
    }
}