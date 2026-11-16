package com.tuneet.tuneetbe.domain.matching.util;

import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanceCalculatorTest {

    // 서울시청
    private static final double LAT = 37.5665;
    private static final double LON = 126.9780;

    @Test
    @DisplayName("두 좌표 사이의 거리를 km로 계산한다 (서울시청-부산시청 약 325km)")
    void kilometers() {
        assertThat(DistanceCalculator.kilometers(LAT, LON, 35.1796, 129.0756)).isCloseTo(325, within(5.0));
        assertThat(DistanceCalculator.kilometers(LAT, LON, LAT, LON)).isZero();
    }

    @Test
    @DisplayName("거리를 1km, 3km, 5km 경계로 구간을 나눈다")
    void range() {
        // 위도 0.009도 ≈ 1km
        assertThat(DistanceCalculator.range(LAT, LON, LAT + 0.008, LON)).isEqualTo(DistanceRange.WITHIN_1KM);
        assertThat(DistanceCalculator.range(LAT, LON, LAT + 0.02, LON)).isEqualTo(DistanceRange.WITHIN_3KM);
        assertThat(DistanceCalculator.range(LAT, LON, LAT + 0.04, LON)).isEqualTo(DistanceRange.WITHIN_5KM);
        assertThat(DistanceCalculator.range(LAT, LON, LAT + 0.05, LON)).isEqualTo(DistanceRange.OVER_5KM);
    }
}
