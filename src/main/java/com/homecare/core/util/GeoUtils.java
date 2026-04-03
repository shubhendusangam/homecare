package com.homecare.core.util;

/**
 * Geographic utility methods.
 */
public final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371;
    private static final double KM_PER_DEGREE_LAT = 111.0;

    private GeoUtils() {
        // Utility class — no instantiation
    }

    /**
     * Computes the Haversine (great-circle) distance between two geographic
     * coordinates in kilometres.
     */
    public static double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * Computes a lat/lng bounding box for a given centre point and radius.
     * Used as a cheap pre-filter before the expensive Haversine calculation.
     *
     * @return {@code [minLat, maxLat, minLng, maxLng]}
     */
    public static double[] boundingBox(double lat, double lng, double radiusKm) {
        double latDelta = radiusKm / KM_PER_DEGREE_LAT;
        double lngDelta = radiusKm / (KM_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat)));
        return new double[]{
                lat - latDelta, lat + latDelta,
                lng - lngDelta, lng + lngDelta
        };
    }
}

