from fastapi import FastAPI
import models 
from database import SessionLocal, engine
from router.user import user
from router.wifi_log import wifi_log
from router.statistics import statistics_router


app = FastAPI()

app.include_router(wifi_log)
app.include_router(user)
app.include_router(statistics_router)

models.Base.metadata.create_all(bind=engine)

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
