# 🎧 TUNEET | Backend

> **나만의 음악에서 우리의 음악으로, TUNEET**
>
> 좋아하는 장르·아티스트·노래로 음악 프로필을 만들고, 취향이 비슷한 사람을 추천받아
> 음악을 담은 편지와 방으로 연결되는 모바일 웹 서비스입니다.

- **서비스 주소**: https://www.tuneet.site
- **API 문서 (Swagger)**: https://api.kimjeongmo.shop/swagger-ui/index.html

## 👥 Team Member

| <a href="https://github.com/kjm021028"><img width="150" alt="/" src="https://avatars.githubusercontent.com/kjm021028" /></a> | <a href="https://github.com/Woohwooh03"><img width="150" alt="/" src="https://avatars.githubusercontent.com/Woohwooh03" /></a> |
| --- | --- |
| 김정모 | 김태현 |

## 🛠 기술 스택

### Backend
<img src="https://img.shields.io/badge/Java 21-F89820?style=for-the-badge&logo=openjdk&logoColor=white"> <img src="https://img.shields.io/badge/Spring Boot 4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">

### Database
<img src="https://img.shields.io/badge/MySQL 8-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> <img src="https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white">

### Auth & Notification
<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"> <img src="https://img.shields.io/badge/Firebase Cloud Messaging-DD2C00?style=for-the-badge&logo=firebase&logoColor=white">

### Infrastructure & Tools
<img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/GitHub Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white"> <img src="https://img.shields.io/badge/AWS EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white"> <img src="https://img.shields.io/badge/AWS S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white"> <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black">

## 🏗 System Architecture
<img width="3200" height="2160" alt="시스템 아키텍처" src="https://github.com/user-attachments/assets/d35dd07b-8fef-47d1-9b7c-426722ffa960" />

## 🗂 ERD
### 📌 Service DB
<img width="4220" height="1772" alt="Tuneet" src="https://github.com/user-attachments/assets/8671a6f4-f0d2-451b-a3e7-30320a17c5f2" />

### 🎵 Music DB (외부 음악 데이터)
<img width="2140" height="732" alt="외부 DB" src="https://github.com/user-attachments/assets/483900af-6418-43c0-bb56-c67562082d0a" />

## ✨ 주요 기능

| 영역            | 기능                                                                                              |
| --------------- | ------------------------------------------------------------------------------------------------- |
| **인증**        | 이메일 회원가입·로그인, JWT Access/Refresh 토큰 발급·재발급, 약관 동의 기록                        |
| **음악 프로필** | 닉네임·프로필 사진(S3 업로드), 장르·아티스트·대표곡 설정, 외부 음악 API 연동 곡 검색               |
| **유튜브 뮤직** | Google OAuth 연동, 좋아요한 곡 동기화, 플랫폼 토큰 암호화 저장, 주기적 자동 재동기화               |
| **피드**        | 음악 피드 CRUD, 좋아요, 노래를 담은 코멘트·답장                                  |
| **매칭**        | 취향 일치율 계산, 위치 기반 유저 추천, 공통 취향 조회, 차단                                        |
| **편지**        | 지정·랜덤 유저 편지 발송, 답장, 받은·보낸 편지함, 하루 발송 횟수 제한, 환영 편지                    |
| **방**          | 방 생성(비밀번호), 초대 링크 입장, 멤버별 취향 일치율, 방 피드, 공동 플레이리스트                  |
| **알림**        | FCM 푸시(코멘트·편지·답장), 트랜잭션 커밋 후 비동기 전송, 만료 토큰 자동 정리, 알림별 이동 링크    |
| **계정 관리**   | 프로필 수정, 위치 공개·편지 수신 설정, 탈퇴 시 연관 데이터 정리                                    |

<br>

## 🎯 Git Convention

- 🎉 **Start:** Start New Project [:tada:]
- ✨ **Feat:** 새로운 기능을 추가 [:sparkles:]
- 🐛 **Fix:** 버그 수정 [:bug:]
- 🎨 **Design:** CSS 등 사용자 UI 디자인 변경 [:art:]
- ♻️ **Refactor:** 코드 리팩토링 [:recycle:]
- 🔧 **Settings:** Changing configuration files [:wrench:]
- 🗃️ **Comment:** 필요한 주석 추가 및 변경 [:card_file_box:]
- ➕ **Dependency/Plugin:** Add a dependency/plugin [:heavy_plus_sign:]
- 📝 **Docs:** 문서 수정 [:memo:]
- 🔀 **Merge:** Merge branches [:twisted_rightwards_arrows:]
- 🚀 **Deploy:** Deploying stuff [:rocket:]
- 🚚 **Rename:** 파일 혹은 폴더명을 수정하거나 옮기는 작업만인 경우 [:truck:]
- 🔥 **Remove:** 파일을 삭제하는 작업만 수행한 경우 [:fire:]
- ⏪️ **Revert:** 전 버전으로 롤백 [:rewind:]

## 🪴 Branch Convention (Git Flow)

- `main`: 배포 가능한 브랜치, 항상 배포 가능한 상태를 유지
- `develop`: 개발 통합 브랜치, 기능 브랜치가 모이는 곳
- `feature/{description}`: 새로운 기능을 개발하는 브랜치
  - 예: `feature/social-login`
- 브랜치 공유 X → 특수한 경우 팀원들에게 알리기
- 팀원이 짠 코드 리뷰 없이 수정 X → 수정 시 MR 남기고 리뷰 필수
  - ex) 본인이 정의한 클래스가 아닌 곳에서 코드 작성 시 팀원들과 논의

## 🔄 Flow

1. `develop` 브랜치에서 새로운 브랜치를 생성 (`develop`은 `main`에서 생성)
2. 작업을 완료하고 커밋 컨벤션에 맞게 커밋 후 푸시
3. `develop`으로 병합 시 Merge Request(MR) 생성 / 팀원 리뷰
4. 리뷰가 완료되면 `develop` 브랜치로 병합
5. 배포가 필요하면 `develop` → `main`으로 MR 생성 / 팀원 리뷰
6. 병합 후 배포

## 💡 예시

```bash
# 새로운 기능 개발
git checkout -b feature/social-login

# 작업 완료 후, main 브랜치로 병합
git checkout main
git pull origin main
git merge feature/social-login
git push origin main
```
