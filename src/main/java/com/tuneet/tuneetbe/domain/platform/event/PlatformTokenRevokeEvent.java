package com.tuneet.tuneetbe.domain.platform.event;

// 연동 해제·회원탈퇴가 커밋된 뒤 Google 토큰을 폐기하기 위한 이벤트 (암호화된 refresh token)
public record PlatformTokenRevokeEvent(String encryptedRefreshToken) {
}
