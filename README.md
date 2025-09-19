# 📄 1. 프로젝트 기획서
<img width="1903" height="668" alt="{595346E7-5DD5-4A83-B6C6-B404B9743768}" src="https://github.com/user-attachments/assets/e9c37123-e1bf-4de4-b057-a84f8d19885a" />

##  1-1. 프로젝트 개요
- **팀명:** 뉴노멀리스트 / **사이트명:**  NewSpare
- **프로젝트명:**
MSA 기반 뉴스 수집 및 검색 플랫폼 서비스
- **진행 기간:**
2025.07.18 ~ 2025.09.10

- **설명:** 뉴스 수집 및 검색 플랫폼의 주요 서비스(회원기능, 뉴스 크롤링, 툴팁, 개인화 추천, 뉴스레터, 스크랩, 뉴스 요약 등)를 MSA 아키텍처 기반으로 구현하고, 이후 CI/CD 및 클라우드 배포 환경까지 실제 서비스 운영 경험을 목표로 한 뉴스 플랫폼 프로젝트

## 1-2. 팀원 구성

| 김지환                                                           | 박준서                                                           | 박창준                                                           | 유지은                                                           | 이채희                                                           |
| ---------------------------------------------------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------- | ---------------------------------------------------------------- |
| ![김지환](https://github.com/user-attachments/assets/df565f12-74c1-4374-b56a-2e2d34e2fb48) | ![박준서](https://github.com/user-attachments/assets/702b2f38-76c3-4c99-9a8d-cf37704a08da) | ![박창준](https://github.com/user-attachments/assets/7ddf7b0f-6d8b-423f-8c21-03e2a0292715) | ![유지은](https://github.com/user-attachments/assets/05794872-72cb-405e-af55-79a8ec897620) | ![이채희](https://github.com/user-attachments/assets/12bcc0e8-133b-48e1-b6e1-3bd67cc7b577) |
| [GitHub](https://github.com/FerryLa)                             | [GitHub](https://github.com/Berry-mas)                           | [GitHub](https://github.com/changjunpark13)                      | [GitHub](https://github.com/yde222)                              | [GitHub](https://github.com/apocalcal)                           |

## 1-3. 프로젝트 설명
본 프로젝트는 MSA 아키텍처 기반의 뉴스 검색 및 수집 플랫폼 프로젝트이다.
뉴스 서비스의 대표적인 기능인 회원기능, 뉴스 크롤링, 중복제거, 개인화 추천, 뉴스레터 발송 기능, 툴팁, 검색기능, 스크랩/컬렉션, 뉴스요약을 중심으로 구현했으며,
JWT 기반의 로그인 및 인증을 적용해 실제 서비스와 유사한 사용자 경험을 제공한다.
사용자는 개인화된 뉴스 추천, 뉴스레터 수신, 스크랩/컬렉션, 뉴스 요약 등 다양한 기능을 사용할 수 있다.
개발이 완료된 후에는 Jenkins, AWS 등의 DevOps 도구를 활용하여 자동화된 배포 환경을 구축했다.

## 1-4. 목표 및 범위

본 프로젝트의 목표는 뉴스 플랫폼의 주요 서비스 흐름과 핵심 기능을 MSA 구조에서 직접 구현하며,
실무에서 요구하는 "회원기능, 뉴스 크롤링, 중복제거, 개인화 추천, 뉴스레터 발송 기능, 툴팁, 검색기능, 스크랩/컬렉션, 뉴스요약" 등 개발을 직접 경험하는 데 있다.

또한 CI/CD, 클라우드(AWS) 환경에서의 자동화된 배포 파이프라인 구축 등
개발부터 배포까지 전 과정을 경험하고, 실제 서비스와 유사한 환경을 구축하는 것을 목표로 한다.

## 1-5. 타겟 사용자

- **일반 사용자**
  : 뉴스를 탐색하고 개인화된 추천을 받으며, 스크랩/컬렉션 및 뉴스레터 수신 등 뉴스 서비스를 이용하는 고객

- **관리자**
  : 뉴스 승격, 통계 데이터, 사용자 관리 등 서비스 데이터와 콘텐츠를 관리하는 관리자 계정

## 1-6. 주요 기능 목록

| 메인 홈페이지 | 소셜 로그인 및 마이페이지 |
|:-------------:|:-------------------------:|
| ![Image1](https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExaGV6ZGk3dDgxMGE2NnRyYWR0Z25rdzl6cm12MGNrazR3czJ1NHR4byZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/LcpAJc8xPJggEHUTPB/giphy.gif) | ![Image](https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExYWpqcHkzdmwzNXF6cXhxZGN5Y2NzZXo5cWdudzhrcWtoM3ZrN3NpeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/WJ6ZFum7wMPhSY0RSf/giphy.gif) |

| 검색 및 스크랩 | 뉴스레터 |
|:-------------:|:-------------------------:|
|![Image3](https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExamxqaWpqbnllbHFhcmprbHh2MWs5dzlhZXdmYTFmaXp5eHdhdjI5ZyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/0Mi1yG8QWCgQwfuDQm/giphy.gif) |![Image4](https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExZWVocDc2YmhiczR6eHJsenY4eHpyNTA1NnhwNjNnZGZ2NXczenZvNiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/m6uJMt2qKeJbhRgXQf/giphy.gif) |

| AI요약 및 호버링 | 컬렉션 |
|:-------------:|:-------------------------:|
|![Image5](https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExd3dmNmo5Z2p6ZGlicWNvOWY0YXAwbDA1dXZ6c3M3b2FvaWlmbGpoeCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/z0MWtpmcy4dOBjMDcH/giphy.gif) | ![Image6](https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExOGp3NnNxZHcxdGJkZjh3YjFrZzZxMXl3ZW1tMXd3MzBlcDB4Ym5wdiZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9Zw/leceLLl9UO0NsSwkGW/giphy.gif)|

- **회원가입 및 로그인**
  : 이메일 기반 회원가입/로그인, JWT 인증 적용
- **뉴스 크롤링 및 중복제거**
  : 네이버 뉴스 크롤링, Python 기반 SBERT 중복제거 시스템
- **개인화 뉴스 추천**
  : 사용자 선호도 기반 맞춤형 뉴스 추천 시스템
- **뉴스레터 발송**
  : 개인화된 뉴스레터 생성 및 이메일 발송
- **뉴스 스크랩 및 컬렉션**
  : 뉴스 스크랩, 컬렉션 생성 및 관리 기능
- **뉴스 분석 및 툴팁**
  : 어려운 단어 자동 분석 및 툴팁 제공
- **뉴스 신고 기능**
  : 부적절한 뉴스 신고 및 관리 기능
- **AI 요약**
  : OpenAI 기반 뉴스 요약 기능

## 1-7. 담당 기능

| 담당자 | 서비스명 (`영문-service`)          | 주요 역할/설명                                                 |
| ------ | ---------------------------------- | -------------------------------------------------------------- |
| 김지환 | **AI 요약 (`flaskapi`)**           | Flask API 및 AI 요약 산출물 관리                               |
| 박준서 | **배포/크롤링/툴팁/추천/중복제거** | 배포 작업, 크롤링, 툴팁, 개인화 추천 로직, 중복제거            |
| 박창준 | **뉴스/스크랩/신고 서비스**        | 뉴스 서비스, 스크랩 기능, 신고 기능                            |
| 유지은 | **프론트엔드/뉴스레터/검색/개인화**            | Next.js 기반 프론트엔드 개발, 뉴스레터 서비스, UI/UX 구현, 개인화 뉴스 추천, 검색기능    |
| 이채희 | **회원/보안/인프라**               | 회원 기능, 보안 및 인프라, Config, Gateway, Discovery, Swagger |

## 1-8. MSA 식 구조
<img width="1000" height="1200" alt="image" src="https://github.com/user-attachments/assets/da8a3c64-343f-44be-bd4b-48e4487a7c89" />


| 모듈명                   | 기능 역할                                                          | 담당자 |
| ------------------------ | ------------------------------------------------------------------ | ------ |
| **`news-service`**            | 뉴스 CRUD, 스크랩 기능, 신고 기능, 트렌딩 뉴스, 카테고리 관리          | 박창준 |
| **`user-service`**            | 회원가입/로그인, JWT 인증, 마이페이지, 읽기 이력, 사용자 정보 관리      | 이채희 |
| **`crawler-service`**         | 네이버 뉴스 크롤링, 파일서버 연동, 크롤링 데이터 관리                   | 박준서 |
| **`newsletter-service`**      | 개인화 뉴스레터 생성, 이메일 발송, 구독 관리                            | 유지은 |
| **`tooltip-service`**         | 뉴스 본문 NLP 분석, 어려운 단어 툴팁 제공, 단어 정의 관리               | 박준서 |
| **`dedup-service`**           | Python 기반 SBERT 중복제거, FastAPI 서비스                              | 박준서 |
| **`flaskapi`**                | OpenAI 기반 뉴스 요약, AI 기능 제공                                     | 김지환 |
| **`gateway-service`**         | API Gateway, 라우팅, 인증, 로드밸런싱                                   | 이채희 |
| **`discovery-service`**       | Eureka 서버, 서비스 등록/발견                                           | 이채희 |
| **`config-service`**          | 공통 환경설정, JWT 토큰 및 마이크로서비스 중앙 설정 관리                | 이채희 |

---

# 👤 2. 요구사항 정의서

## 2-1. 프로젝트 개요

- **목표**: 사용자가 다양한 뉴스 정보를 쉽고 편리하게 조회·구독할 수 있는 개인화 뉴스 플랫폼 서비스 구현
- **구성**: MSA 기반
- **주요 기능**: 회원 관리, 뉴스 크롤링/중복제거, 개인화 추천, 뉴스레터, 스크랩/컬렉션, AI 요약 등

## 2-2. 사용자 영역 요구사항

### 회원 관리
| TC ID | 기능 명                        | 목적/설명                      |
| ----- | ------------------------------ | ------------------------------ |
| US-01 | 회원 중복 체크                 | 중복 회원(이메일) 가입 차단    |
| US-02 | 회원가입 성공                  | 신규 회원 정상 가입            |
| US-03 | 회원탈퇴 성공                  | 회원 탈퇴 요청 정상 처리       |
| US-04 | 마이페이지 정보 조회           | 로그인한 사용자 정보 조회      |
| US-05 | 회원 정보 수정                 | 사용자가 자신의 정보 수정 가능 |
| US-06 | 회원정보 수정 전 비밀번호 확인 | 정보 수정 전 비밀번호 확인     |
| US-07 | 비밀번호 변경                  | 비밀번호 수정 정상 동작        |
| US-08 | 로그인 성공                    | 정상 로그인                    |
| US-09 | 토큰 재발급                    | accessToken 만료 시 재발급     |
| US-10 | 로그아웃                       | 정상 로그아웃 처리             |

### 뉴스 관리
| TC ID | 기능 명              | 목적/설명                |
| ----- | -------------------- | ------------------------ |
| NS-01 | 뉴스 전체 목록 조회  | 모든 뉴스 정상 조회      |
| NS-02 | 특정 뉴스 상세 조회  | 단일 뉴스 상세 정보 조회 |
| NS-03 | 뉴스 검색/필터링     | 조건 검색된 뉴스만 반환  |
| NS-04 | 카테고리별 뉴스 조회 | 카테고리별 뉴스만 조회   |
| NS-05 | 트렌딩 뉴스 조회     | 인기 뉴스 조회           |
| NS-06 | 개인화 뉴스 추천     | 사용자 맞춤 뉴스 추천    |
| NS-07 | 뉴스 조회수 증가     | 뉴스 조회 시 조회수 증가 |
| NS-08 | 뉴스 스크랩          | 뉴스 스크랩 기능         |
| NS-09 | 뉴스 신고            | 부적절한 뉴스 신고 기능  |
| NS-10 | 뉴스 요약            | AI 기반 뉴스 요약 기능   |

### 스크랩 및 컬렉션 관리
| TC ID | 기능 명            | 목적/설명                    |
| ----- | ------------------ | ---------------------------- |
| SC-01 | 뉴스 스크랩        | 관심 뉴스 스크랩 저장        |
| SC-02 | 스크랩 목록 조회   | 사용자 스크랩 뉴스 목록 조회 |
| SC-03 | 스크랩 해제        | 스크랩한 뉴스 해제           |
| SC-04 | 컬렉션 생성        | 뉴스 컬렉션 생성             |
| SC-05 | 컬렉션 관리        | 컬렉션 수정/삭제/조회        |
| SC-06 | 컬렉션에 뉴스 추가 | 컬렉션에 뉴스 추가/제거      |

### 뉴스레터 관리
| TC ID | 기능 명              | 목적/설명                 |
| ----- | -------------------- | ------------------------- |
| NL-01 | 뉴스레터 구독 등록   | 뉴스레터 구독 신청        |
| NL-02 | 뉴스레터 구독 해지   | 뉴스레터 구독 해지        |
| NL-03 | 개인화 뉴스레터 생성 | 사용자 맞춤 뉴스레터 생성 |
| NL-04 | 뉴스레터 이메일 발송 | 구독자에게 뉴스레터 발송  |
| NL-05 | 뉴스레터 미리보기    | 뉴스레터 내용 미리보기    |

### AI 기능 관리
| TC ID | 기능 명        | 목적/설명                 |
| ----- | -------------- | ------------------------- |
| AI-01 | 뉴스 요약 생성 | OpenAI 기반 뉴스 요약     |
| AI-02 | 뉴스 본문 분석 | 어려운 단어 자동 분석     |
| AI-03 | 단어 툴팁 제공 | 어려운 단어 정의 툴팁     |
| AI-04 | 중복 뉴스 제거 | SBERT 기반 중복 뉴스 제거 |

### 관리자 영역 요구사항
| TC ID | 기능 명              | 목적/설명                      |
| ----- | -------------------- | ------------------------------ |
| AD-01 | 관리자 로그인        | 관리자 계정 로그인             |
| AD-02 | 관리자 대시보드 조회 | 대시보드에 집계 정보 정상 조회 |
| AD-03 | 뉴스 승격 관리       | 크롤링된 뉴스를 승격하여 노출  |
| AD-04 | 뉴스 관리            | 뉴스 등록/수정/삭제 관리       |
| AD-05 | 사용자 관리          | 사용자 목록 조회 및 관리       |
| AD-06 | 통계 데이터 조회     | 서비스 이용 통계 조회          |
| AD-07 | 크롤링 관리          | 뉴스 크롤링 상태 및 설정 관리  |

---

# 📜 3. 문서

### 3-1 프로젝트 문서
- [**`WBS`**](https://drive.google.com/file/d/1KMIRRldpsOt_d2XmYJy9O4sseIVCdpa2/view)
- [**`ERD`**](https://www.erdcloud.com/d/rJGtPgDQyZvDcfZMv)
- [**`MSA 아키텍쳐 설계`**](https://www.notion.so/coffit23/MSA-269a02b1ffb180f680aac8018b7ff206)

### 3-2 데이터 구축 설계
- [**`웹 크롤러 및 수집 데이터`**](https://www.notion.so/coffit23/269a02b1ffb180fbaf23eeca1205a643)
- [**`프롬프트 엔지니어링 설계서`**](https://www.notion.so/coffit23/269a02b1ffb180749572f47582d0d1c7)
- [**`API 명세서`**](https://www.notion.so/coffit23/API-269a02b1ffb18035a7f1e3ab735f730b)

### 3-3 UI/UX 문서 (프런트엔드)
- [**`UI 테스트 케이스`**](https://www.notion.so/coffit23/UI-269a02b1ffb180469aa5ce91d4885150)
- [**`스토리보드`**](https://www.notion.so/coffit23/269a02b1ffb1805f97dde08fde4376b0)

### 3-4 API 상세 명세서 (백엔드)
- [**`인터페이스 설계서`**](https://www.notion.so/coffit23/269a02b1ffb180d697f8dc2b4d2d750e)
- [**`테스트 케이스`**](https://www.notion.so/coffit23/269a02b1ffb1804aa2a8cd1bd2176fc8)
- [**`테스트 결과서`**](https://www.notion.so/coffit23/269a02b1ffb180ba882fceb6f2022687)

### 3-5 테스트 배포 계획
- [**`프로젝트 테스트 결과서`**](https://www.notion.so/coffit23/269a02b1ffb180469932f6b3510870bf)
- [**`CI/CD 설계서`**](https://www.notion.so/coffit23/CI-CD-269a02b1ffb1805c8faec7ceca3ce7d6)


---

# 🛠 4. 기술 스택

## 4-1. 프론트엔드 기술 스택

| 항목                      | 사용 기술                                                                                                                                                                                                                                                                                                                                  |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **프론트엔드 언어**       | ![JavaScript](https://img.shields.io/badge/JAVASCRIPT-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black) ![TypeScript](https://img.shields.io/badge/TYPESCRIPT-3178C6?style=for-the-badge&logo=typescript&logoColor=white)                                                                                                        |
| **프론트엔드 프레임워크** | ![Next.js](https://img.shields.io/badge/NEXT.JS_15-000000?style=for-the-badge&logo=next.js&logoColor=white) ![React](https://img.shields.io/badge/REACT_19-61DAFB?style=for-the-badge&logo=react&logoColor=black)                                                                                                                          |
| **스타일링**              | ![Tailwind CSS](https://img.shields.io/badge/TAILWIND_CSS-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white) ![CSS3](https://img.shields.io/badge/CSS3-1572B6?style=for-the-badge&logo=css3&logoColor=white)                                                                                                                    |
| **UI 컴포넌트**           | ![Shadcn/ui](https://img.shields.io/badge/SHADCN/UI-000000?style=for-the-badge&logo=shadcn&logoColor=white) ![Radix UI](https://img.shields.io/badge/RADIX_UI-161618?style=for-the-badge&logo=radix-ui&logoColor=white) ![Lucide React](https://img.shields.io/badge/LUCIDE_REACT-FF6B6B?style=for-the-badge&logo=lucide&logoColor=white)  |
| **상태 관리**             | ![React Context](https://img.shields.io/badge/REACT_CONTEXT-61DAFB?style=for-the-badge&logo=react&logoColor=black) ![SWR](https://img.shields.io/badge/SWR-000000?style=for-the-badge&logo=swr&logoColor=white) ![TanStack Query](https://img.shields.io/badge/TANSTACK_QUERY-FF4154?style=for-the-badge&logo=react-query&logoColor=white) |
| **인증/소셜 로그인**      | ![Google OAuth](https://img.shields.io/badge/GOOGLE_OAUTH-4285F4?style=for-the-badge&logo=google&logoColor=white) ![Kakao Login](https://img.shields.io/badge/KAKAO_LOGIN-FFCD00?style=for-the-badge&logo=kakao&logoColor=black)                                                                                                           |
| **공유 기능**             | ![Kakao Share](https://img.shields.io/badge/KAKAO_SHARE-FFCD00?style=for-the-badge&logo=kakao&logoColor=black)                                                                                                                                                                                                                             |
| **폼 관리**               | ![React Hook Form](https://img.shields.io/badge/REACT_HOOK_FORM-EC5990?style=for-the-badge&logo=react-hook-form&logoColor=white) ![Zod](https://img.shields.io/badge/ZOD-3E67B1?style=for-the-badge&logo=zod&logoColor=white)                                                                                                              |
| **테마 관리**             | ![Next Themes](https://img.shields.io/badge/NEXT_THEMES-000000?style=for-the-badge&logo=next.js&logoColor=white)                                                                                                                                                                                                                           |
| **개발 도구**             | ![ESLint](https://img.shields.io/badge/ESLINT-4B32C3?style=for-the-badge&logo=eslint&logoColor=white) ![TypeScript](https://img.shields.io/badge/TYPESCRIPT-3178C6?style=for-the-badge&logo=typescript&logoColor=white) ![PostCSS](https://img.shields.io/badge/POSTCSS-DD3A0A?style=for-the-badge&logo=postcss&logoColor=white)           |

## 4-2. 백엔드 기술 스택

| 항목                  | 사용 기술                                                                                                                                                                                                                                                                                                                                                                                                                            |
| --------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| **백엔드 언어**       | ![Java](https://img.shields.io/badge/JAVA-007396?style=for-the-badge&logo=java&logoColor=white) ![Python](https://img.shields.io/badge/PYTHON-3776AB?style=for-the-badge&logo=python&logoColor=white)                                                                                                                                                                                                                                |
| **백엔드 프레임워크** | ![Spring](https://img.shields.io/badge/SPRING-6DB33F?style=for-the-badge&logo=spring&logoColor=white) ![Spring Boot](https://img.shields.io/badge/SPRINGBOOT-6DB33F?style=for-the-badge&logo=springboot&logoColor=white) ![FastAPI](https://img.shields.io/badge/FASTAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white) ![Flask](https://img.shields.io/badge/FLASK-000000?style=for-the-badge&logo=flask&logoColor=white) |
| **데이터베이스**      | ![MySQL](https://img.shields.io/badge/MYSQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)                                                                                                                                                                                                                                                                                                                                   |
| **AI/ML**             | ![OpenAI](https://img.shields.io/badge/OPENAI-412991?style=for-the-badge&logo=openai&logoColor=white) ![SBERT](https://img.shields.io/badge/SBERT-FF6B6B?style=for-the-badge&logo=sentence-transformers&logoColor=white)                                                                                                                                                                                                             |
| **협업/버전관리**     | ![GitHub](https://img.shields.io/badge/GITHUB-181717?style=for-the-badge&logo=github&logoColor=white) ![Git](https://img.shields.io/badge/GIT-F05032?style=for-the-badge&logo=git&logoColor=white)                                                                                                                                                                                                                                   |
| **배포/운영**         | ![Docker](https://img.shields.io/badge/DOCKER-2496ED?style=for-the-badge&logo=docker&logoColor=white) ![Kubernetes](https://img.shields.io/badge/KUBERNETES-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white) ![Jenkins](https://img.shields.io/badge/JENKINS-D24939?style=for-the-badge&logo=jenkins&logoColor=white) ![AWS](https://img.shields.io/badge/AWS-232F3E?style=for-the-badge&logo=amazonaws&logoColor=white)  |

---

# 🏗️ 5. 프론트엔드 아키텍처 및 구조
## 5-1. 프로젝트 구조

```
app/
├── (admin)/              # 관리자 페이지 그룹
│   ├── admin/            # 관리자 대시보드
│   └── newsletter/       # 뉴스레터 관리
├── (auth)/               # 인증 관련 페이지 그룹
│   ├── auth/             # 로그인/회원가입
│   ├── forgot-password/  # 비밀번호 찾기
│   ├── oauth/            # 소셜 로그인
│   └── reset-password/   # 비밀번호 재설정
├── (news)/               # 뉴스 관련 페이지 그룹
│   └── news/             # 뉴스 목록/상세
├── (newsletter)/         # 뉴스레터 관련 페이지 그룹
│   └── newsletter/       # 뉴스레터 구독/관리
├── (user)/               # 사용자 관련 페이지 그룹
│   ├── mypage/           # 마이페이지
│   └── user/             # 사용자 정보
├── api/                  # API 라우트
└── components/           # 공통 컴포넌트

components/
├── ui/                   # Shadcn/ui 기본 컴포넌트
├── header.jsx            # 헤더 컴포넌트
├── footer.jsx            # 푸터 컴포넌트
├── NewsCard.jsx          # 뉴스 카드 컴포넌트
├── NewsletterTemplate.jsx # 뉴스레터 템플릿
└── ...                   # 기타 공통 컴포넌트

contexts/
├── MypageContext.jsx     # 마이페이지 상태 관리
└── ScrapContext.jsx      # 스크랩 상태 관리

hooks/
├── useInterests.js       # 관심사 관리 훅
├── useKakaoShare.js      # 카카오 공유 훅
├── useNewsletter.js      # 뉴스레터 훅
└── useSummary.jsx        # AI 요약 훅
```

## 5-2. 프론트엔드 주요 기능

### 인증 및 사용자 관리

- **소셜 로그인**: Google OAuth, Kakao Login 연동
- **JWT 토큰 관리**: 자동 토큰 갱신 및 만료 처리
- **사용자 인증**: 보호된 라우트 및 권한 기반 접근 제어
- **마이페이지**: 사용자 정보 수정, 구독 관리, 스크랩 관리

### 뉴스 관련 기능

- **뉴스 목록**: 카테고리별, 트렌딩 뉴스 표시
- **뉴스 상세**: 상세 정보, 관련 뉴스, AI 요약
- **뉴스 검색**: 실시간 검색 자동완성, 필터링
- **뉴스 스크랩**: 개별 스크랩 및 컬렉션 관리
- **뉴스 공유**: 카카오톡 공유 기능

### 뉴스레터 기능

- **구독 관리**: 카테고리별 뉴스레터 구독/해지
- **뉴스레터 미리보기**: 이메일 발송 전 미리보기
- **개인화 설정**: 관심사 기반 뉴스레터 커스터마이징

### AI 기능

- **뉴스 요약**: OpenAI 기반 뉴스 요약
- **툴팁**: 어려운 단어 자동 분석 및 툴팁 제공
- **관련 뉴스**: AI 기반 관련 뉴스 추천

### UI/UX 특징

- **반응형 디자인**: 모바일, 태블릿, 데스크톱 대응
- **다크/라이트 모드**: 테마 전환 기능
- **접근성**: WCAG 가이드라인 준수
- **성능 최적화**: 이미지 지연 로딩, 코드 스플리팅

## 5-3. 상태 관리 구조

### Context API 활용

- **MypageContext**: 마이페이지 관련 상태 (구독, 스크랩, 설정)
- **ScrapContext**: 스크랩 및 컬렉션 상태 관리
- **ThemeProvider**: 다크/라이트 모드 테마 관리

### 데이터 페칭 및 상태 관리

- **SWR**: 서버 상태 관리 및 캐싱
- **React Context**: 전역 상태 관리 (마이페이지, 스크랩, 테마)

### 커스텀 훅 패턴

- **useInterests**: 사용자 관심사 관리
- **useKakaoShare**: 카카오 공유 기능
- **useNewsletter**: 뉴스레터 구독 관리
- **useSummary**: AI 요약 기능
- **useLoading**: 로딩 상태 관리
- **usePerformance**: 성능 모니터링

## 5-4. API 통신 구조

### API 라우트 (Next.js App Router)

```
app/api/
├── auth/                 # 인증 관련 API
├── news/                 # 뉴스 관련 API
├── newsletter/           # 뉴스레터 API
├── users/                # 사용자 API
└── weather/              # 날씨 API
```

### 서비스 레이어

- **newsService.js**: 뉴스 관련 API 호출
- **newsletterService.js**: 뉴스레터 관련 API 호출
- **api-utils.js**: 공통 API 유틸리티 함수

## 5-5. 미들웨어 및 인증 관리

### Next.js Middleware

- **경로 보호**: `/admin`, `/mypage` 등 인증이 필요한 경로 자동 보호
- **쿠키 기반 인증**: HttpOnly 쿠키(`access-token`)를 통한 JWT 토큰 관리
- **자동 리디렉션**: 인증되지 않은 사용자를 `/auth` 페이지로 자동 리디렉션
- **매처 설정**: API, 정적 파일, favicon 등을 제외한 모든 경로에 적용

### 인증 플로우

1. **로그인 성공** → HttpOnly 쿠키에 JWT 토큰 저장
2. **보호된 경로 접근** → 미들웨어가 쿠키 존재 여부 확인
3. **인증 실패** → 자동으로 로그인 페이지로 리디렉션
4. **인증 성공** → 페이지 접근 허용

### 보안 특징

- **HttpOnly 쿠키**: XSS 공격 방지를 위한 클라이언트 사이드 접근 차단
- **자동 토큰 갱신**: 백엔드에서 토큰 만료 시 자동 갱신 처리
- **경로별 권한 관리**: 관리자/사용자별 접근 권한 구분

---

# 📝 6. 회고

### `김지환`

- **주요 역할** : AI 요약 (Flask API 및 산출물 관리)
- **느낀 점** : 기획부터 개발단계까지 팀원과 소통을 끊이지 않고 했던 것이 제일 잘한 부분이라고 생각합니다.
여러 논점이 많았는데 기획단계에서는 시장에 대한 다른 시각으로 프로젝트의 방향성을 잡는 것이 많이 어려웠지만 그렇게 많이 고심한 탓에 좋은 결과물이 만들어진 것 같습니다.

  제가 모르는 부분이 많았기 때문에 팀원들의 도움이 컸습니다. 특히 MSA 설정에서 모르는 부분을 자꾸 물어봤고, 그 덕에 Config, Eureka, Gateway 서버의 역할을 자세하게 배울 수 있었습니다. 
저는 주로 교육과정과 다른 AI요약기능을 개발하고, 프롬프트를 어떻게 활용할지 연구했고, 팀원들의 코드를 보며 따로 공부했습니다. 그 덕에 프로젝트에 구현된 기능에 대해선 프로세스 흐름을 파악할 수 있었습니다.
제가 맡은 기능이 현저히 적은 부분이 많이 아쉽습니다. 다만, 이전 단위 프로젝트에서 제대로 하지 못했던 병합까지 깔끔하게 할 수 있게 되었고, 맡은 기능이 작다보니 정규화도 다양한 방법으로 할 수 있었습니다. 팀원과 협업 활동으로 깃 관리 능력 등 여러 부분에서 발전한 것 같습니다. 
결과적으로 각 기술스택에 대해선 하나씩은 제대로 챙겨가는 것 같습니다.

  이 프로젝트를 마무리하면서 느낀 점은 기술역량이 많이 부족하다는 생각 듭니다. 이번 경험을 바탕으로 부족한 공부를 더 하며 기회가 되는대로 프로젝트를 많이 임해볼 생각입니다.
특히, CRUD, JWT, AWS를 중점적으로 복습할 생각입니다.

### `박준서`

- **주요 역할** : 배포 작업, 크롤링, 툴팁, 개인화 추천 로직, 중복제거
- **느낀 점** : 파이널 프로젝트는 도전의 연속이었습니다. 모든 기능이 산 넘어 산이었고, 어떻게 백엔드에서 구현해야 할지 고민을 많이 해야 했습니다. 그 과정이 쉽지는 않았지만 덕분에 많이 공부할 수 있었고, 즐거웠습니다. 팀원들을 너무 잘 만났다는 생각이 듭니다. 큰 이슈 없이 협업이 잘 이루어져서 만족스러웠습니다. 물론 협업 과정이 쉽지는 않습니다. 협업 매뉴얼이 얼마나 중요한지 뼈저리게 느꼈습니다. 소통의 능력, 매뉴얼을 지키는 능력이 어쩌면 개발과정에서 그 무엇보다 중요하다는 생각이 들었습니다.
  툴팁을 구현하는 게 가장 어려웠습니다. Redis를 처음 사용해봤고, api로 사전을 가져올지 자체 사전을 구축할지 고민을 많이 했습니다. 고민의 연속과 최적의 선택을 내려야 하는 순간들이 가장 어려웠습니다. 많은 공부와 경험을 통해 앞으로 최선의 선택을 할 수 있는 사람이 되겠노라 다짐하는 계기가 되었습니다.
  이번 파이널 프로젝트는 학습과 경험 두마리 토끼를 잡은 프로젝트였습니다. 산출물 작성법, msa 기반 아키텍처의 흐름 등 거시적인 관점에서 코드를 보는 능력을 기를 수 있었습니다. 어떤 흐름으로 코드를 짜야 원활히 정보가 이동하고, 어떤 방향성을 가지고 프로젝트를 운영해야 사용자 입장에서 좋은 사이트가 될 수 있을지 고민하고 판단하는 시간을 가질 수 있어 뜻깊었습니다. 플레이데이터에서 겪은 이러한 경험들을 바탕으로 더 성장해나갈 수 있도록 해야겠습니다.

### `박창준`

- **주요 역할** : 뉴스 서비스, 스크랩 기능, 신고 기능, 컬렉션 기능
- **느낀 점** : 이번 파이널 프로젝트는 단순히 프론트엔드와 백엔드 기능을 구현하는 것을 넘어, 하나의 아이디어가 어떻게 코드와 인프라를 거쳐 사용자에게 전달되는 과정을 온몸으로 부딪히며 배운 소중한 경험이었습니다. 뉴스 서비스와 컬렉션 기능을 개발하며 MSA의 구조와 서비스 간의 협력 방식을 익혔고, 스크 기능, 신고 기능, 컬력센 기능, 뉴스 서비스 기능을 만들며 뉴스의 대한 사용자의 가치와 서비스의 안정성을 고민했습니다. 하지만 저희에게 가장 큰 도전이자 가장 값진 성장을 안겨준 것은 단연 CI/CD 파이프라인 구축과 배포 자동화였습니다. 배포하는 과정은 수많은 시행착오의 연속이었습니다.

  특히, 팀원분들이 없었다면 배포하는데 많은 어려움이 있었을 거 같습니다. 많이 어려웠지만, 코드를 푸시했을 때 자동으로 테스트, 빌드, 배포가 이루어지는 것을 처음 목격했을 때 팀원 모두가 느꼈던 성취감은 그 어떤 기능 개발보다도 컸습니다. 그리고 이번 프로젝트는 특정 기술의 습득을 넘어, 시스템 전체를 보는 시야를 갖게 해주었습니다. 앞으로는 AI 발전으로 인해 코드 작성도 중요하지만 설계능력과 얼마나 잘 협업하고 다양한 개발 도구를 활용해 잘 해결해 나갈 수 있는 사람이 개발자에게 중요해질거 같은데 이번 프로젝트는 모든 경험을 해볼 수 있던거 같아서 의미있던 경험이었습니다.

  사실 이번 부트캠프를 통해 처음 배우는게 대다수였고 많은 걱정이 있었고 어려움도 많았지만 좋은 강사님, 그리고 좋은 동료분들 덕분에 파이널 프로젝트까지 잘 마무리 할 수 있어서 정말 의미있던 시간을 보낼 수 있어서 감사했던 거 같습니다. 덕분에 단순히 코드를 작성하는 것을 넘어, 아키텍처를 설계하고, 서비스간의 관계를 조율하며, 사용자의 가치를 고민하는 등 다각적인 시야를 가진 개발자로 한층 더 성장할 수 있었고, 앞으로는 단순히 주어진 기능을 구현하는 것에 그치지 않고, 비즈니스 요구사항부터 아키텍처 설계, 안정적인 배포와 운영까지 고려하고 제가 작성한 코드가 어떻게 비즈니스 가치로 이어지고, 어떻게 사용자에게 안정적으로 전달되는지의 전 과정을 이해하고 책임질 수 있는, 신뢰받는 개발자가 되고 싶습니다.

### `유지은`

- **주요 역할** : 프론트엔드, 뉴스레터 서비스, UI/UX 구현, 개인화 뉴스 추천, 검색기능
- **느낀 점** : 뉴스레터 서비스 백엔드 개발을 통해 마이크로서비스 아키텍처와 Spring Boot 생태계에 대한 깊은 이해를 얻었습니다. 특히 Feign Client를 활용한 서비스간 통신과 개인화 추천 알고리즘 구현 과정에서 실무 수준의 백엔드 개발 역량을 기를 수 있었습니다. 처음에는 단순히 기능 구현에만 집중했지만, 프로젝트가 진행되면서 견고한 아키텍처 설계가 얼마나 중요한지 깨달았습니다. 서비스 계층 분리와 에러 처리, 설정 관리 등을 체계적으로 구현한 덕분에 코드의 유지보수성과 확장성이 크게 향상되었습니다. 개인화 추천 시스템을 구현하면서 단순해 보이는 기능도 실제로는 사용자 행동 분석, 콘텐츠 필터링, 성과 측정 등 여러 요소를 종합적으로 고려해야 함을 배웠습니다. 기술적 구현 능력만큼 비즈니스 요구사항을 정확히 파악하는 것이 중요하다는 점을 실감했습니다. 구독 관리 시스템과 자동 발송 기능 등 핵심 기능들이 미완성된 점이 아쉽지만, 이를 통해 프로젝트 계획과 우선순위 설정의 중요성을 배웠습니다. 기술적 기반은 탄탄하게 구축했으므로 향후 이런 경험을 바탕으로 더 완성도 높은 서비스를 개발할 수 있을 것이라 생각합니다. 다른 서비스들과의 연동 과정에서 API 설계와 문서화의 중요성을 깊이 느꼈고, 코드 리뷰를 통해 더 나은 코드 작성 방법을 학습할 수 있었습니다. 혼자 개발할 때와는 다른 협업의 가치를 경험했습니다.

### `이채희`

- **주요 역할** : 회원 기능, 보안 및 인프라, Config, Gateway, Discovery, Swagger
- **느낀 점** : 이번 파이널 프로젝트에서 회원 기능과 전반적인 인프라를 담당하면서, 전체적인 풀스택 개발의 흐름에 대해서 이해할 수 있었습니다. 그리고 이번에 프로젝트의 전반적인 보안 인프라를 구축하면서, 우리가 개발하는 웹 사이트의 보안성을 어떻게 강화해야 하는지, 어떤 방법이 있는지를 배울 수 있었던 좋은 경험이었습니다. 특히 httpOnly 쿠키 인증 방식을 도입하는 부분과, 소셜 로그인을 구현하는 과정이 쉽지 않았는데, 며칠에 걸쳐 완성하고 나니 이전보다 사용자 친화적으로도, 또 보안적으로도 향상된 웹 사이트를 개발할 수 있었다는 것이 정말 보람차게 다가왔습니다. 프론트엔드와 백엔드 간의 긴밀한 연결, 또 그 과정에서 생기는 오류들을 디버깅하면서 웹 개발에 대한 전반적인 자신감을 얻을 수 있었던 프로젝트였습니다. CI/CD 또한 기간이 얼마 남지 않은 상황에 계속 오류가 생기고, 쉽지 않은 과정 가운데 진행되었지만, 팀원분들의 도움과 완벽한 역할 분배로 일정에 차질없이 모든 개발을 잘 완료했다는 점이 가장 보람된 것 같습니다.
