package com.tuneet.tuneetbe.domain.user.service;

import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageRepository;
import com.tuneet.tuneetbe.domain.feed.repository.FeedRepository;
import com.tuneet.tuneetbe.domain.music.dto.response.ArtistResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.GenreResponse;
import com.tuneet.tuneetbe.domain.music.dto.response.MusicResponse;
import com.tuneet.tuneetbe.domain.music.entity.Genre;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.repository.GenreRepository;
import com.tuneet.tuneetbe.domain.notification.repository.DeviceTokenRepository;
import com.tuneet.tuneetbe.domain.notification.repository.NotificationRepository;
import com.tuneet.tuneetbe.domain.room.service.RoomRemover;
import com.tuneet.tuneetbe.domain.user.dto.request.GenreReplaceRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.ProfileAddRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.SettingsUpdateRequest;
import com.tuneet.tuneetbe.domain.user.dto.request.SignUpRequest;
import com.tuneet.tuneetbe.domain.user.dto.response.BlockedUserListResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.EmailAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.GenreReplaceResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.MyProfileResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.NicknameAvailabilityResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.ProfileAddResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.SettingsResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.SignUpResponse;
import com.tuneet.tuneetbe.domain.user.dto.response.UserProfileResponse;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserArtist;
import com.tuneet.tuneetbe.domain.user.entity.UserBlock;
import com.tuneet.tuneetbe.domain.user.entity.UserGenre;
import com.tuneet.tuneetbe.domain.user.enums.BlockType;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserArtistRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserBlockRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserGenreRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserPlatformRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserGenreRepository userGenreRepository;
    private final GenreRepository genreRepository;
    private final UserPlatformRepository userPlatformRepository;
    private final UserArtistRepository userArtistRepository;
    private final UserMusicRepository userMusicRepository;
    private final UserBlockRepository userBlockRepository;
    private final FeedRepository feedRepository;
    private final FeedMessageRepository feedMessageRepository;
    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final RoomRemover roomRemover;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {

        // 비밀번호가 일치하는지 조회
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            log.warn("[UserService] 비밀번호가 일치하지 않습니다.");
            throw new CustomException(UserErrorCode.PASSWORD_MISMATCH);
        }

        // 중복된 이메일인지 조회
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("[UserService] 중복된 이메일입니다.");
            throw new CustomException(UserErrorCode.EMAIL_DUPLICATE);
        }

        LocalDateTime now = LocalDateTime.now();

        // 회원가입 저장
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .termsAgreedAt(now)
                .privacyAgreedAt(now)
                .locationAgreedAt(Boolean.TRUE.equals(request.getLocationAgreed()) ? now : null)
                .build();

        // DB에 저장
        userRepository.save(user);

        // 로그 출력
        log.info("[UserService] 회원가입 성공: userId = {}", user.getId());

        // 응답 반환
        return SignUpResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }

    // 이메일 사용 가능 여부 확인
    public EmailAvailabilityResponse emailAvailability(String email) {

        // 가입된 이메일인지 조회
        boolean existsByEmail = userRepository.existsByEmail(email);

        // 가입되지 않은 이메일이면 사용 가능
        boolean available = !existsByEmail;

        // 응답 반환
        return EmailAvailabilityResponse.builder()
                .available(available)
                .build();
    }

    // 프로필 추가 설정
    @Transactional
    public ProfileAddResponse profileAdd(Long userId, ProfileAddRequest request) {

        // 로그인한 사용자 조회
        User userProfile = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 중복된 닉네임인지 조회
        if (request.getNickname() != null
                && !request.getNickname().equals(userProfile.getNickname())
                && userRepository.existsByNickname(request.getNickname())) {
            log.warn("[UserService] 중복된 닉네임입니다.");
            throw new CustomException(UserErrorCode.NICKNAME_DUPLICATE);
        }

        // 프로필 추가 설정 저장
        userProfile.updateProfile(request.getNickname(), request.getProfilePicture());

        // DB 저장
        userRepository.saveAndFlush(userProfile);

        // 로그 출력
        log.info("[UserService] 프로필 추가 설정 저장 성공: userId = {}", userProfile.getId());

        // 응답 세팅
        return ProfileAddResponse.builder()
                .userId(userProfile.getId())
                .nickname(userProfile.getNickname())
                .profilePicture(userProfile.getProfilePicture())
                .modifiedAt(userProfile.getModifiedAt())
                .build();
    }

    // 닉네임 사용 가능 여부 확인
    public NicknameAvailabilityResponse nicknameAvailability(String nickname) {

        // 중복된 닉네임인지 조회
        boolean existsByNickname = userRepository.existsByNickname(nickname);

        // 존재하지 않는 닉네임이면 사용 가능
        boolean available = !existsByNickname;

        // 응답 반환
        return NicknameAvailabilityResponse.builder()
                .available(available)
                .build();
    }

    // 내 정보 조회
    public MyProfileResponse getMyProfile(Long userId) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 응답 세팅
        List<GenreResponse> genreResponses = new ArrayList<>();
        for (UserGenre userGenre : userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(userId)) {
            genreResponses.add(toGenreResponse(userGenre.getGenre()));
        }

        // 응답 반환
        return MyProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profilePicture(user.getProfilePicture())
                .profileMusic(toMusicResponse(user.getProfileMusic()))
                .genres(genreResponses)
                .favoriteArtists(toArtistResponses(userArtistRepository.findAllWithArtistByUserIdOrderByIdAsc(userId)))
                .build();
    }

    // 선호 장르 교체
    @Transactional
    public GenreReplaceResponse replaceGenres(Long userId, GenreReplaceRequest request) {

        // 같은 사용자의 중복 요청이 순서대로 처리되도록 사용자 행을 잠그고 조회
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 요청한 장르 조회
        List<Genre> genres = genreRepository.findAllByGenreNameInOrderByDisplayOrderAsc(request.getGenres());

        // 존재하지 않는 장르가 있는지 확인
        if (genres.size() != request.getGenres().size()) {
            log.warn("[UserService] 존재하지 않는 장르입니다.");
            throw new CustomException(UserErrorCode.GENRE_NOT_FOUND);
        }

        // 현재 선호 장르 조회
        List<UserGenre> currentUserGenres = userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(userId);

        // 요청에 없는 장르는 삭제 대상, 현재 장르 id는 추가 대상 비교에 사용
        Set<Long> currentGenreIds = new HashSet<>();
        List<UserGenre> removedUserGenres = new ArrayList<>();
        for (UserGenre userGenre : currentUserGenres) {
            currentGenreIds.add(userGenre.getGenre().getId());

            if (!request.getGenres().contains(userGenre.getGenre().getGenreName())) {
                removedUserGenres.add(userGenre);
            }
        }

        // 현재 없는 장르만 추가 대상으로 세팅하고 응답 목록 세팅
        List<UserGenre> addedUserGenres = new ArrayList<>();
        List<GenreResponse> genreResponses = new ArrayList<>();
        for (Genre genre : genres) {
            if (!currentGenreIds.contains(genre.getId())) {
                addedUserGenres.add(UserGenre.builder()
                        .user(user)
                        .genre(genre)
                        .build());
            }
            genreResponses.add(toGenreResponse(genre));
        }

        // 달라진 장르만 삭제 및 추가
        userGenreRepository.deleteAllInBatch(removedUserGenres);
        userGenreRepository.saveAll(addedUserGenres);

        // 로그 출력
        log.info("[UserService] 선호 장르 교체 성공: userId = {}", userId);

        // 응답 반환
        return GenreReplaceResponse.builder()
                .genres(genreResponses)
                .build();
    }

    // 설정 조회
    public SettingsResponse getSettings(Long userId) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 응답 반환
        return toSettingsResponse(user);
    }

    // 설정 변경
    @Transactional
    public SettingsResponse updateSettings(Long userId, SettingsUpdateRequest request) {

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 요청에 값이 없으면 기존 위치 동의 여부 사용
        boolean locationAgreed = user.getLocationAgreedAt() != null;
        if (request.getLocationAgreed() != null) {
            locationAgreed = request.getLocationAgreed();
        }

        // 위치 동의 없이 위치 노출을 켜는지 확인
        if (request.getLocationVisible() != null && request.getLocationVisible() && !locationAgreed) {
            log.warn("[UserService] 위치정보 수집 및 이용 동의가 필요합니다.");
            throw new CustomException(UserErrorCode.LOCATION_AGREEMENT_REQUIRED);
        }

        // 위치 동의 저장
        if (request.getLocationAgreed() != null && request.getLocationAgreed()) {
            user.agreeLocation(LocalDateTime.now());
        }

        // 위치 동의 철회 시 동의 일시와 좌표 삭제
        if (request.getLocationAgreed() != null && !request.getLocationAgreed()) {
            user.withdrawLocationAgreement();
        }

        // 위치 노출과 편지 받기 변경
        user.updateSettings(request.getLocationVisible(), request.getLetterReceivable());

        // DB 저장
        userRepository.saveAndFlush(user);

        // 로그 출력
        log.info("[UserService] 설정 변경 성공: userId = {}", userId);

        // 응답 반환
        return toSettingsResponse(user);
    }

    // Genre 엔티티를 응답 dto로 변환
    private GenreResponse toGenreResponse(Genre genre) {
        return GenreResponse.builder()
                .genreId(genre.getId())
                .genreName(genre.getGenreName())
                .displayName(genre.getGenreName().getDisplayName())
                .build();
    }

    // 음악을 응답 dto로 변환 (없으면 null)
    private MusicResponse toMusicResponse(Music music) {
        return music == null ? null : MusicResponse.from(music);
    }

    // 좋아하는 아티스트 목록을 응답 dto 목록으로 변환
    private List<ArtistResponse> toArtistResponses(List<UserArtist> userArtists) {
        List<ArtistResponse> artistResponses = new ArrayList<>();
        for (UserArtist userArtist : userArtists) {
            artistResponses.add(ArtistResponse.from(userArtist.getArtist()));
        }
        return artistResponses;
    }

    // User 엔티티를 설정 응답 dto로 변환
    private SettingsResponse toSettingsResponse(User user) {

        // 동의 일시가 있으면 위치 동의 상태
        boolean locationAgreed = user.getLocationAgreedAt() != null;

        return SettingsResponse.builder()
                .locationVisible(locationAgreed && user.getLocationVisible())
                .letterReceivable(user.getLetterReceivable())
                .platformConnected(userPlatformRepository.existsByUserId(user.getId()))
                .termsAgreed(user.getTermsAgreedAt() != null)
                .privacyAgreed(user.getPrivacyAgreedAt() != null)
                .locationAgreed(locationAgreed)
                .build();
    }

    // 다른 사용자 프로필 조회
    public UserProfileResponse getUserProfile(Long loginUserId, Long targetUserId) {

        // 본인을 조회하는지 확인
        if (loginUserId.equals(targetUserId)) {
            log.warn("[UserService] 본인의 프로필은 다른 사용자 프로필로 조회할 수 없습니다.");
            throw new CustomException(UserErrorCode.SELF_PROFILE_NOT_ALLOWED);
        }

        // TODO: 차단 API 구현 후 차단 관계인 사용자 조회 제한
        // 조회할 사용자 조회
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 탈퇴한 사용자인지 확인
        if (targetUser.isDeleted()) {
            log.warn("[UserService] 탈퇴한 사용자입니다.");
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        // 로그인한 사용자의 선호 장르 id 조회
        Set<Long> loginUserGenreIds = new HashSet<>();
        for (UserGenre userGenre : userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(loginUserId)) {
            loginUserGenreIds.add(userGenre.getGenre().getId());
        }

        // 조회할 사용자의 선호 장르와 함께 좋아하는 장르 세팅
        List<GenreResponse> genreResponses = new ArrayList<>();
        List<GenreResponse> commonGenreResponses = new ArrayList<>();
        for (UserGenre userGenre : userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(targetUserId)) {
            GenreResponse genreResponse = toGenreResponse(userGenre.getGenre());
            genreResponses.add(genreResponse);

            if (loginUserGenreIds.contains(userGenre.getGenre().getId())) {
                commonGenreResponses.add(genreResponse);
            }
        }

        // 로그인한 사용자의 좋아하는 아티스트 id 조회
        Set<Long> loginUserArtistIds = new HashSet<>();
        for (UserArtist userArtist : userArtistRepository.findAllByUserId(loginUserId)) {
            loginUserArtistIds.add(userArtist.getArtist().getId());
        }

        // 조회할 사용자의 좋아하는 아티스트와 함께 좋아하는 아티스트 세팅
        List<UserArtist> targetUserArtists = userArtistRepository.findAllWithArtistByUserIdOrderByIdAsc(targetUserId);
        List<UserArtist> commonUserArtists = new ArrayList<>();
        for (UserArtist userArtist : targetUserArtists) {
            if (loginUserArtistIds.contains(userArtist.getArtist().getId())) {
                commonUserArtists.add(userArtist);
            }
        }

        // 프로필 음악이 같은 곡이면 함께 좋아하는 노래로 세팅 (같은 곡의 다른 음원도 같은 곡으로 봄)
        User loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));
        Music targetProfileMusic = targetUser.getProfileMusic();
        Music loginProfileMusic = loginUser.getProfileMusic();
        Music commonMusic = null;
        if (targetProfileMusic != null && loginProfileMusic != null
                && targetProfileMusic.getCanonicalId().equals(loginProfileMusic.getCanonicalId())) {
            commonMusic = targetProfileMusic;
        }

        // 함께 좋아하는 항목 세팅
        UserProfileResponse.Common common = UserProfileResponse.Common.builder()
                .artists(toArtistResponses(commonUserArtists))
                .music(toMusicResponse(commonMusic))
                .genres(commonGenreResponses)
                .build();

        // 응답 반환
        return UserProfileResponse.builder()
                .userId(targetUser.getId())
                .nickname(targetUser.getNickname())
                .profilePicture(targetUser.getProfilePicture())
                .profileMusic(toMusicResponse(targetProfileMusic))
                .genres(genreResponses)
                .favoriteArtists(toArtistResponses(targetUserArtists))
                // TODO: 매칭 구현 후 유사도 계산
                .similarity(null)
                // TODO: 위치 갱신 구현 후 거리 계산 (위치 노출을 끈 사용자는 null)
                .distanceRange(null)
                .letterReceivable(targetUser.getLetterReceivable())
                .common(common)
                .build();
    }

    // 회원탈퇴
    @Transactional
    public void deleteUser(Long userId) {

        // 로그인한 사용자 조회 (방 입장과 같은 행 잠금, 가장 먼저 잠가야 이후 조회가 최신 상태를 읽음)
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 이미 탈퇴한 사용자인지 확인
        if (user.isDeleted()) {
            log.warn("[UserService] 이미 탈퇴한 사용자입니다.");
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        // 작성한 피드와 피드에 달린 반응, 메시지 삭제
        feedMessageRepository.deleteAllByFeedUserId(userId);
        feedRepository.deleteAllByUserId(userId);

        // 선호 장르, 좋아하는 아티스트, 좋아하는 노래, 음원 플랫폼 연동 정보 삭제
        userGenreRepository.deleteAllByUserId(userId);
        userArtistRepository.deleteAllByUserId(userId);
        userMusicRepository.deleteAllByUserId(userId);
        userPlatformRepository.deleteAllByUserId(userId);

        // 차단 기록 삭제
        userBlockRepository.deleteAllByBlockerIdOrBlockedId(userId, userId);

        // 받은 알림 삭제
        notificationRepository.deleteAllByReceiverId(userId);

        // 푸시 알림 기기 토큰 삭제
        deviceTokenRepository.deleteAllByUserId(userId);

        // 참여 중인 방 나가기 (방장인 방은 가장 먼저 참여한 멤버에게 위임, 다른 멤버가 없으면 방 삭제)
        roomRemover.leaveAllRooms(userId);

        // 개인정보 파기 및 탈퇴 일시 저장
        user.deleteUser(LocalDateTime.now());

        // DB 저장
        userRepository.saveAndFlush(user);

        // 로그 출력
        log.info("[UserService] 회원탈퇴 성공: userId = {}", userId);
    }

    // 사용자 차단
    @Transactional
    public void blockUser(Long userId, Long blockedUserId) {

        // 본인을 차단하는지 확인
        if (userId.equals(blockedUserId)) {
            log.warn("[UserService] 사용자 본인은 차단할 수 없습니다.");
            throw new CustomException(UserErrorCode.SELF_BLOCK_NOT_ALLOWED);
        }

        // 로그인한 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 차단할 사용자 조회
        User blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.USER_NOT_FOUND));

        // 탈퇴한 사용자인지 확인
        if (blocked.isDeleted()) {
            log.warn("[UserService] 이미 탈퇴한 사용자입니다.");
            throw new CustomException(UserErrorCode.USER_NOT_FOUND);
        }

        // 이미 차단했는지 확인
        if (userBlockRepository.existsByBlockerIdAndBlockedId(userId, blockedUserId)) {
            log.warn("[UserService] 이미 차단한 사용자입니다.");
            throw new CustomException(UserErrorCode.ALREADY_BLOCKED);
        }

        // DB 저장
        userBlockRepository.save(UserBlock.builder()
                .blocker(user)
                .blocked(blocked)
                .type(BlockType.BLOCK)
                .build());

        // 로그 출력
        log.info("[UserService] 사용자 차단 성공: blockerId = {}, blockedId = {}", userId, blockedUserId);
    }

    // 사용자 차단 해제
    @Transactional
    public void unblockUser(Long userId, Long blockedUserId) {

        // 내가 차단한 기록 조회
        UserBlock userBlock = userBlockRepository.findByBlockerIdAndBlockedId(userId, blockedUserId)
                .orElseThrow(() -> new CustomException(UserErrorCode.BLOCK_NOT_FOUND));

        // DB 삭제
        userBlockRepository.delete(userBlock);

        // 로그 출력
        log.info("[UserService] 사용자 차단 해제 성공: blockerId = {}, blockedId = {}", userId, blockedUserId);
    }

    // 차단한 사용자 목록 조회
    public BlockedUserListResponse getBlockedUsers(Long userId) {

        // 내가 차단한 기록을 최근 차단순으로 조회
        List<UserBlock> userBlocks = userBlockRepository.findAllByBlockerIdOrderByIdDesc(userId);

        // 응답 목록 세팅
        List<BlockedUserListResponse.BlockedUser> blockedUsers = new ArrayList<>();
        for (UserBlock userBlock : userBlocks) {
            User blocked = userBlock.getBlocked();
            blockedUsers.add(BlockedUserListResponse.BlockedUser.builder()
                    .userId(blocked.getId())
                    .nickname(blocked.getNickname())
                    .profilePicture(blocked.getProfilePicture())
                    .blockedAt(userBlock.getCreatedAt())
                    .build());
        }

        // 응답 반환
        return BlockedUserListResponse.builder()
                .blockedUsers(blockedUsers)
                .build();
    }
}
