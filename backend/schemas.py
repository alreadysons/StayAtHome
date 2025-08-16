from datetime import datetime
from pydantic import BaseModel

# User 관련 스키마
class UserBase(BaseModel):
    user_name: str
    home_ssid: str
    home_bssid: str

class UserCreate(UserBase):
    pass

class User(UserBase):
    id: int

    class Config:
        from_attributes = True

# WifiLog 관련 스키마
class WifiLogBase(BaseModel):
    bssid: str
    ssid: str
    start_time: datetime | None = None
    end_time: datetime | None = None

class WifiLog(WifiLogBase):
    id: int
    user_id: int
    start_time: datetime
    end_time: datetime | None = None
    
    class Config:
        from_attributes = True