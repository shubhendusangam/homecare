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
}

