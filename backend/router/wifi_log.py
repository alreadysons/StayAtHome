from fastapi import APIRouter
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List

from database import get_db
import schemas
from api import log_api

wifi_log = APIRouter(prefix="/log", tags=["log"])


@wifi_log.post("/start", response_model=schemas.WifiLog)
def start_log(wifi_log: schemas.WifiLogCreate, db: Session = Depends(get_db)):
    return log_api.start_log(db=db, wifi_log=wifi_log)

@wifi_log.get("/id/{user_id}", response_model=schemas.WifiLog)
def read_log_by_id(user_id: int, db: Session = Depends(get_db)):
    db_log = log_api.get_log_by_id(db, user_id=user_id)
    if db_log is None:
        raise HTTPException(status_code=404, detail="Log not found")
    return db_log


@wifi_log.get("/list", response_model=List[schemas.WifiLog])
def read_logs(start: int = 0, last: int = 10, db: Session = Depends(get_db)):
    logs = log_api.get_all_logs(db, start=start, last=last)
    return logs