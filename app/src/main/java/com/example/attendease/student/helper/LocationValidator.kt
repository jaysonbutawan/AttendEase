package com.example.attendease.student.helper


import android.location.Location
import com.google.android.gms.maps.model.LatLng

object LocationValidator {

    fun isInsidePolygon(studentLocation: LatLng, polygonPoints: List<LatLng>, toleranceMeters: Float = 5f): Boolean {
        if (isPointInsidePolygon(studentLocation, polygonPoints)) return true
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

    fun getDistanceFromPolygon(
        studentLocation: LatLng,
        polygonPoints: List<LatLng>
    ): Float {
        if (polygonPoints.isEmpty()) return Float.MAX_VALUE

        var minDistance = Float.MAX_VALUE
        val result = FloatArray(1)

        for (i in polygonPoints.indices) {
            val start = polygonPoints[i]
            val end = polygonPoints[(i + 1) % polygonPoints.size]

            val distance = distanceToSegment(studentLocation, start, end)
            if (distance < minDistance) minDistance = distance
        }

        return minDistance
    }

    private fun distanceToSegment(
        p: LatLng,
        v: LatLng,
        w: LatLng
    ): Float {
        val result = FloatArray(1)

        val l2 = distanceBetween(v, w)
        if (l2 == 0f) {
            Location.distanceBetween(
                p.latitude, p.longitude,
                v.latitude, v.longitude,
                result
            )
            return result[0]
        }

        val t = ((p.longitude - v.longitude) * (w.longitude - v.longitude) +
                (p.latitude - v.latitude) * (w.latitude - v.latitude)) / l2

        val clampedT = t.coerceIn(0.0, 1.0)
        val projection = LatLng(
            v.latitude + clampedT * (w.latitude - v.latitude),
            v.longitude + clampedT * (w.longitude - v.longitude)
        )

        Location.distanceBetween(
            p.latitude, p.longitude,
            projection.latitude, projection.longitude,
            result
        )

        return result[0]
    }

    private fun distanceBetween(a: LatLng, b: LatLng): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            a.latitude, a.longitude,
            b.latitude, b.longitude,
            result
        )
        return result[0]
    }

}
