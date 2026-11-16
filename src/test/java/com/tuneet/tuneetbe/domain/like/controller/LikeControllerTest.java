package com.tuneet.tuneetbe.domain.like.controller;

import com.tuneet.tuneetbe.domain.like.service.LikeService;
import com.tuneet.tuneetbe.global.exception.GlobalExceptionHandler;
import com.tuneet.tuneetbe.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LikeControllerTest {

    private static final Long USER_ID = 1L;
    private static final String MBID = "27e95178-49ac-47a1-8b4c-540c77982680";

    private LikeService likeService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        likeService = mock(LikeService.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        given(userDetails.getUserId()).willReturn(USER_ID);
        mockMvc = MockMvcBuilders.standaloneSetup(new LikeController(likeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().equals(CustomUserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return userDetails;
                    }
                })
                .build();
    }

    @Test
    @DisplayName("MBID 형식의 id로 곡·아티스트 좋아요와 취소를 호출한다")
    void likeAndUnlike() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/likes/musics/{id}", MBID)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/users/me/likes/musics/{id}", MBID)).andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/users/me/likes/artists/{id}", MBID)).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/users/me/likes/artists/{id}", MBID)).andExpect(status().isOk());

        verify(likeService).likeMusic(USER_ID, MBID);
        verify(likeService).unlikeMusic(USER_ID, MBID);
        verify(likeService).likeArtist(USER_ID, MBID);
        verify(likeService).unlikeArtist(USER_ID, MBID);
    }

    @Test
    @DisplayName("MBID 형식이 아닌 id는 400이고 서비스를 호출하지 않는다")
    void invalidId() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/likes/musics/{id}", "not-an-id")).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/users/me/likes/artists/{id}", "123")).andExpect(status().isBadRequest());

        verify(likeService, never()).likeMusic(any(), any());
        verify(likeService, never()).unlikeArtist(any(), any());
    }

    @Test
    @DisplayName("목록의 size는 1~50만 허용한다")
    void listSize() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/likes/musics")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/me/likes/artists").param("size", "51")).andExpect(status().isBadRequest());

        verify(likeService).getLikedMusics(USER_ID, null, 20);
    }
}
