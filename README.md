# StayAtHome

집 Wi‑Fi 연결 여부로 체류 시간을 자동 기록하고 주간 통계를 시각화하는 프로젝트입니다. Android 앱(Compose)과 FastAPI 백엔드로 구성됩니다.

## 🌟 주요 기능

- Wi‑Fi 기반 위치 감지: 등록한 집 SSID/BSSID와 현재 연결 정보를 비교하여 “집” 판정
- 자동 시간 기록: 집 Wi‑Fi 연결/해제 시 서버에 로그 시작/종료 기록
- 주간 통계: 최근 주간(월–일) 일별 체류시간, 합계/평균 계산
- 시각화: 앱 내 간단 라인 차트로 일별 체류시간 표시

## 🛠️ 기술 스택

- Backend: FastAPI, SQLAlchemy, MySQL(mysqlclient), Uvicorn
- Android: Kotlin, Jetpack Compose, ViewModel, Retrofit2, DataStore, WorkManager

## 📂 프로젝트 구조

```
.
├── app/                     # Android 앱 (Compose)
├── backend/                 # FastAPI 백엔드
│   ├── api/                 # DB 액션 로직(user/log/statistics)
│   ├── router/              # FastAPI 라우터
│   ├── models.py            # SQLAlchemy 모델(User, WifiLog)
│   ├── schemas.py           # Pydantic 스키마
│   ├── database.py          # DB 세션/엔진(DATABASE_URL 사용)
│   ├── main.py              # FastAPI 엔트리포인트
│   └── requirements.txt
├── docker-compose.yml       # 백엔드+DB 로컬 실행
└── backend/Dockerfile       # 백엔드 컨테이너 이미지
```

## 🚀 실행 방법

### 1) Docker Compose (권장: 로컬 개발)

- 요구사항: Docker, Docker Compose
- 실행
  - `docker compose up -d --build`
  - 백엔드: http://localhost:8000, 문서: http://localhost:8000/docs
- 중지/정리
  - `docker compose down` (데이터 유지)
  - `docker compose down -v` (DB 볼륨까지 삭제)

구성 요약
- DB: MySQL 8.0 (DB: `stayathome`, 계정: `stay/staypass`)
- 백엔드 환경변수: `DATABASE_URL=mysql+mysqldb://stay:staypass@db:3306/stayathome?charset=utf8mb4`

### 2) 직접 실행 (백엔드만)

1) 의존성 설치
```
cd backend
python -m venv venv && source venv/bin/activate
pip install -r requirements.txt
```
2) 환경변수 설정 (`.env` 또는 셸에서 지정)
```
DATABASE_URL=mysql+mysqldb://<USER>:<PASS>@<HOST>:<PORT>/<DB>?charset=utf8mb4
```
3) 서버 기동
```
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

## 📡 백엔드 API 개요

- User
  - POST `/user/create`: 집 SSID/BSSID 등록(신규 사용자 생성)
  - PUT `/user/{user_id}/home_wifi`: 집 SSID/BSSID 변경
  - DELETE `/user/delete/{user_id}`: 사용자 및 관련 로그 삭제
  - GET `/user/id/{user_id}`: 사용자 조회
  - GET `/user/list?start=&last=`: 사용자 목록 페이지네이션
- Log
  - POST `/log/start`: 열려 있는 로그 없으면 시작(한국시간)
  - POST `/log/end?log_id=`: 로그 종료
  - GET `/log/list?start=&last=`: 로그 목록 페이지네이션
- Statistics
  - GET `/statistics/weekly/{user_id}`: 이번 주(월–일) 일별 체류시간, 합계/평균

참고
- 시간대: 서버는 한국시간(KST, UTC+9)을 사용하도록 구현되어 있습니다.
- 향후 개선: 자정 경계에 걸친 로그를 일별로 분할 집계, `log/end`를 PathParam 형태로 변경 권장

## 🤖 Android 앱

1) 열기: Android Studio로 `app/` 프로젝트를 엽니다.
2) 서버 주소 설정: `RetrofitClient.kt`의 `BASE_URL`을 환경에 맞게 수정합니다.
   - 에뮬레이터: `http://10.0.2.2:8000/`
   - 실제 기기: PC와 동일 네트워크의 PC IP 사용(예: `http://192.168.x.x:8000/`)
3) 권한
   - Manifest: `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`, `INTERNET`
   - Android 13+(targetSdk 33+)은 `NEARBY_WIFI_DEVICES` 추가 필요(런타임 권한 요청 포함)
4) 동작 요약
   - 홈: 현재 SSID/BSSID 표시, “등록”으로 집 Wi‑Fi 저장/갱신
   - 통계: 주간 체류시간 차트/요약
   - 설정: 현재 접속 정보 새로고침, 사용자 삭제
   - 백그라운드: WorkManager가 주기적으로 연결 상태를 확인하여 로그 시작/종료

의존성 참고
- Retrofit은 2.x 사용 권장(예: `2.11.0`), 현재 샘플 코드는 버전 정리가 필요할 수 있습니다.

## 🔧 개발 메모 / TODO

- 백엔드
  - CORS 설정(CORSMiddleware) 추가(개발용)
  - 로그가 자정을 넘는 경우 일별 분할 집계
  - 엔드포인트 명확화(`/log/{log_id}/end`, `/log/user/{user_id}` 등)
  - Alembic 마이그레이션 및 인덱스 최적화(`wifi_logs(user_id, start_time)`) 
- 안드로이드
  - Android 13+ 권한 분기 적용, 네트워크 콜백 등록로 즉시 반응
  - Retrofit/OkHttp 로깅 인터셉터 추가, 에러 메시지 표준화
  - Compose UI 컴포넌트 파일 분리 및 아이콘 적용(Material Icons)

---
StayAtHome — 집에 머문 시간을 편하게 기록하고 확인하세요.
