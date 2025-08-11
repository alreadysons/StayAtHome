# StayAtHome

Wi-Fi 접속을 기반으로 사용자의 주간 가내 체류 시간을 분석하고 시각화해주는 애플리케이션입니다.

## 🌟 주요 기능

- **Wi-Fi 기반 위치 감지**: 사용자가 사전에 등록한 집 Wi-Fi 정보를 바탕으로 집에 있는지 여부를 자동으로 감지합니다.
- **자동 시간 기록**: 집 Wi-Fi에 연결되거나 연결이 끊어지는 시간을 감지하여 서버에 자동으로 기록합니다.
- **주간 통계 분석**: 기록된 데이터를 바탕으로 최근 7일간의 일별 재택 시간을 계산하고 통계를 제공합니다.
- **시각화**: 분석된 통계 데이터를 앱 내에서 차트(그래프) 형태로 시각화하여 보여줍니다.

## 🛠️ 기술 스택

- **Backend**: FastAPI, SQLAlchemy, PostgreSQL
- **Android**: Kotlin, Retrofit2, MPAndroidChart (예정)
- **Server**: Uvicorn

## 📂 프로젝트 구조

```
.
├── andriod/      # Android App 소스 코드
└── backend/      # FastAPI Backend 소스 코드
```

## 🚀 시작하기

### Backend

1.  **가상 환경 설정 및 라이브러리 설치**
    ```bash
    cd backend
    python -m venv venv
    source venv/bin/activate
    pip install -r requirements.txt
    ```

2.  **데이터베이스 설정**
    `backend/database.py` 파일의 `SQLALCHEMY_DATABASE_URL`을 사용하는 PostgreSQL 정보에 맞게 수정해야 합니다.
    ```python
    # backend/database.py
    SQLALCHEMY_DATABASE_URL = "postgresql://<user>:<password>@<host>:<port>/<database>"
    ```

3.  **서버 실행**
    `backend` 디렉토리에서 다음 명령어를 실행합니다.
    ```bash
    uvicorn main:app --reload
    ```
    서버는 `http://127.0.0.1:8000`에서 실행됩니다.

### Android

1.  Android Studio에서 `andriod` 폴더를 엽니다.
2.  필요한 Gradle 종속성이 동기화될 때까지 기다립니다.
3.  백엔드 서버 주소를 안드로이드 코드 내의 API 클라이언트 설정에 맞게 수정합니다. (예: `http://10.0.2.2:8000` for emulator)
4.  앱을 빌드하고 에뮬레이터나 실제 기기에서 실행합니다.

---
*Wifi 기반 가내 체류시간 분석 App*