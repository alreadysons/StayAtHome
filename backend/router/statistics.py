from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from api import statistics_api
from typing import Dict

statistics_router = APIRouter(prefix="/statistics", tags=["statistics"])

@statistics_router.get("/weekly/{user_id}")
def get_weekly_statistics(user_id: int, db: Session = Depends(get_db)) -> Dict:
    try:
        return statistics_api.get_weekly_statistics(db=db, user_id=user_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))