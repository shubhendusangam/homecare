package com.homecare.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GeoUtils — Haversine distance computation")
class GeoUtilsTest {

    @Test
    @DisplayName("Same point → distance is 0")
    void samePoint_distanceZero() {
        double d = GeoUtils.haversineDistance(28.6139, 77.2090, 28.6139, 77.2090);
        assertEquals(0.0, d, 0.001);
    }

    @Test
    @DisplayName("Delhi to Mumbai ≈ 1,150 km")
    void delhiToMumbai() {
        double d = GeoUtils.haversineDistance(28.6139, 77.2090, 19.0760, 72.8777);
        assertEquals(1150.0, d, 50.0); // ±50 km tolerance
    }

    @Test
    @DisplayName("Short distance — nearby points ≈ few km")
    void nearbyPoints() {
        // ~1.1 km apart in central Delhi
        double d = GeoUtils.haversineDistance(28.6139, 77.2090, 28.6239, 77.2090);
        assertTrue(d > 0.5 && d < 2.0, "Expected ~1.1 km, got: " + d);
    }

    @Test
    @DisplayName("Antipodal points — max distance ≈ 20,000 km")
    void antipodalPoints() {
        double d = GeoUtils.haversineDistance(0, 0, 0, 180);
        assertEquals(20015.0, d, 100.0);
    }

    @Test
    @DisplayName("Distance is symmetric")
    void distanceIsSymmetric() {
        double d1 = GeoUtils.haversineDistance(28.6139, 77.2090, 19.0760, 72.8777);
        double d2 = GeoUtils.haversineDistance(19.0760, 72.8777, 28.6139, 77.2090);
        assertEquals(d1, d2, 0.001);
    }

    @Test
    @DisplayName("Negative coordinates are handled")
    void negativeCoordinates() {
        double d = GeoUtils.haversineDistance(-33.8688, 151.2093, -37.8136, 144.9631);
        assertTrue(d > 600 && d < 800, "Sydney to Melbourne ≈ 714 km, got: " + d);
    }

    @Test
    @DisplayName("boundingBox — 10km radius returns correct lat/lng deltas")
    void boundingBox_10kmRadius() {
        // Delhi: 28.6139°N, 77.2090°E, radius 10 km
        double[] bbox = GeoUtils.boundingBox(28.6139, 77.2090, 10);
        // 10 km ≈ 0.09009° latitude
        double expectedLatDelta = 10.0 / 111.0;
        assertEquals(28.6139 - expectedLatDelta, bbox[0], 0.001, "minLat");
        assertEquals(28.6139 + expectedLatDelta, bbox[1], 0.001, "maxLat");
        // lngDelta is wider because cos(28.6°) ≈ 0.878
        assertTrue(bbox[2] < 77.2090, "minLng should be less than centre");
        assertTrue(bbox[3] > 77.2090, "maxLng should be greater than centre");
        // Verify symmetry
        double lngDelta = bbox[3] - 77.2090;
        assertEquals(77.2090 - bbox[2], lngDelta, 0.0001, "lng deltas should be symmetric");
        // lngDelta should be > latDelta (at non-equatorial latitudes)
        assertTrue(lngDelta > expectedLatDelta, "lng delta should be wider than lat delta at 28°N");
    }

    @Test
    @DisplayName("boundingBox — at equator, lat and lng deltas are equal")
    void boundingBox_equator() {
        double[] bbox = GeoUtils.boundingBox(0.0, 0.0, 10);
        double latDelta = bbox[1] - 0.0;
        double lngDelta = bbox[3] - 0.0;
        assertEquals(latDelta, lngDelta, 0.001, "At equator, lat and lng deltas should be equal");
    }
}

