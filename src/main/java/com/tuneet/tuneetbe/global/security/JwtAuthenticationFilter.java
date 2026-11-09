package com.tuneet.tuneetbe.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 요청 헤더에서 Access Token 추출
        String token = resolveToken(request);

        try {
            // 토큰이 존재하고 유효한 경우
            if (token != null && jwtProvider.validateToken(token)) {

                // 토큰에서 userId 추출
                Long userId = jwtProvider.getUserId(token);

                // userId로 사용자 정보 조회
                CustomUserDetails userDetails = customUserDetailsService.loadUserById(userId);

                // Spring Security가 사용할 인증 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities());

                // 현재 요청의 인증 정보를 SecurityContext에 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // 토큰이 잘못되었거나 사용자 조회에 실패하면 인증 정보 제거
            SecurityContextHolder.clearContext();
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // Authorization 헤더 값 조회
        String authorization = request.getHeader("Authorization");

        // Authorization 헤더가 없거나 Bearer 형식이 아니면 null 반환
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }

        // "Bearer " 부분을 제거하고 실제 토큰만 반환
        return authorization.substring(7);
    }
}
