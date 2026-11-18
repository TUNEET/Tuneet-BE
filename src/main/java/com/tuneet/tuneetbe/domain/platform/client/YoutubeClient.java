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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String VIDEO_FIELDS = "items(id,contentDetails/duration,snippet/description)";
    private static final Pattern RELEASED_ON = Pattern.compile("Released on: (\\d{4})-\\d{2}-\\d{2}");

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
            if (page == null) {
                if (pageToken != null) {
                    // 중간 페이지가 비면 일부 목록으로 기존 좋아요를 지우지 않도록 중단
                    throw new CustomException(PlatformErrorCode.PLATFORM_API_UNAVAILABLE);
                }
                break;
            }
            for (PlaylistItem item : page.items() == null ? List.<PlaylistItem>of() : page.items()) {
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

    // 영상 길이와 설명의 발매 연도 (매칭이 안 된 Topic 영상 재매칭용). 실패하면 빈 결과
    public Map<String, VideoDetails> getVideoDetails(String accessToken, List<String> videoIds) {
        Map<String, VideoDetails> details = new HashMap<>();
        for (int from = 0; from < videoIds.size(); from += PAGE_SIZE) {
            String ids = String.join(",", videoIds.subList(from, Math.min(from + PAGE_SIZE, videoIds.size())));
            VideosResponse page;
            try {
                page = client.get()
                        .uri(uri -> uri.path("/videos")
                                .queryParam("part", "contentDetails,snippet")
                                .queryParam("id", "{ids}")
                                .queryParam("fields", "{fields}")
                                .build(ids, VIDEO_FIELDS))
                        .header("Authorization", "Bearer " + accessToken)
                        .retrieve()
                        .body(VideosResponse.class);
            } catch (RestClientException e) {
                log.warn("유튜브 영상 정보 조회 실패: {}", e.getMessage());
                return details;
            }
            for (Video video : page == null || page.items() == null ? List.<Video>of() : page.items()) {
                Integer seconds = durationSeconds(video);
                if (video.id() != null && seconds != null) {
                    details.put(video.id(), new VideoDetails(seconds, releaseYear(video)));
                }
            }
        }
        return details;
    }

    private static Integer durationSeconds(Video video) {
        if (video.contentDetails() == null || video.contentDetails().duration() == null) {
            return null;
        }
        try {
            long seconds = Duration.parse(video.contentDetails().duration()).getSeconds();
            return seconds > 0 ? (int) seconds : null;
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // Topic 영상 설명의 "Released on: 2015-12-09"
    private static Integer releaseYear(Video video) {
        if (video.snippet() == null || video.snippet().description() == null) {
            return null;
        }
        Matcher matcher = RELEASED_ON.matcher(video.snippet().description());
        return matcher.find() ? Integer.valueOf(matcher.group(1)) : null;
    }

    public record VideoDetails(int durationSeconds, Integer releaseYear) {
    }

    record VideosResponse(List<Video> items) {
    }

    record Video(String id, ContentDetails contentDetails, VideoSnippet snippet) {
    }

    record ContentDetails(String duration) {
    }

    record VideoSnippet(String description) {
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
