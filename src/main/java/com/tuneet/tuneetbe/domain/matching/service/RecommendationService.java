package com.tuneet.tuneetbe.domain.matching.service;

import com.tuneet.tuneetbe.domain.matching.dto.request.RecommendationRequest;
import com.tuneet.tuneetbe.domain.matching.dto.response.RecommendationListResponse;
import com.tuneet.tuneetbe.domain.matching.dto.response.RecommendationListResponse.Reason;
import com.tuneet.tuneetbe.domain.matching.dto.response.RecommendationListResponse.ReasonType;
import com.tuneet.tuneetbe.domain.matching.dto.response.RecommendationListResponse.RecommendedUser;
import com.tuneet.tuneetbe.domain.matching.entity.UserRecommendation;
import com.tuneet.tuneetbe.domain.matching.repository.UserRecommendationRepository;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserBlock;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserBlockRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/**
 * 근처 사용자 중 취향이 비슷한 사용자를 추천한다.
 * 새로 받을 때만 다시 뽑고 결과를 저장해서, 홈·매칭 탭·추천 목록이 같은 사용자를 보여 준다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    static final int CANDIDATE_LIMIT = 200;
    static final int TOP_POOL_SIZE = 20;
    static final int RECOMMEND_COUNT = 5;
    static final Duration LOCATION_MAX_AGE = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserRecommendationRepository userRecommendationRepository;
    private final TasteMatchService tasteMatchService;
    private final Clock clock;

    // 추천 새로 받기 (현재 위치 저장)
    @Transactional
    public RecommendationListResponse refresh(Long userId, RecommendationRequest request) {

        // 같은 사용자의 중복 요청이 순서대로 처리되도록 사용자 행을 잠그고 조회
        User me = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        validateMatchingAvailable(me);

        LocalDateTime now = LocalDateTime.now(clock);
        me.updateLocation(request.getLatitude(), request.getLongitude(), now);

        // 가까운 후보 200명 (7일 넘게 위치를 갱신하지 않은 사용자 제외) → 매칭률 계산
        List<User> candidates = userRepository.findRecommendationCandidates(
                userId, me.getLatitude(), me.getLongitude(), now.minus(LOCATION_MAX_AGE), CANDIDATE_LIMIT);
        Map<Long, TasteMatch> matches = tasteMatchService.match(me, candidates);

        // 직전 추천은 가능하면 빼고 뽑기
        List<UserRecommendation> previous = userRecommendationRepository.findAllByUserIdOrderByDisplayOrderAsc(userId);
        Set<Long> previousIds = new HashSet<>();
        previous.forEach(recommendation -> previousIds.add(recommendation.getRecommendedUser().getId()));
        List<User> picked = pick(candidates, matches, previousIds, ThreadLocalRandom.current());

        // 추천 결과 교체
        userRecommendationRepository.deleteAllInBatch(previous);
        List<UserRecommendation> recommendations = new ArrayList<>();
        for (int i = 0; i < picked.size(); i++) {
            recommendations.add(UserRecommendation.builder()
                    .user(me)
                    .recommendedUser(picked.get(i))
                    .displayOrder(i)
                    .createdAt(now)
                    .build());
        }
        userRecommendationRepository.saveAll(recommendations);

        log.info("[RecommendationService] 추천 사용자 갱신 성공: userId = {}, candidates = {}, picked = {}",
                userId, candidates.size(), picked.size());
        return toResponse(picked, matches, now);
    }

    // 마지막으로 받은 추천 조회 (그사이 탈퇴, 위치 비공개, 차단한 사용자는 제외)
    @Transactional(readOnly = true)
    public RecommendationListResponse getRecommendations(Long userId) {

        User me = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        validateMatchingAvailable(me);

        List<UserRecommendation> saved = userRecommendationRepository.findAllByUserIdOrderByDisplayOrderAsc(userId);
        if (saved.isEmpty()) {
            return toResponse(List.of(), Map.of(), null);
        }

        Set<Long> blockedIds = new HashSet<>();
        for (UserBlock block : userBlockRepository.findAllByBlockerIdOrBlockedId(userId, userId)) {
            blockedIds.add(block.getBlocker().getId());
            blockedIds.add(block.getBlocked().getId());
        }
        List<User> users = saved.stream()
                .map(UserRecommendation::getRecommendedUser)
                .filter(user -> user.canUseMatching() && !blockedIds.contains(user.getId()))
                .toList();

        return toResponse(users, tasteMatchService.match(me, users), saved.getFirst().getCreatedAt());
    }

    // 위치 동의와 위치 노출을 모두 켜야 추천을 받을 수 있음 (프론트는 이 오류로 위치 유도 화면을 보여 줌)
    private void validateMatchingAvailable(User me) {
        if (me.isDeleted()) {
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }
        if (me.getLocationAgreedAt() == null) {
            log.warn("[RecommendationService] 위치정보 수집 및 이용 동의가 필요합니다.");
            throw new CustomException(UserErrorCode.LOCATION_AGREEMENT_REQUIRED);
        }
        if (!me.canUseMatching()) {
            log.warn("[RecommendationService] 위치정보 노출을 꺼서 추천을 받을 수 없습니다.");
            throw new CustomException(UserErrorCode.LOCATION_NOT_VISIBLE);
        }
    }

    // 매칭률 상위 20명 중 매칭률에 비례한 확률로 5명 (직전 추천은 부족할 때만 다시 포함), 매칭률 높은 순으로 정렬
    static List<User> pick(List<User> candidates, Map<Long, TasteMatch> matches, Set<Long> previousIds, RandomGenerator random) {
        Comparator<User> byPercent = Comparator.comparingInt((User user) -> percentOf(matches, user)).reversed();
        List<User> top = candidates.stream().sorted(byPercent).limit(TOP_POOL_SIZE).toList();

        List<User> fresh = new ArrayList<>();
        List<User> repeated = new ArrayList<>();
        top.forEach(user -> (previousIds.contains(user.getId()) ? repeated : fresh).add(user));

        List<User> picked = new ArrayList<>();
        pickWeighted(fresh, picked, matches, random);
        pickWeighted(repeated, picked, matches, random);
        picked.sort(byPercent);
        return picked;
    }

    private static void pickWeighted(List<User> pool, List<User> picked, Map<Long, TasteMatch> matches, RandomGenerator random) {
        List<User> remaining = new ArrayList<>(pool);
        while (picked.size() < RECOMMEND_COUNT && !remaining.isEmpty()) {
            // 매칭률 0인 사용자도 뽑힐 수 있도록 1을 더함
            double total = remaining.stream().mapToDouble(user -> percentOf(matches, user) + 1).sum();
            double target = random.nextDouble() * total;
            int index = 0;
            double sum = percentOf(matches, remaining.get(0)) + 1;
            while (sum <= target && index < remaining.size() - 1) {
                index++;
                sum += percentOf(matches, remaining.get(index)) + 1;
            }
            picked.add(remaining.remove(index));
        }
    }

    private static int percentOf(Map<Long, TasteMatch> matches, User user) {
        TasteMatch match = matches.get(user.getId());
        return match == null || match.matchPercent() == null ? 0 : match.matchPercent();
    }

    private RecommendationListResponse toResponse(List<User> users, Map<Long, TasteMatch> matches, LocalDateTime recommendedAt) {
        List<RecommendedUser> cards = new ArrayList<>();
        for (User user : users) {
            TasteMatch match = matches.get(user.getId());
            cards.add(RecommendedUser.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .profilePicture(user.getProfilePicture())
                    .matchPercent(match == null ? null : match.matchPercent())
                    .distanceRange(match == null ? null : match.distanceRange())
                    .reason(match == null ? null : reason(match))
                    .letterReceivable(user.getLetterReceivable())
                    .build());
        }
        return RecommendationListResponse.builder()
                .recommendedAt(recommendedAt)
                .users(cards)
                .build();
    }

    // 카드 문구: 곡 → 아티스트 → 장르 순으로 하나
    private static Reason reason(TasteMatch match) {
        if (!match.commonMusics().isEmpty()) {
            Music music = match.commonMusics().getFirst();
            return new Reason(ReasonType.MUSIC, music.getTitle(), music.getMusicPicture());
        }
        if (!match.commonArtists().isEmpty()) {
            Artist artist = match.commonArtists().getFirst();
            return new Reason(ReasonType.ARTIST, artist.getName(), artist.getArtistImage());
        }
        if (!match.commonGenres().isEmpty()) {
            return new Reason(ReasonType.GENRE, match.commonGenres().getFirst().getGenreName().getDisplayName(), null);
        }
        return null;
    }
}
