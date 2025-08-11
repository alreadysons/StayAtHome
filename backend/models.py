from sqlalchemy import Column, Integer, String, DateTime, ForeignKey
from sqlalchemy.orm import relationship
from .database import Base
import datetime

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    home_wifi_bssid = Column(String, nullable=True) # 사용자의 집 와이파이 BSSID
    logs = relationship("WifiLog", back_populates="user")

class WifiLog(Base):
    __tablename__ = "wifi_logs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    bssid = Column(String, index=True, nullable=False)
    start_time = Column(DateTime, default=datetime.datetime.utcnow, nullable=False)
    end_time = Column(DateTime, nullable=True)

    user = relationship("User", back_populates="logs")
