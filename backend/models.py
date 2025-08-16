from sqlalchemy import Column, DateTime, Integer, String, ForeignKey
from sqlalchemy.orm import relationship
from sqlalchemy.sql import func  
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    user_name = Column(String(50), unique=True, index=True, nullable=False)
    home_ssid = Column(String(50), nullable=False)
    home_bssid = Column(String(50), nullable=False)
    logs = relationship("WifiLog", back_populates="user")

class WifiLog(Base):
    __tablename__ = "wifi_logs"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    start_time = Column(DateTime, server_default=func.now(), nullable=False)
    end_time = Column(DateTime, nullable=True)

    user = relationship("User", back_populates="logs")