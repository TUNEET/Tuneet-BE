package com.tuneet.tuneetbe.domain.matching.service;

import com.tuneet.tuneetbe.domain.matching.dto.request.RecommendationRequest;
import com.tuneet.tuneetbe.domain.matching.dto.response.RecommendationListResponse;
import com.tuneet.tuneetbe.domain.matching.dto.response.RecommendationListResponse.ReasonType;
import com.tuneet.tuneetbe.domain.matching.dto.response.RecommendationListResponse.RecommendedUser;
import com.tuneet.tuneetbe.domain.matching.entity.UserRecommendation;
import com.tuneet.tuneetbe.domain.matching.repository.UserRecommendationRepository;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserBlock;
import com.tuneet.tuneetbe.domain.user.enums.BlockType;
import com.tuneet.tuneetbe.domain.user.enums.DistanceRange;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserBlockRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final Long USER_ID = 1L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 15, 0);

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private UserRecommendationRepository userRecommendationRepository;

    @Mock
    private TasteMatchService tasteMatchService;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        service = new RecommendationService(userRepository, userBlockRepository, userRecommendationRepository,
                tasteMatchService, clock);
    }

    // 위치 동의, 위치 노출을 켠 사용자
    private User user(Long id) {
        User user = User.builder().email(id + "@email.com").password("pw")
                .termsAgreedAt(NOW).privacyAgreedAt(NOW).locationAgreedAt(NOW).build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "nickname", "user" + id);
        return user;
    }

    private TasteMatch match(int percent) {
        return new TasteMatch(percent, DistanceRange.WITHIN_1KM, List.of(), List.of(), List.of());
    }

    private UserRecommendation saved(User me, User recommended, int order) {
        return UserRecommendation.builder().user(me).recommendedUser(recommended).displayOrder(order)
                .createdAt(NOW.minusDays(1)).build();
    }

    @Test
    @DisplayName("위치 동의를 하지 않았으면 LOCATION_AGREEMENT_REQUIRED, 위치 노출을 껐으면 LOCATION_NOT_VISIBLE")
    void locationRequired() {
        User notAgreed = user(USER_ID);
        ReflectionTestUtils.setField(notAgreed, "locationAgreedAt", null);
        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(notAgreed));

        assertThatThrownBy(() -> service.refresh(USER_ID, new RecommendationRequest(37.5, 127.0)))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.LOCATION_AGREEMENT_REQUIRED));

        User hidden = user(USER_ID);
        hidden.updateSettings(false, null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(hidden));

        assertThatThrownBy(() -> service.getRecommendations(USER_ID))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(UserErrorCode.LOCATION_NOT_VISIBLE));
        verify(userRepository, never()).findRecommendationCandidates(anyLong(), anyDouble(), anyDouble(), any(), anyInt());
    }

    @Test
    @DisplayName("새로 받으면 위치를 약 1km 단위로 반올림해 저장하고 7일 안에 위치를 갱신한 후보 중에서 뽑아 매칭률 순으로 저장한다")
    @SuppressWarnings("unchecked")
    void refresh() {
        User me = user(USER_ID);
        User low = user(2L);
        User high = user(3L);
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "title", "Hype Boy");
        ReflectionTestUtils.setField(music, "musicPicture", "cover.png");
        Artist artist = BeanUtils.instantiateClass(Artist.class);
        ReflectionTestUtils.setField(artist, "name", "NewJeans");

        given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(me));
        given(userRepository.findRecommendationCandidates(USER_ID, 37.5, 127.0, NOW.minusDays(7), 200))
                .willReturn(List.of(low, high));
        given(tasteMatchService.match(me, List.of(low, high))).willReturn(Map.of(
                2L, new TasteMatch(20, DistanceRange.WITHIN_1KM, List.of(), List.of(artist), List.of()),
                3L, new TasteMatch(80, DistanceRange.WITHIN_3KM, List.of(music), List.of(artist), List.of())));

        RecommendationListResponse response = service.refresh(USER_ID, new RecommendationRequest(37.50432, 126.99871));

        assertThat(me.getLatitude()).isEqualTo(37.5);
        assertThat(me.getLongitude()).isEqualTo(127.0);
        assertThat(me.getLocationUpdatedAt()).isEqualTo(NOW);
        assertThat(response.getRecommendedAt()).isEqualTo(NOW);
        assertThat(response.getUsers()).extracting(RecommendedUser::getUserId).containsExactly(3L, 2L);
        RecommendedUser first = response.getUsers().getFirst();
        assertThat(first.getMatchPercent()).isEqualTo(80);
        assertThat(first.getDistanceRange()).isEqualTo(DistanceRange.WITHIN_3KM);
        assertThat(first.getReason().getType()).isEqualTo(ReasonType.MUSIC);
        assertThat(first.getReason().getName()).isEqualTo("Hype Boy");
        assertThat(first.getReason().getImageUrl()).isEqualTo("cover.png");
        assertThat(response.getUsers().get(1).getReason().getType()).isEqualTo(ReasonType.ARTIST);

        ArgumentCaptor<List<UserRecommendation>> saved = ArgumentCaptor.forClass(List.class);
        verify(userRecommendationRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(r -> r.getRecommendedUser().getId(), UserRecommendation::getDisplayOrder)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(3L, 0), org.assertj.core.groups.Tuple.tuple(2L, 1));
    }

    @Test
    @DisplayName("직전 추천은 다른 후보가 충분하면 빼고 뽑는다")
    void excludesPrevious() {
        List<User> candidates = new ArrayList<>();
        Map<Long, TasteMatch> matches = new HashMap<>();
        for (long id = 2; id <= 8; id++) {
            candidates.add(user(id));
            matches.put(id, match((int) id * 10));
        }

        List<User> picked = RecommendationService.pick(candidates, matches, Set.of(8L, 7L), RandomGenerator.getDefault());

        assertThat(picked).extracting(User::getId).containsExactly(6L, 5L, 4L, 3L, 2L);
    }

    @Test
    @DisplayName("다른 후보가 부족하면 직전 추천도 다시 넣는다")
    void refillsWithPrevious() {
        List<User> candidates = List.of(user(2L), user(3L), user(4L));
        Map<Long, TasteMatch> matches = Map.of(2L, match(10), 3L, match(50), 4L, match(90));

        List<User> picked = RecommendationService.pick(candidates, matches, Set.of(4L), RandomGenerator.getDefault());

        assertThat(picked).extracting(User::getId).containsExactly(4L, 3L, 2L);
    }

    @Test
    @DisplayName("매칭률 상위 20명 안에서만 뽑는다")
    void onlyTopTwenty() {
        List<User> candidates = new ArrayList<>();
        Map<Long, TasteMatch> matches = new HashMap<>();
        for (long id = 1; id <= 30; id++) {
            candidates.add(user(id + 1));
            matches.put(id + 1, match((int) id));
        }
        // 항상 가장 큰 값을 내서 남은 후보 중 마지막(매칭률이 가장 낮은 쪽)을 고름
        RandomGenerator last = () -> -1L;

        List<User> picked = RecommendationService.pick(candidates, matches, Set.of(), last);

        // 상위 20명은 매칭률 11~30, 그중 가장 낮은 5명
        assertThat(picked).extracting(user -> matches.get(user.getId()).matchPercent())
                .containsExactly(15, 14, 13, 12, 11);
    }

    @Test
    @DisplayName("조회는 다시 뽑지 않고, 그사이 차단했거나 위치를 숨긴 사용자는 뺀다")
    void getSaved() {
        User me = user(USER_ID);
        User keep = user(2L);
        User blocked = user(3L);
        User hidden = user(4L);
        hidden.updateSettings(false, null);
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
        given(userRecommendationRepository.findAllByUserIdOrderByDisplayOrderAsc(USER_ID)).willReturn(List.of(
                saved(me, keep, 0), saved(me, blocked, 1), saved(me, hidden, 2)));
        given(userBlockRepository.findAllByBlockerIdOrBlockedId(USER_ID, USER_ID)).willReturn(List.of(
                UserBlock.builder().blocker(blocked).blocked(me).type(BlockType.BLOCK).build()));
        given(tasteMatchService.match(me, List.of(keep))).willReturn(Map.of(2L, match(40)));

        RecommendationListResponse response = service.getRecommendations(USER_ID);

        assertThat(response.getRecommendedAt()).isEqualTo(NOW.minusDays(1));
        assertThat(response.getUsers()).singleElement().satisfies(card -> {
            assertThat(card.getUserId()).isEqualTo(2L);
            assertThat(card.getMatchPercent()).isEqualTo(40);
            assertThat(card.getReason()).isNull();
        });
        verify(userRecommendationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("추천을 받은 적이 없으면 빈 목록이다")
    void neverRecommended() {
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user(USER_ID)));

        RecommendationListResponse response = service.getRecommendations(USER_ID);

        assertThat(response.getRecommendedAt()).isNull();
        assertThat(response.getUsers()).isEmpty();
    }
}
