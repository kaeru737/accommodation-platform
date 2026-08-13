# Accommodation Platform

## 프로젝트 소개
숙박 공간 등록, 예약, 리뷰, 찜, 채팅 기능을 포함한 Spring Boot 기반 숙박 예약 웹 서비스입니다.

## 주요 기능
- 회원가입 및 로그인
- 숙소 등록/조회/수정
- 예약 가능 날짜 관리
- 숙소 예약 및 예약 내역 조회
- 리뷰 작성 및 조회
- 찜 기능
- 사용자 간 채팅 기능

## 사용 기술
- Java
- Spring Boot
- Spring Data JPA
- Maven
- MySQL
- Thymeleaf
- WebSocket

## 프로젝트 구조
src/main/java/com/rs/acd
├── chat
├── config
├── controller
├── domain
├── dto
├── repository
├── service
└── web

## 실행 방법
1. 저장소 클론
2. application-example.properties를 참고해 application.properties 생성
3. DB 설정 입력
4. mvnw.cmd spring-boot:run 실행

## 담당/학습 내용
- Controller-Service-Repository 계층 구조 이해
- 숙소/예약/리뷰/찜 데이터 흐름 구현
- Spring Boot와 MySQL 연동
- WebSocket 기반 채팅 기능 구현
- 웹 서비스 기능 흐름 설계 경험

## 개선하고 싶은 점
- 예외 처리 보완
- UI 정리
- 이미지 업로드 구조 개선
- 예약 중복 방지 로직 강화
- 배포 환경 구성