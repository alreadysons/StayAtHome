from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from typing import List

from database import get_db
import schemas
from api import user_api

user = APIRouter(prefix="/user", tags=["user"])

@user.post("/create", response_model=schemas.User)
def create_user(user: schemas.UserCreate, db: Session = Depends(get_db)):
    db_user = user_api.get_user_by_name(db, user.user_name)
    if db_user:
        raise HTTPException(status_code=400, detail="Username already registered")
    return user_api.create_user(db=db, user=user)

@user.get("/name/{user_name}", response_model=schemas.User)
def read_user_by_name(user_name: str, db: Session = Depends(get_db)):
    db_user = user_api.get_user_by_name(db, user_name=user_name)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return db_user

@user.get("/id/{user_id}", response_model=schemas.User)
def read_user_by_id(user_id: int, db: Session = Depends(get_db)):
    db_user = user_api.get_user_by_id(db, user_id=user_id)
    if db_user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return db_user

@user.get("/list", response_model=List[schemas.User])
def read_users(start: int = 0, last: int = 10, db: Session = Depends(get_db)):
    users = user_api.get_all_users(db, start=start, last=last)
    return users

