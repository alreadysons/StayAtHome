from fastapi import Depends, FastAPI, HTTPException
app = FastAPI()

@app.get("/")
def read_root():
    return {"message": "StayAtHome Backend"}
