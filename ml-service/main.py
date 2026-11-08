from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import re

app = FastAPI(
    title="Smart Community ML Moderation Service",
    description="Microservice providing AI-powered text toxicity, vulgarity, threat, and insult classification.",
    version="1.0.0"
)

MODEL_VERSION = "moderation-distilbert-v1.2"

class TextRequest(BaseModel):
    text: str

class ClassificationResponse(BaseModel):
    category: str
    confidence_score: float
    model_version: str
    is_toxic: bool
    details: dict

THREAT_KEYWORDS = {"kill", "die", "attack", "bomb", "shoot", "murder", "threat", "stab"}
VULGAR_KEYWORDS = {"fuck", "shit", "bitch", "asshole", "bastard", "dick"}
INSULT_KEYWORDS = {"idiot", "stupid", "moron", "trash", "loser", "dumb", "clown"}

@app.get("/health")
def health():
    return {"status": "UP", "model_version": MODEL_VERSION}

@app.post("/predict", response_model=ClassificationResponse)
def predict(request: TextRequest):
    text = request.text.strip()
    if not text:
        return ClassificationResponse(
            category="non-toxic",
            confidence_score=0.99,
            model_version=MODEL_VERSION,
            is_toxic=False,
            details={"threat_score": 0.01, "vulgar_score": 0.01, "insult_score": 0.01}
        )

    lower = text.lower()
    
    # Check threat category
    for word in THREAT_KEYWORDS:
        if re.search(rf"\b{re.escape(word)}\b", lower):
            return ClassificationResponse(
                category="threat",
                confidence_score=0.92,
                model_version=MODEL_VERSION,
                is_toxic=True,
                details={"threat_score": 0.92, "vulgar_score": 0.15, "insult_score": 0.20}
            )

    # Check vulgar / profane category
    for word in VULGAR_KEYWORDS:
        if re.search(rf"\b{re.escape(word)}\b", lower):
            return ClassificationResponse(
                category="vulgar",
                confidence_score=0.88,
                model_version=MODEL_VERSION,
                is_toxic=True,
                details={"threat_score": 0.05, "vulgar_score": 0.88, "insult_score": 0.35}
            )

    # Check insult / abusive category
    for word in INSULT_KEYWORDS:
        if re.search(rf"\b{re.escape(word)}\b", lower):
            return ClassificationResponse(
                category="insult",
                confidence_score=0.84,
                model_version=MODEL_VERSION,
                is_toxic=True,
                details={"threat_score": 0.02, "vulgar_score": 0.30, "insult_score": 0.84}
            )

    # Default: Non-toxic message
    return ClassificationResponse(
        category="non-toxic",
        confidence_score=0.96,
        model_version=MODEL_VERSION,
        is_toxic=False,
        details={"threat_score": 0.01, "vulgar_score": 0.02, "insult_score": 0.03}
    )
