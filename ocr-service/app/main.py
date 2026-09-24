"""OCR-Microservice: nimmt ein Dokument entgegen und liefert den erkannten Text als JSON."""
import logging
import time

from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel

from .ocr import OCR_LANG, UnsupportedDocumentError, process_document

MAX_UPLOAD_BYTES = 20 * 1024 * 1024

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger("ocr-service")

app = FastAPI(title="OCR Service", version="1.0.0")


class Page(BaseModel):
    number: int
    method: str
    confidence: float | None
    characters: int


class OcrResponse(BaseModel):
    filename: str
    text: str
    pages: list[Page]
    meanConfidence: float | None
    durationMs: int


@app.get("/health")
def health() -> dict:
    return {"status": "UP", "language": OCR_LANG}


@app.post("/ocr", response_model=OcrResponse)
async def ocr(file: UploadFile = File(...)) -> OcrResponse:
    content = await file.read()
    if not content:
        raise HTTPException(status_code=400, detail="Leere Datei")
    if len(content) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="Datei größer als 20 MB")

    started = time.perf_counter()
    try:
        result = process_document(content, file.filename or "", file.content_type)
    except UnsupportedDocumentError as exc:
        raise HTTPException(status_code=415, detail=str(exc)) from exc
    duration_ms = int((time.perf_counter() - started) * 1000)

    log.info("%s: %d Seite(n), %d ms, Konfidenz %s",
             file.filename, len(result.pages), duration_ms, result.mean_confidence)
    return OcrResponse(
        filename=file.filename or "",
        text=result.text,
        pages=[Page(number=p.number, method=p.method, confidence=p.confidence, characters=len(p.text))
               for p in result.pages],
        meanConfidence=result.mean_confidence,
        durationMs=duration_ms,
    )
