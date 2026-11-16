package com.tuneet.tuneetbe.domain.platform.client;

import com.tuneet.tuneetbe.domain.music.dto.request.VideoMatchItem;
import com.tuneet.tuneetbe.domain.platform.exception.PlatformErrorCode;
import com.tuneet.tuneetbe.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class YoutubeClient {

    // 유튜브 뮤직 좋아요 재생목록 (문서에 없는 ID, 일반 영상 좋아요는 LL)
    static final String LIKED_MUSIC_PLAYLIST_ID = "LM";
    static final int PAGE_SIZE = 50;
    static final int MAX_ITEMS = 1000;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final String FIELDS = "nextPageToken,items(snippet(title,videoOwnerChannelTitle,resourceId/videoId))";

    private final RestClient client;

    public YoutubeClient(@Value("${youtube.api-base-url:https://www.googleapis.com/youtube/v3}") String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    // 좋아요한 곡을 최신순으로 최대 1000개 조회 (삭제·비공개 영상은 제외)
    public List<VideoMatchItem> getLikedMusicVideos(String accessToken) {
        List<VideoMatchItem> videos = new ArrayList<>();
        String pageToken = null;
        int fetched = 0;
        do {
            PlaylistItemsResponse page = fetchPage(accessToken, pageToken);
            if (page == null || page.items() == null) {
                break;
            }
            for (PlaylistItem item : page.items()) {
                fetched++;
                Snippet snippet = item.snippet();
                if (snippet == null || snippet.resourceId() == null || snippet.videoOwnerChannelTitle() == null) {
                    continue;
                }
                videos.add(new VideoMatchItem(snippet.resourceId().videoId(), snippet.title(), snippet.videoOwnerChannelTitle()));
            }
            pageToken = page.nextPageToken();
        } while (pageToken != null && fetched < MAX_ITEMS);
        return videos;
    }

    private PlaylistItemsResponse fetchPage(String accessToken, String pageToken) {
        try {
            return client.get()
                    .uri(uri -> {
                        uri.path("/playlistItems")
                                .queryParam("part", "snippet")
                                .queryParam("playlistId", LIKED_MUSIC_PLAYLIST_ID)
                                .queryParam("maxResults", PAGE_SIZE)
                                .queryParam("fields", "{fields}");
                        if (pageToken != null) {
                            uri.queryParam("pageToken", "{pageToken}");
                        }
                        return pageToken == null ? uri.build(FIELDS) : uri.build(FIELDS, pageToken);
                    })
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(PlaylistItemsResponse.class);
        } catch (HttpClientErrorException.NotFound e) {
            // 유튜브 뮤직을 쓴 적이 없어 좋아요 재생목록이 없음
            return null;
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("유튜브 좋아요 조회 인증 실패: {}", e.getResponseBodyAsString());
            throw new CustomException(PlatformErrorCode.PLATFORM_AUTH_FAILED);
        } catch (RestClientException e) {
            // 403 quotaExceeded 등
            log.error("유튜브 좋아요 조회 실패: {}", e.getMessage());
            throw new CustomException(PlatformErrorCode.PLATFORM_API_UNAVAILABLE);
        }
    }

    record PlaylistItemsResponse(String nextPageToken, List<PlaylistItem> items) {
    }

    record PlaylistItem(Snippet snippet) {
    }

    record Snippet(String title, String videoOwnerChannelTitle, ResourceId resourceId) {
    }

    record ResourceId(String videoId) {
    }
}
