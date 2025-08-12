from fastapi import FastAPI
import models 
from database import SessionLocal, engine
from router.user import user
from router.wifi_log import wifi_log


app = FastAPI()

app.include_router(wifi_log)
app.include_router(user)

models.Base.metadata.create_all(bind=engine)

app = FastAPI()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

@app.get("/")
def home():
    return {"message": "Welcome to the StayAtHome API"}

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
