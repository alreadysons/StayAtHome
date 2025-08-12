from datetime import datetime
from pydantic import BaseModel, EmailStr

# User 관련 스키마
class UserBase(BaseModel):
    user_name: str
    home_wifi_bssid: str | None = None

class User(UserBase):
    id: int
    user_name: str

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
    
    class Config:
        from_attributes = True