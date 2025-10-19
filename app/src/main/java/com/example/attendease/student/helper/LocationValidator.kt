package com.example.attendease.student.helper


import android.location.Location
import com.google.android.gms.maps.model.LatLng

object LocationValidator {

    fun isInsidePolygon(studentLocation: LatLng, polygonPoints: List<LatLng>, toleranceMeters: Float = 5f): Boolean {
        // If directly inside polygon
        if (isPointInsidePolygon(studentLocation, polygonPoints)) return true

        // Otherwise check if within small radius around polygon edges
        for (i in polygonPoints.indices) {
            val j = (i + 1) % polygonPoints.size
            val midPoint = LatLng(
                (polygonPoints[i].latitude + polygonPoints[j].latitude) / 2,
                (polygonPoints[i].longitude + polygonPoints[j].longitude) / 2
            )
            if (isWithinRadius(studentLocation, midPoint, toleranceMeters)) return true
        }
        return false
    }

    private fun isPointInsidePolygon(studentLocation: LatLng, polygonPoints: List<LatLng>): Boolean {
        var intersectCount = 0
        for (i in polygonPoints.indices) {
            val j = (i + 1) % polygonPoints.size
            if (rayCrossesSegment(studentLocation, polygonPoints[i], polygonPoints[j])) {
                intersectCount++
            }
        }
        return (intersectCount % 2 == 1)
    }

    // Ray-casting algorithm helper
    private fun rayCrossesSegment(point: LatLng, a: LatLng, b: LatLng): Boolean {
        val px = point.longitude
        val py = point.latitude
        val ax = a.longitude
        val ay = a.latitude
        val bx = b.longitude
        val by = b.latitude

        if (ay > by) return rayCrossesSegment(point, b, a)
        if (py == ay || py == by) return rayCrossesSegment(
            LatLng(py + 1e-10, px),
            a,
            b
        )

        if (py > by || py < ay || px >= maxOf(ax, bx)) return false

        if (px < minOf(ax, bx)) return true

        val red = if (ax != bx) (by - ay) / (bx - ax) else Double.MAX_VALUE
        val blue = if (ax != px) (py - ay) / (px - ax) else Double.MAX_VALUE
        return blue >= red
    }

    // Optional: Check if within circular radius
    fun isWithinRadius(
        studentLocation: LatLng,
        center: LatLng,
        radiusMeters: Float
    ): Boolean {
        val result = FloatArray(1)
        Location.distanceBetween(
            studentLocation.latitude,
            studentLocation.longitude,
            center.latitude,
            center.longitude,
            result
        )
        return result[0] <= radiusMeters
    }
}
