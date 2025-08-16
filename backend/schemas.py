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
    user_id: int

class WifiLogCreate(WifiLogBase):
    pass

class WifiLog(WifiLogBase):
    id: int
    start_time: datetime
    end_time: datetime | None = None
    
    class Config:
        from_attributes = True