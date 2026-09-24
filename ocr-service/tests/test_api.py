from pathlib import Path

from fastapi.testclient import TestClient

from app.main import app

SAMPLES = Path(__file__).resolve().parents[2] / "samples"
client = TestClient(app)


def upload(name: str, content_type: str):
    with open(SAMPLES / name, "rb") as fh:
        return client.post("/ocr", files={"file": (name, fh, content_type)})


def test_health():
    assert client.get("/health").json()["status"] == "UP"


def test_scanned_image_is_ocred():
    response = upload("pruefauftrag_scan.png", "image/png")
    assert response.status_code == 200
    body = response.json()
    assert body["pages"][0]["method"] == "ocr"
    assert body["meanConfidence"] > 60
    assert "Nordlicht Funktechnik GmbH" in body["text"]
    assert "a.becker@nordlicht-funk.example" in body["text"]


def test_digital_pdf_uses_text_layer():
    response = upload("pruefauftrag_digital.pdf", "application/pdf")
    assert response.status_code == 200
    body = response.json()
    assert body["pages"][0]["method"] == "text-layer"
    assert "Kappa Automotive Systems GmbH" in body["text"]
    assert "DE445120871" in body["text"]


def test_rejects_non_document():
    response = client.post("/ocr", files={"file": ("notes.txt", b"hello", "text/plain")})
    assert response.status_code == 415


def test_rejects_empty_file():
    response = client.post("/ocr", files={"file": ("empty.png", b"", "image/png")})
    assert response.status_code == 400


def test_scanned_pdf_without_text_layer_falls_back_to_ocr(tmp_path):
    from PIL import Image
    pdf_path = tmp_path / "scan.pdf"
    Image.open(SAMPLES / "anfrage_brief_scan.png").convert("RGB").save(pdf_path, resolution=200)
    with open(pdf_path, "rb") as fh:
        response = client.post("/ocr", files={"file": ("scan.pdf", fh, "application/pdf")})
    assert response.status_code == 200
    body = response.json()
    assert body["pages"][0]["method"] == "ocr"
    assert "m.olsen@rheinwerk-med.example" in body["text"]
