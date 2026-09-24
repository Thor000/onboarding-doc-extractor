"""Texterkennung für Bilder und PDFs.

Digitale PDFs haben bereits eine Textebene – die wird direkt gelesen.
Nur gescannte Seiten (Bilder, PDFs ohne Text) laufen durch Tesseract.
"""
from dataclasses import dataclass, field
import io
import os

import pypdfium2 as pdfium
import pytesseract
from PIL import Image, ImageOps

OCR_LANG = os.getenv("OCR_LANG", "deu+eng")
PDF_RENDER_DPI = 300
# Unter dieser Zeichenzahl gilt eine PDF-Seite als "gescannt" (keine brauchbare Textebene).
MIN_TEXT_LAYER_CHARS = 30


class UnsupportedDocumentError(ValueError):
    pass


@dataclass
class PageResult:
    number: int
    method: str  # "text-layer" oder "ocr"
    text: str
    confidence: float | None = None  # mittlere Tesseract-Konfidenz 0–100, nur bei OCR


@dataclass
class OcrResult:
    pages: list[PageResult] = field(default_factory=list)

    @property
    def text(self) -> str:
        return "\n\n".join(p.text for p in self.pages).strip()

    @property
    def mean_confidence(self) -> float | None:
        values = [p.confidence for p in self.pages if p.confidence is not None]
        return round(sum(values) / len(values), 1) if values else None


def preprocess(image: Image.Image) -> Image.Image:
    """Graustufen + Kontrastausgleich; kleine Bilder werden hochskaliert."""
    image = ImageOps.exif_transpose(image).convert("L")
    image = ImageOps.autocontrast(image)
    if image.width < 1500:
        factor = 1500 / image.width
        image = image.resize((int(image.width * factor), int(image.height * factor)), Image.LANCZOS)
    return image


def ocr_image(image: Image.Image, number: int = 1) -> PageResult:
    image = preprocess(image)
    text = pytesseract.image_to_string(image, lang=OCR_LANG)
    data = pytesseract.image_to_data(image, lang=OCR_LANG, output_type=pytesseract.Output.DICT)
    confidences = [float(c) for c in data["conf"] if float(c) >= 0]
    confidence = round(sum(confidences) / len(confidences), 1) if confidences else None
    return PageResult(number=number, method="ocr", text=text.strip(), confidence=confidence)


def process_pdf(content: bytes) -> OcrResult:
    result = OcrResult()
    pdf = pdfium.PdfDocument(content)
    try:
        for index in range(len(pdf)):
            page = pdf[index]
            text_page = page.get_textpage()
            text = text_page.get_text_range().strip()
            text_page.close()
            if len(text) >= MIN_TEXT_LAYER_CHARS:
                result.pages.append(PageResult(number=index + 1, method="text-layer", text=text))
            else:
                bitmap = page.render(scale=PDF_RENDER_DPI / 72)
                result.pages.append(ocr_image(bitmap.to_pil(), number=index + 1))
            page.close()
    finally:
        pdf.close()
    return result


def process_document(content: bytes, filename: str, content_type: str | None) -> OcrResult:
    name = (filename or "").lower()
    if name.endswith(".pdf") or content_type == "application/pdf" or content.startswith(b"%PDF"):
        return process_pdf(content)
    try:
        image = Image.open(io.BytesIO(content))
        image.load()
    except Exception as exc:  # Pillow wirft je nach Format unterschiedliche Fehler
        raise UnsupportedDocumentError("Datei ist weder PDF noch ein lesbares Bild") from exc
    return OcrResult(pages=[ocr_image(image)])
