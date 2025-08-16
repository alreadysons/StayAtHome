from sqlalchemy.orm import Session
import models, schemas
from datetime import datetime

# 새로운 로그 생성
def start_log(db: Session, wifi_log: schemas.WifiLogCreate):
    db_wifi_log = models.WifiLog(
        user_id=wifi_log.user_id,
        start_time=datetime.now()
    )
    db.add(db_wifi_log)
    db.commit()    
    db.refresh(db_wifi_log)
    return db_wifi_log

# 전체 사용자 목록
def get_all_logs(db: Session, start: int = 0, last: int = 10):
    return db.query(models.WifiLog).offset(start).limit(last).all()

# 사용자 id로 사용자 반환
def get_log_by_id(db: Session, user_id: int):
    return db.query(models.WifiLog).filter(models.WifiLog.user_id == user_id).first()

def end_log(db: Session, log_id: int):
    db_log = db.query(models.WifiLog).filter(models.WifiLog.id == log_id).first()
    if db_log is None:
        return None
    
    db_log.end_time = datetime.now()
    db.commit()
    db.refresh(db_log)
    return db_log