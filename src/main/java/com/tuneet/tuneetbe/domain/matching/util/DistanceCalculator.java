package com.tuneet.tuneetbe.domain.matching.util;

import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;

public final class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {
    }

    // 두 좌표 사이의 거리 (km, Haversine)
    public static double kilometers(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    // 정확한 거리 대신 구간으로 표시 (위치 추정 방지)
    public static DistanceRange range(double lat1, double lon1, double lat2, double lon2) {
        double km = kilometers(lat1, lon1, lat2, lon2);
        if (km <= 1) {
            return DistanceRange.WITHIN_1KM;
        }
        if (km <= 3) {
            return DistanceRange.WITHIN_3KM;
        }
        if (km <= 5) {
            return DistanceRange.WITHIN_5KM;
        }
        return DistanceRange.OVER_5KM;
    }
}
