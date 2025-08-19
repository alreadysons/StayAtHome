from sqlalchemy.orm import Session
import models
from datetime import datetime, timedelta, timezone
from typing import Dict

# 한국 시간대 설정
KST = timezone(timedelta(hours=9))

def get_weekly_statistics(db: Session, user_id: int) -> Dict:
    # 오늘 날짜
    today = datetime.now(KST).date()
    
    # 이번 주의 월요일 찾기
    monday = today - timedelta(days=today.weekday())
    sunday = monday + timedelta(days=6)
    
    # 해당 사용자의 이번 주 로그 조회
    logs = db.query(models.WifiLog).filter(
        models.WifiLog.user_id == user_id,
        models.WifiLog.start_time >= datetime.combine(monday, datetime.min.time()).replace(tzinfo=KST),
        models.WifiLog.start_time <= datetime.combine(sunday, datetime.max.time()).replace(tzinfo=KST)
    ).all()
    
    # 일별 체류 시간 계산 (월요일부터 일요일까지)
    daily_hours = {}
    for i in range(7):
        current_date = (monday + timedelta(days=i)).strftime('%Y-%m-%d')
        daily_hours[current_date] = 0
    
    current_time = datetime.now(KST)
    
    # 각 로그별 체류 시간 계산
    for log in logs:
        log_date = log.start_time.strftime('%Y-%m-%d')
        
        # start_time에 timezone 정보 추가
        start_time = log.start_time.replace(tzinfo=KST) if log.start_time.tzinfo is None else log.start_time
        
        # end_time이 있는 경우 timezone 정보 추가
        if log.end_time:
            end_time = log.end_time.replace(tzinfo=KST) if log.end_time.tzinfo is None else log.end_time
        else:
            end_time = current_time
            
        time_long = (end_time - start_time).total_seconds() / 3600
        
        # 24시간을 넘지 않도록 보정
        time_long = min(time_long, 24.0)
        daily_hours[log_date] = round(time_long + daily_hours[log_date], 1) 
    
    # 주간 평균 계산
    weekly_total = sum(daily_hours.values())
    weekly_average = weekly_total / 7
    
    return {
        "week_start": monday.strftime('%Y-%m-%d'),
        "week_end": sunday.strftime('%Y-%m-%d'),
        "daily_hours": daily_hours,
        "weekly_total": round(weekly_total, 1),
        "weekly_average": round(weekly_average, 1)
    }