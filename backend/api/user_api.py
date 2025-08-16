from sqlalchemy.orm import Session
import models, schemas

# 새로운 사용자를 생성
def create_user(db: Session, user: schemas.UserCreate):
    db_user = models.User(**user.model_dump())
    db.add(db_user)
    db.commit()    
    db.refresh(db_user)
    return db_user

# 전체 사용자 목록
def get_all_users(db: Session, start: int = 0, last: int = 10):
    return db.query(models.User).offset(start).limit(last).all()

# 사용자 id로 사용자 반환
def get_user_by_id(db: Session, user_id: int):
    return db.query(models.User).filter(models.User.id == user_id).first()

# 사용자 id로 사용자 반환
def get_user_by_name(db: Session, user_name: str):
    return db.query(models.User).filter(models.User.user_name == user_name).first()