# Tuneet-BE
TUNEET | Backend

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
