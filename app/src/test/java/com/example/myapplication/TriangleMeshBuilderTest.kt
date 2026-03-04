package com.example.myapplication

import com.google.android.gms.maps.model.LatLng
import org.junit.Test
import org.junit.Assert.*

import com.example.myapplication.data.TriangleMeshBuilder

class TriangleMeshBuilderTest {

    @Test
    fun noNeighbours() {
        val builder = TriangleMeshBuilder()
        builder.addTriangle(
            LatLng(0.0, 0.0),
            LatLng(1.0, 0.0),
            LatLng(0.0, 1.0)
        )
        val mesh = builder.build()

        assertEquals(1, mesh.nodes.size)
        assertEquals(3, mesh.edges.size)
        assertTrue(mesh.nodes[0].neighbours.isEmpty())
    }

    @Test
    fun sharedEdge() {
        val builder = TriangleMeshBuilder()
        val a = LatLng(0.0, 0.0)
        val b = LatLng(1.0, 0.0)
        val c = LatLng(0.0, 1.0)
        val d = LatLng(1.0, 1.0)

        builder.addTriangle(a, b, c)
        builder.addTriangle(b, d, c)

        val mesh = builder.build()

        val sharedEdge = mesh.edges.first { e ->
            val pts = setOf(e.points.first, e.points.second)
            pts == setOf(b, c)
        }

        assertEquals(2, mesh.nodes.size)
        assertEquals(5, mesh.edges.size)
        assertEquals(1, mesh.nodes[0].neighbours.size)
        assertEquals(1, mesh.nodes[1].neighbours.size)

        assertNotNull(sharedEdge.nodes.first)
        assertNotNull(sharedEdge.nodes.second)
    }

    @Test(expected = Exception::class)
    fun tooManySharedEdgesThrow() {
        val builder = TriangleMeshBuilder()
        val a = LatLng(0.0, 0.0)
        val b = LatLng(1.0, 0.0)
        val c = LatLng(0.0, 1.0)
        val d = LatLng(0.0, 2.0)
        val e = LatLng(0.0, 3.0)

        builder.addTriangle(a, b, c)
        builder.addTriangle(a, b, d)
        builder.addTriangle(a, b, e)
    }
}