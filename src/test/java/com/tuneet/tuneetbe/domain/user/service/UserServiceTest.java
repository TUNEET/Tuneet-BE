package com.tuneet.tuneetbe.domain.user.service;

import com.tuneet.tuneetbe.domain.feed.repository.FeedMessageRepository;
import com.tuneet.tuneetbe.domain.feed.repository.FeedRepository;
import com.tuneet.tuneetbe.domain.music.entity.Artist;
import com.tuneet.tuneetbe.domain.music.entity.Genre;
import com.tuneet.tuneetbe.domain.music.entity.Music;
import com.tuneet.tuneetbe.domain.music.enums.GenreType;
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
import com.tuneet.tuneetbe.domain.user.dto.response.UserProfileResponse;
import com.tuneet.tuneetbe.domain.user.entity.User;
import com.tuneet.tuneetbe.domain.user.entity.UserArtist;
import com.tuneet.tuneetbe.domain.user.entity.UserBlock;
import com.tuneet.tuneetbe.domain.user.enums.BlockType;
import com.tuneet.tuneetbe.domain.user.entity.UserGenre;
import com.tuneet.tuneetbe.domain.user.exception.UserErrorCode;
import com.tuneet.tuneetbe.domain.user.repository.UserArtistRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserBlockRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserGenreRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserMusicRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserPlatformRepository;
import com.tuneet.tuneetbe.domain.user.repository.UserRepository;
import com.tuneet.tuneetbe.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "test01@email.com";
    private static final String PASSWORD = "tuneet1234!";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGenreRepository userGenreRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private UserPlatformRepository userPlatformRepository;

    @Mock
    private UserArtistRepository userArtistRepository;

    @Mock
    private UserMusicRepository userMusicRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedMessageRepository feedMessageRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private RoomRemover roomRemover;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User savedUser(String nickname, String profilePicture) {
        User user = User.builder()
                .email(EMAIL)
                .password(ENCODED_PASSWORD)
                .termsAgreedAt(LocalDateTime.now())
                .privacyAgreedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.updateProfile(nickname, profilePicture);
        return user;
    }

    private Genre genre(Long id, GenreType genreType) {
        Genre genre = BeanUtils.instantiateClass(Genre.class);
        ReflectionTestUtils.setField(genre, "id", id);
        ReflectionTestUtils.setField(genre, "genreName", genreType);
        return genre;
    }

    private Music music(String externalId, String canonicalId) {
        Music music = BeanUtils.instantiateClass(Music.class);
        ReflectionTestUtils.setField(music, "externalId", externalId);
        ReflectionTestUtils.setField(music, "canonicalId", canonicalId);
        ReflectionTestUtils.setField(music, "title", "제목");
        return music;
    }

    private UserArtist userArtist(User user, Long artistId) {
        Artist artist = BeanUtils.instantiateClass(Artist.class);
        ReflectionTestUtils.setField(artist, "id", artistId);
        ReflectionTestUtils.setField(artist, "externalId", "artist-" + artistId);
        ReflectionTestUtils.setField(artist, "name", "가수" + artistId);
        return UserArtist.builder().user(user).artist(artist).build();
    }

    @Nested
    @DisplayName("다른 사용자 프로필 조회")
    class GetUserProfile {

        private static final Long TARGET_USER_ID = 2L;

        private UserGenre userGenre(User user, Genre genre) {
            return UserGenre.builder().user(user).genre(genre).build();
        }

        @Test
        @DisplayName("상대의 프로필과 함께 좋아하는 장르를 반환한다")
        void success() {
            // given
            User loginUser = savedUser("나", null);
            User targetUser = savedUser("산책하는 튜니", "target.png");
            ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);
            Genre kPop = genre(1L, GenreType.K_POP);
            Genre pop = genre(2L, GenreType.POP);
            Genre rock = genre(10L, GenreType.ROCK);

            given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.of(targetUser));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(loginUser));
            given(userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(USER_ID))
                    .willReturn(List.of(userGenre(loginUser, kPop), userGenre(loginUser, rock)));
            given(userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(TARGET_USER_ID))
                    .willReturn(List.of(userGenre(targetUser, kPop), userGenre(targetUser, pop)));

            // when
            UserProfileResponse response = userService.getUserProfile(USER_ID, TARGET_USER_ID);

            // then
            assertThat(response.getUserId()).isEqualTo(TARGET_USER_ID);
            assertThat(response.getNickname()).isEqualTo("산책하는 튜니");
            assertThat(response.getProfilePicture()).isEqualTo("target.png");
            assertThat(response.getLetterReceivable()).isTrue();
            assertThat(response.getGenres()).extracting("genreName")
                    .containsExactly(GenreType.K_POP, GenreType.POP);
            assertThat(response.getCommon().getGenres()).extracting("genreName")
                    .containsExactly(GenreType.K_POP);
            assertThat(response.getCommon().getArtists()).isEmpty();
            assertThat(response.getCommon().getMusic()).isNull();
            assertThat(response.getIsBlocked()).isFalse();
        }

        @Test
        @DisplayName("내가 차단한 사용자의 프로필이면 isBlocked가 true이다")
        void blockedByMe() {
            // given
            User targetUser = savedUser("산책하는 튜니", null);
            ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);
            given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.of(targetUser));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser("나", null)));
            given(userBlockRepository.existsByBlockerIdAndBlockedId(USER_ID, TARGET_USER_ID)).willReturn(true);

            // when
            UserProfileResponse response = userService.getUserProfile(USER_ID, TARGET_USER_ID);

            // then
            assertThat(response.getIsBlocked()).isTrue();
        }

        @Test
        @DisplayName("겹치는 장르가 없으면 함께 좋아하는 장르는 빈 목록이다")
        void noCommonGenres() {
            // given
            User loginUser = savedUser("나", null);
            User targetUser = savedUser("산책하는 튜니", null);
            ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);

            given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.of(targetUser));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(loginUser));
            given(userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(USER_ID))
                    .willReturn(List.of(userGenre(loginUser, genre(10L, GenreType.ROCK))));
            given(userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(TARGET_USER_ID))
                    .willReturn(List.of(userGenre(targetUser, genre(1L, GenreType.K_POP))));

            // when
            UserProfileResponse response = userService.getUserProfile(USER_ID, TARGET_USER_ID);

            // then
            assertThat(response.getGenres()).hasSize(1);
            assertThat(response.getCommon().getGenres()).isEmpty();
        }

        @Test
        @DisplayName("상대의 프로필 음악과 좋아하는 아티스트, 함께 좋아하는 아티스트와 노래를 반환한다")
        void musicAndArtists() {
            // given
            User loginUser = savedUser("나", null);
            User targetUser = savedUser("산책하는 튜니", null);
            ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);
            loginUser.updateProfileMusic(music("recording-a", "song-1"));
            targetUser.updateProfileMusic(music("recording-b", "song-1"));

            given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.of(targetUser));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(loginUser));
            given(userArtistRepository.findAllByUserId(USER_ID))
                    .willReturn(List.of(userArtist(loginUser, 2L), userArtist(loginUser, 9L)));
            given(userArtistRepository.findAllWithArtistByUserIdOrderByIdAsc(TARGET_USER_ID))
                    .willReturn(List.of(userArtist(targetUser, 1L), userArtist(targetUser, 2L)));

            // when
            UserProfileResponse response = userService.getUserProfile(USER_ID, TARGET_USER_ID);

            // then
            assertThat(response.getProfileMusic().getMusicId()).isEqualTo("recording-b");
            assertThat(response.getFavoriteArtists()).extracting("artistId").containsExactly("artist-1", "artist-2");
            assertThat(response.getCommon().getArtists()).extracting("artistId").containsExactly("artist-2");
            assertThat(response.getCommon().getMusic().getMusicId()).isEqualTo("recording-b");
        }

        @Test
        @DisplayName("프로필 음악이 다른 곡이거나 한쪽이 없으면 함께 좋아하는 노래는 null이다")
        void differentProfileMusic() {
            // given
            User loginUser = savedUser("나", null);
            User targetUser = savedUser("산책하는 튜니", null);
            ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);
            loginUser.updateProfileMusic(music("recording-a", "song-1"));
            targetUser.updateProfileMusic(music("recording-c", "song-2"));
            given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.of(targetUser));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(loginUser));

            // when
            UserProfileResponse different = userService.getUserProfile(USER_ID, TARGET_USER_ID);
            loginUser.updateProfileMusic(null);
            UserProfileResponse loginNone = userService.getUserProfile(USER_ID, TARGET_USER_ID);

            // then
            assertThat(different.getProfileMusic().getMusicId()).isEqualTo("recording-c");
            assertThat(different.getCommon().getMusic()).isNull();
            assertThat(loginNone.getCommon().getMusic()).isNull();
        }

        @Test
        @DisplayName("탈퇴한 사용자를 조회하면 USER_NOT_FOUND 예외가 발생한다")
        void deletedUser() {
            // given
            User targetUser = savedUser("산책하는 튜니", null);
            ReflectionTestUtils.setField(targetUser, "id", TARGET_USER_ID);
            targetUser.deleteUser(LocalDateTime.now());
            given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.of(targetUser));

            // when & then
            assertThatThrownBy(() -> userService.getUserProfile(USER_ID, TARGET_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("본인을 조회하면 SELF_PROFILE_NOT_ALLOWED 예외가 발생하고 사용자를 조회하지 않는다")
        void selfProfile() {
            // when & then
            assertThatThrownBy(() -> userService.getUserProfile(USER_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.SELF_PROFILE_NOT_ALLOWED);

            verify(userRepository, never()).findById(any());
        }

        @Test
        @DisplayName("상대가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(TARGET_USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getUserProfile(USER_ID, TARGET_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("회원탈퇴")
    class DeleteUser {

        private User deletableUser() {
            User user = User.builder()
                    .email("test01@email.com")
                    .password("encoded-password")
                    .termsAgreedAt(LocalDateTime.now())
                    .privacyAgreedAt(LocalDateTime.now())
                    .build();
            ReflectionTestUtils.setField(user, "id", USER_ID);
            user.updateProfile("튜니", "profile.png");
            user.updateRefreshToken("hash", LocalDateTime.now());
            return user;
        }

        @Test
        @DisplayName("개인정보를 파기하고 탈퇴 일시를 저장한다")
        void destroyPersonalInformation() {
            // given
            User user = deletableUser();
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

            // when
            userService.deleteUser(USER_ID);

            // then
            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getEmail()).startsWith("deleted_1_").endsWith("@deleted.tuneet");
            assertThat(user.getPassword()).isNotEqualTo("encoded-password");
            assertThat(user.getNickname()).isNull();
            assertThat(user.getProfilePicture()).isNull();
            assertThat(user.getRefreshTokenHash()).isNull();
            verify(userRepository).saveAndFlush(user);
        }

        @Test
        @DisplayName("피드는 자식 데이터부터 삭제하고 연관 데이터와 받은 알림을 삭제한다")
        void deleteRelatedData() {
            // given
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(deletableUser()));

            // when
            userService.deleteUser(USER_ID);

            // then
            InOrder inOrder = inOrder(feedMessageRepository, feedRepository);
            inOrder.verify(feedMessageRepository).deleteAllByFeedUserId(USER_ID);
            inOrder.verify(feedRepository).deleteAllByUserId(USER_ID);
            verify(userGenreRepository).deleteAllByUserId(USER_ID);
            verify(userArtistRepository).deleteAllByUserId(USER_ID);
            verify(userMusicRepository).deleteAllByUserId(USER_ID);
            verify(userPlatformRepository).deleteAllByUserId(USER_ID);
            verify(userBlockRepository).deleteAllByBlockerIdOrBlockedId(USER_ID, USER_ID);
            verify(notificationRepository).deleteAllByReceiverId(USER_ID);
            verify(deviceTokenRepository).deleteAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("참여 중인 방을 모두 나간다")
        void leaveRooms() {
            // given
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(deletableUser()));

            // when
            userService.deleteUser(USER_ID);

            // then
            verify(roomRemover).leaveAllRooms(USER_ID);
        }

        @Test
        @DisplayName("이미 탈퇴한 사용자면 USER_NOT_FOUND 예외가 발생하고 삭제하지 않는다")
        void alreadyDeleted() {
            // given
            User user = deletableUser();
            user.deleteUser(LocalDateTime.now());
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);

            verify(feedRepository, never()).deleteAllByUserId(any());
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.deleteUser(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("설정")
    class Settings {

        private User locationAgreedUser() {
            User user = savedUser("튜니", null);
            user.agreeLocation(LocalDateTime.now());
            ReflectionTestUtils.setField(user, "latitude", 37.5);
            ReflectionTestUtils.setField(user, "longitude", 127.0);
            return user;
        }

        @Test
        @DisplayName("설정 화면의 6개 항목을 반환한다")
        void getSettings() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(locationAgreedUser()));
            given(userPlatformRepository.existsByUserId(USER_ID)).willReturn(true);

            // when
            SettingsResponse response = userService.getSettings(USER_ID);

            // then
            assertThat(response.getLocationVisible()).isTrue();
            assertThat(response.getLetterReceivable()).isTrue();
            assertThat(response.getPlatformConnected()).isTrue();
            assertThat(response.getTermsAgreed()).isTrue();
            assertThat(response.getPrivacyAgreed()).isTrue();
            assertThat(response.getLocationAgreed()).isTrue();
        }

        @Test
        @DisplayName("위치 동의를 하지 않은 사용자의 위치 노출은 false로 반환한다")
        void getSettingsWithoutLocationAgreement() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser("튜니", null)));

            // when
            SettingsResponse response = userService.getSettings(USER_ID);

            // then
            assertThat(response.getLocationVisible()).isFalse();
            assertThat(response.getLocationAgreed()).isFalse();
        }

        @Test
        @DisplayName("보낸 값만 변경하고 null인 값은 유지한다")
        void updateOnlySentValues() {
            // given
            User user = locationAgreedUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            SettingsResponse response = userService.updateSettings(USER_ID, new SettingsUpdateRequest(null, false, null));

            // then
            assertThat(response.getLetterReceivable()).isFalse();
            assertThat(response.getLocationVisible()).isTrue();
            assertThat(response.getLocationAgreed()).isTrue();
        }

        @Test
        @DisplayName("위치 동의와 위치 노출을 함께 켤 수 있다")
        void agreeAndShowLocation() {
            // given
            User user = savedUser("튜니", null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            SettingsResponse response = userService.updateSettings(USER_ID, new SettingsUpdateRequest(true, null, true));

            // then
            assertThat(user.getLocationAgreedAt()).isNotNull();
            assertThat(response.getLocationVisible()).isTrue();
        }

        @Test
        @DisplayName("위치 동의만 보내면 위치 노출이 켜진다")
        void agreeOnlyShowsLocation() {
            // given
            User user = savedUser("튜니", null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            SettingsResponse response = userService.updateSettings(USER_ID, new SettingsUpdateRequest(null, null, true));

            // then
            assertThat(user.getLocationAgreedAt()).isNotNull();
            assertThat(user.getLocationVisible()).isTrue();
            assertThat(response.getLocationVisible()).isTrue();
        }

        @Test
        @DisplayName("위치 동의와 함께 위치 노출을 끄면 꺼진 상태로 저장한다")
        void agreeAndHideLocation() {
            // given
            User user = savedUser("튜니", null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            SettingsResponse response = userService.updateSettings(USER_ID, new SettingsUpdateRequest(false, null, true));

            // then
            assertThat(user.getLocationAgreedAt()).isNotNull();
            assertThat(response.getLocationVisible()).isFalse();
        }

        @Test
        @DisplayName("철회 후 다시 동의하면 위치 노출이 켜진다")
        void reAgreeShowsLocation() {
            // given
            User user = locationAgreedUser();
            user.withdrawLocationAgreement();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            SettingsResponse response = userService.updateSettings(USER_ID, new SettingsUpdateRequest(null, null, true));

            // then
            assertThat(response.getLocationVisible()).isTrue();
        }

        @Test
        @DisplayName("이미 동의한 사용자가 다시 동의해도 기존 동의 일시를 유지한다")
        void keepAgreedAt() {
            // given
            User user = locationAgreedUser();
            LocalDateTime agreedAt = user.getLocationAgreedAt();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            userService.updateSettings(USER_ID, new SettingsUpdateRequest(null, null, true));

            // then
            assertThat(user.getLocationAgreedAt()).isEqualTo(agreedAt);
        }

        @Test
        @DisplayName("위치 동의를 철회하면 위치 좌표를 지우고 위치 노출을 끈다")
        void withdrawLocationAgreement() {
            // given
            User user = locationAgreedUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            SettingsResponse response = userService.updateSettings(USER_ID, new SettingsUpdateRequest(null, null, false));

            // then
            assertThat(user.getLocationAgreedAt()).isNull();
            assertThat(user.getLatitude()).isNull();
            assertThat(user.getLongitude()).isNull();
            assertThat(response.getLocationVisible()).isFalse();
        }

        @Test
        @DisplayName("위치 동의 없이 위치 노출을 켜면 LOCATION_AGREEMENT_REQUIRED 예외가 발생한다")
        void showLocationWithoutAgreement() {
            // given
            User user = savedUser("튜니", null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.updateSettings(USER_ID, new SettingsUpdateRequest(true, null, null)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.LOCATION_AGREEMENT_REQUIRED);
        }

        @Test
        @DisplayName("위치 동의를 철회하면서 위치 노출을 켜면 예외가 발생하고 좌표를 유지한다")
        void withdrawAndShowLocation() {
            // given
            User user = locationAgreedUser();
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when & then
            assertThatThrownBy(() -> userService.updateSettings(USER_ID, new SettingsUpdateRequest(true, null, false)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.LOCATION_AGREEMENT_REQUIRED);

            assertThat(user.getLatitude()).isEqualTo(37.5);
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getSettings(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("선호 장르 교체")
    class ReplaceGenres {

        private final Set<GenreType> requested = Set.of(GenreType.K_POP, GenreType.ROCK);

        private UserGenre userGenre(User user, Genre genre) {
            return UserGenre.builder().user(user).genre(genre).build();
        }

        @Test
        @DisplayName("달라진 장르만 삭제하고 추가하며, 유지되는 장르는 건드리지 않는다")
        void onlyChangedGenres() {
            // given
            User user = savedUser("튜니", null);
            Genre kPop = genre(1L, GenreType.K_POP);
            Genre pop = genre(2L, GenreType.POP);
            Genre rock = genre(10L, GenreType.ROCK);
            UserGenre keptGenre = userGenre(user, kPop);
            UserGenre removedGenre = userGenre(user, pop);
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
            given(genreRepository.findAllByGenreNameInOrderByDisplayOrderAsc(requested))
                    .willReturn(List.of(kPop, rock));
            given(userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(USER_ID))
                    .willReturn(List.of(keptGenre, removedGenre));

            // when
            GenreReplaceResponse response = userService.replaceGenres(USER_ID, new GenreReplaceRequest(requested));

            // then
            verify(userGenreRepository).deleteAllInBatch(List.of(removedGenre));
            ArgumentCaptor<List<UserGenre>> captor = ArgumentCaptor.forClass(List.class);
            verify(userGenreRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting("genre").containsExactly(rock);
            assertThat(response.getGenres()).extracting("genreName")
                    .containsExactly(GenreType.K_POP, GenreType.ROCK);
            assertThat(response.getGenres()).extracting("displayName")
                    .containsExactly("K-POP", "ROCK");
        }

        @Test
        @DisplayName("신규 사용자는 삭제할 장르 없이 요청한 장르를 모두 추가한다")
        void newUser() {
            // given
            User user = savedUser("튜니", null);
            Genre kPop = genre(1L, GenreType.K_POP);
            Genre rock = genre(10L, GenreType.ROCK);
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
            given(genreRepository.findAllByGenreNameInOrderByDisplayOrderAsc(requested))
                    .willReturn(List.of(kPop, rock));
            given(userGenreRepository.findAllByUserIdOrderByGenreDisplayOrderAsc(USER_ID)).willReturn(List.of());

            // when
            userService.replaceGenres(USER_ID, new GenreReplaceRequest(requested));

            // then
            verify(userGenreRepository).deleteAllInBatch(List.of());
            ArgumentCaptor<List<UserGenre>> captor = ArgumentCaptor.forClass(List.class);
            verify(userGenreRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting("genre").containsExactly(kPop, rock);
        }

        @Test
        @DisplayName("DB에 없는 장르가 있으면 GENRE_NOT_FOUND 예외가 발생하고 기존 장르를 유지한다")
        void genreNotFound() {
            // given
            User user = savedUser("튜니", null);
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.of(user));
            given(genreRepository.findAllByGenreNameInOrderByDisplayOrderAsc(requested))
                    .willReturn(List.of(genre(1L, GenreType.K_POP)));

            // when & then
            assertThatThrownBy(() -> userService.replaceGenres(USER_ID, new GenreReplaceRequest(requested)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.GENRE_NOT_FOUND);

            verify(userGenreRepository, never()).deleteAllInBatch(any());
            verify(userGenreRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findByIdForUpdate(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.replaceGenres(USER_ID, new GenreReplaceRequest(requested)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("프로필 추가 설정")
    class ProfileAdd {

        @Test
        @DisplayName("닉네임과 프로필 사진을 로그인한 사용자에게 저장한다")
        void success() {
            // given
            User user = savedUser(null, null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname("튜니")).willReturn(false);

            // when
            ProfileAddResponse response = userService.profileAdd(USER_ID, new ProfileAddRequest("튜니", "new.png"));

            // then
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getNickname()).isEqualTo("튜니");
            assertThat(response.getProfilePicture()).isEqualTo("new.png");
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            verify(userRepository).saveAndFlush(user);
        }

        @Test
        @DisplayName("프로필 사진만 바꿀 때 기존 닉네임을 함께 보내면 닉네임은 유지하고 중복 검사를 하지 않는다")
        void pictureOnly() {
            // given
            User user = savedUser("튜니", "old.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            userService.profileAdd(USER_ID, new ProfileAddRequest("튜니", "new.png"));

            // then
            assertThat(user.getNickname()).isEqualTo("튜니");
            assertThat(user.getProfilePicture()).isEqualTo("new.png");
            verify(userRepository, never()).existsByNickname(any());
        }

        @Test
        @DisplayName("닉네임만 보내면 프로필 사진은 유지한다")
        void nicknameOnly() {
            // given
            User user = savedUser("튜니", "old.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname("새닉네임")).willReturn(false);

            // when
            userService.profileAdd(USER_ID, new ProfileAddRequest("새닉네임", null));

            // then
            assertThat(user.getNickname()).isEqualTo("새닉네임");
            assertThat(user.getProfilePicture()).isEqualTo("old.png");
        }

        @Test
        @DisplayName("현재 닉네임을 그대로 보내면 중복 검사를 하지 않고 통과한다")
        void sameNickname() {
            // given
            User user = savedUser("튜니", "old.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            userService.profileAdd(USER_ID, new ProfileAddRequest("튜니", null));

            // then
            assertThat(user.getNickname()).isEqualTo("튜니");
            verify(userRepository, never()).existsByNickname(any());
        }

        @Test
        @DisplayName("다른 사용자의 닉네임이면 NICKNAME_DUPLICATE 예외가 발생하고 변경하지 않는다")
        void duplicateNickname() {
            // given
            User user = savedUser("튜니", "old.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userRepository.existsByNickname("원영")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.profileAdd(USER_ID, new ProfileAddRequest("원영", "new.png")))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.NICKNAME_DUPLICATE);

            assertThat(user.getNickname()).isEqualTo("튜니");
            assertThat(user.getProfilePicture()).isEqualTo("old.png");
            verify(userRepository, never()).saveAndFlush(any(User.class));
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.profileAdd(USER_ID, new ProfileAddRequest("튜니", null)))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("내 정보 조회")
    class GetMyProfile {

        @Test
        @DisplayName("로그인한 사용자의 닉네임과 프로필 사진을 반환하고 음악 정보는 비어 있다")
        void success() {
            // given
            User user = savedUser("튜니", "profile.png");
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            MyProfileResponse response = userService.getMyProfile(USER_ID);

            // then
            assertThat(response.getUserId()).isEqualTo(USER_ID);
            assertThat(response.getNickname()).isEqualTo("튜니");
            assertThat(response.getProfilePicture()).isEqualTo("profile.png");
            assertThat(response.getProfileMusic()).isNull();
            assertThat(response.getGenres()).isEmpty();
            assertThat(response.getFavoriteArtists()).isEmpty();
        }

        @Test
        @DisplayName("프로필 음악과 좋아하는 아티스트를 검색 결과와 같은 id로 반환한다")
        void musicAndArtists() {
            // given
            User user = savedUser("튜니", null);
            user.updateProfileMusic(music("recording-a", "song-1"));
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(userArtistRepository.findAllWithArtistByUserIdOrderByIdAsc(USER_ID))
                    .willReturn(List.of(userArtist(user, 3L), userArtist(user, 1L)));

            // when
            MyProfileResponse response = userService.getMyProfile(USER_ID);

            // then
            assertThat(response.getProfileMusic().getMusicId()).isEqualTo("recording-a");
            assertThat(response.getFavoriteArtists()).extracting("artistId").containsExactly("artist-3", "artist-1");
        }

        @Test
        @DisplayName("프로필을 설정하지 않은 사용자면 닉네임과 프로필 사진이 null이다")
        void profileNotSet() {
            // given
            User user = savedUser(null, null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));

            // when
            MyProfileResponse response = userService.getMyProfile(USER_ID);

            // then
            assertThat(response.getNickname()).isNull();
            assertThat(response.getProfilePicture()).isNull();
        }

        @Test
        @DisplayName("사용자가 없으면 USER_NOT_FOUND 예외가 발생한다")
        void userNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.getMyProfile(USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("닉네임 사용 가능 여부 확인")
    class NicknameAvailability {

        @Test
        @DisplayName("사용 중이 아닌 닉네임이면 사용 가능(true)을 반환한다")
        void available() {
            // given
            given(userRepository.existsByNickname("튜니")).willReturn(false);

            // when
            NicknameAvailabilityResponse response = userService.nicknameAvailability("튜니");

            // then
            assertThat(response.getAvailable()).isTrue();
        }

        @Test
        @DisplayName("이미 사용 중인 닉네임이면 사용 불가(false)를 반환한다")
        void unavailable() {
            // given
            given(userRepository.existsByNickname("튜니")).willReturn(true);

            // when
            NicknameAvailabilityResponse response = userService.nicknameAvailability("튜니");

            // then
            assertThat(response.getAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("회원가입")
    class SignUp {

        @Test
        @DisplayName("정상 요청이면 비밀번호를 암호화하고 약관 동의 일시와 함께 저장한다")
        void success() {
            // given
            SignUpRequest request = new SignUpRequest(EMAIL, PASSWORD, PASSWORD, true, true, false);
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);

            // when
            userService.signUp(request);

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User savedUser = captor.getValue();

            assertThat(savedUser.getEmail()).isEqualTo(EMAIL);
            assertThat(savedUser.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(savedUser.getTermsAgreedAt()).isNotNull();
            assertThat(savedUser.getPrivacyAgreedAt()).isNotNull();
            assertThat(savedUser.getLocationAgreedAt()).isNull();
        }

        @Test
        @DisplayName("위치정보 수집에 동의하면 동의 일시를 저장한다")
        void locationAgreed() {
            // given
            SignUpRequest request = new SignUpRequest(EMAIL, PASSWORD, PASSWORD, true, true, true);
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PASSWORD);

            // when
            userService.signUp(request);

            // then
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            assertThat(captor.getValue().getLocationAgreedAt()).isNotNull();
        }

        @Test
        @DisplayName("비밀번호와 비밀번호 확인이 다르면 PASSWORD_MISMATCH 예외가 발생하고 저장하지 않는다")
        void passwordMismatch() {
            // given
            SignUpRequest request = new SignUpRequest(EMAIL, PASSWORD, "different1234!", true, true, false);

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.PASSWORD_MISMATCH);

            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 EMAIL_DUPLICATE 예외가 발생하고 저장하지 않는다")
        void emailDuplicate() {
            // given
            SignUpRequest request = new SignUpRequest(EMAIL, PASSWORD, PASSWORD, true, true, false);
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.signUp(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.EMAIL_DUPLICATE);

            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("이메일 사용 가능 여부 확인")
    class EmailAvailability {

        @Test
        @DisplayName("가입되지 않은 이메일이면 사용 가능(true)을 반환한다")
        void available() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);

            // when
            EmailAvailabilityResponse response = userService.emailAvailability(EMAIL);

            // then
            assertThat(response.getAvailable()).isTrue();
        }

        @Test
        @DisplayName("이미 가입된 이메일이면 사용 불가(false)를 반환한다")
        void unavailable() {
            // given
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            // when
            EmailAvailabilityResponse response = userService.emailAvailability(EMAIL);

            // then
            assertThat(response.getAvailable()).isFalse();
        }
    }

    private User blockTarget(Long id, String nickname, String profilePicture) {
        User user = savedUser(nickname, profilePicture);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Nested
    @DisplayName("사용자 차단")
    class BlockUser {

        private static final Long BLOCKED_USER_ID = 2L;

        @Test
        @DisplayName("다른 사용자를 차단하면 차단 기록을 BLOCK 타입으로 저장한다")
        void success() {
            // given
            User me = savedUser("튜니", null);
            User target = blockTarget(BLOCKED_USER_ID, "산책하는 튜니", null);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(me));
            given(userRepository.findById(BLOCKED_USER_ID)).willReturn(Optional.of(target));
            given(userBlockRepository.existsByBlockerIdAndBlockedId(USER_ID, BLOCKED_USER_ID)).willReturn(false);

            // when
            userService.blockUser(USER_ID, BLOCKED_USER_ID);

            // then
            ArgumentCaptor<UserBlock> captor = ArgumentCaptor.forClass(UserBlock.class);
            verify(userBlockRepository).save(captor.capture());
            assertThat(captor.getValue().getBlocker()).isEqualTo(me);
            assertThat(captor.getValue().getBlocked()).isEqualTo(target);
            assertThat(captor.getValue().getType()).isEqualTo(BlockType.BLOCK);
        }

        @Test
        @DisplayName("본인을 차단하면 SELF_BLOCK_NOT_ALLOWED 예외가 발생하고 사용자를 조회하지 않는다")
        void self() {
            // when & then
            assertThatThrownBy(() -> userService.blockUser(USER_ID, USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.SELF_BLOCK_NOT_ALLOWED);
            verify(userRepository, never()).findById(any());
            verify(userBlockRepository, never()).save(any());
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 차단하면 USER_NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void targetNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser("튜니", null)));
            given(userRepository.findById(BLOCKED_USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.blockUser(USER_ID, BLOCKED_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(userBlockRepository, never()).save(any());
        }

        @Test
        @DisplayName("탈퇴한 사용자를 차단하면 USER_NOT_FOUND 예외가 발생하고 저장하지 않는다")
        void deletedTarget() {
            // given
            User target = blockTarget(BLOCKED_USER_ID, "산책하는 튜니", null);
            target.deleteUser(LocalDateTime.now());
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser("튜니", null)));
            given(userRepository.findById(BLOCKED_USER_ID)).willReturn(Optional.of(target));

            // when & then
            assertThatThrownBy(() -> userService.blockUser(USER_ID, BLOCKED_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.USER_NOT_FOUND);
            verify(userBlockRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 차단한 사용자를 다시 차단하면 ALREADY_BLOCKED 예외가 발생하고 저장하지 않는다")
        void alreadyBlocked() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(savedUser("튜니", null)));
            given(userRepository.findById(BLOCKED_USER_ID)).willReturn(Optional.of(blockTarget(BLOCKED_USER_ID, "산책하는 튜니", null)));
            given(userBlockRepository.existsByBlockerIdAndBlockedId(USER_ID, BLOCKED_USER_ID)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userService.blockUser(USER_ID, BLOCKED_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.ALREADY_BLOCKED);
            verify(userBlockRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("사용자 차단 해제")
    class UnblockUser {

        private static final Long BLOCKED_USER_ID = 2L;

        @Test
        @DisplayName("내가 차단한 기록을 삭제한다")
        void success() {
            // given
            UserBlock userBlock = UserBlock.builder()
                    .blocker(savedUser("튜니", null))
                    .blocked(blockTarget(BLOCKED_USER_ID, "산책하는 튜니", null))
                    .type(BlockType.BLOCK)
                    .build();
            given(userBlockRepository.findByBlockerIdAndBlockedId(USER_ID, BLOCKED_USER_ID)).willReturn(Optional.of(userBlock));

            // when
            userService.unblockUser(USER_ID, BLOCKED_USER_ID);

            // then
            verify(userBlockRepository).delete(userBlock);
        }

        @Test
        @DisplayName("차단하지 않은 사용자를 해제하면 BLOCK_NOT_FOUND 예외가 발생하고 삭제하지 않는다")
        void notBlocked() {
            // given
            given(userBlockRepository.findByBlockerIdAndBlockedId(USER_ID, BLOCKED_USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.unblockUser(USER_ID, BLOCKED_USER_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode")
                    .isEqualTo(UserErrorCode.BLOCK_NOT_FOUND);
            verify(userBlockRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("차단한 사용자 목록 조회")
    class GetBlockedUsers {

        private UserBlock userBlock(User blocker, User blocked, LocalDateTime createdAt) {
            UserBlock userBlock = UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .type(BlockType.BLOCK)
                    .build();
            ReflectionTestUtils.setField(userBlock, "createdAt", createdAt);
            return userBlock;
        }

        @Test
        @DisplayName("내가 차단한 사용자를 최근 차단순으로 닉네임, 프로필 사진, 차단 날짜와 함께 반환한다")
        void success() {
            // given
            User me = savedUser("튜니", null);
            LocalDateTime recentAt = LocalDateTime.of(2026, 11, 21, 10, 0);
            LocalDateTime oldAt = LocalDateTime.of(2026, 11, 20, 9, 0);
            given(userBlockRepository.findAllByBlockerIdOrderByIdDesc(USER_ID)).willReturn(List.of(
                    userBlock(me, blockTarget(3L, "커피 마시는 튜니", "coffee.png"), recentAt),
                    userBlock(me, blockTarget(2L, "산책하는 튜니", null), oldAt)));

            // when
            BlockedUserListResponse response = userService.getBlockedUsers(USER_ID);

            // then
            assertThat(response.getBlockedUsers()).extracting("userId").containsExactly(3L, 2L);
            assertThat(response.getBlockedUsers()).extracting("nickname").containsExactly("커피 마시는 튜니", "산책하는 튜니");
            assertThat(response.getBlockedUsers()).extracting("profilePicture").containsExactly("coffee.png", null);
            assertThat(response.getBlockedUsers()).extracting("blockedAt").containsExactly(recentAt, oldAt);
        }

        @Test
        @DisplayName("차단한 사용자가 없으면 빈 목록을 반환한다")
        void empty() {
            // given
            given(userBlockRepository.findAllByBlockerIdOrderByIdDesc(USER_ID)).willReturn(List.of());

            // when
            BlockedUserListResponse response = userService.getBlockedUsers(USER_ID);

            // then
            assertThat(response.getBlockedUsers()).isEmpty();
        }
    }
}
