"""Erzeugt fiktive Beispieldokumente für den Prototyp.

Alle Firmen, Personen und Nummern sind frei erfunden.

    pip install pillow reportlab
    python tools/generate_samples.py
"""
from pathlib import Path
import random

from PIL import Image, ImageDraw, ImageFilter, ImageFont
from reportlab.lib.pagesizes import A4
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

OUT = Path(__file__).resolve().parent.parent / "samples"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

W, H = 1654, 2339  # A4 bei 200 dpi


def scan_effect(img: Image.Image, seed: int) -> Image.Image:
    """Leichte Drehung, Unschärfe und Rauschen – wie ein Büroscanner."""
    rnd = random.Random(seed)
    img = img.rotate(rnd.uniform(-1.2, 1.2), expand=False, fillcolor="white")
    img = img.filter(ImageFilter.GaussianBlur(0.6))
    px = img.load()
    for _ in range(9000):
        x, y = rnd.randrange(W), rnd.randrange(H)
        g = rnd.randrange(150, 235)
        px[x, y] = (g, g, g)
    return img.convert("L")


def form_scan() -> None:
    img = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(img)
    f_title = ImageFont.truetype(FONT_BOLD, 46)
    f_label = ImageFont.truetype(FONT, 30)
    f_value = ImageFont.truetype(FONT, 34)
    f_head = ImageFont.truetype(FONT_BOLD, 32)

    d.text((120, 110), "Prüfauftrag – Kundendaten", font=f_title, fill="black")
    d.text((120, 180), "Bitte vollständig ausfüllen und an das Prüflabor senden.", font=f_label, fill="#333")
    d.line((120, 240, W - 120, 240), fill="black", width=3)

    rows = [
        ("Firma", "Nordlicht Funktechnik GmbH"),
        ("Straße / Nr.", "Hafenstraße 12"),
        ("PLZ / Ort", "45127 Essen"),
        ("Land", "Deutschland"),
        ("USt-IdNr.", "DE 812 345 673"),
        ("Ansprechpartner/in", "Dr. Anna Becker"),
        ("E-Mail", "a.becker@nordlicht-funk.example"),
        ("Telefon", "+49 201 555 0182"),
    ]
    y = 300
    d.text((120, y), "1. Auftraggeber", font=f_head, fill="black")
    y += 70
    for label, value in rows:
        d.text((140, y), label, font=f_label, fill="#222")
        d.text((620, y - 4), value, font=f_value, fill="black")
        d.line((600, y + 44, W - 140, y + 44), fill="#999", width=2)
        y += 90

    y += 30
    d.text((120, y), "2. Prüfgegenstand", font=f_head, fill="black")
    y += 70
    d.text((140, y), "Produkt", font=f_label, fill="#222")
    d.text((620, y - 4), "NLT-Sensor 868 (LoRa-Funkmodul)", font=f_value, fill="black")
    d.line((600, y + 44, W - 140, y + 44), fill="#999", width=2)
    y += 90
    d.text((140, y), "Gewünschte Normen", font=f_label, fill="#222")
    for norm in ["EN 300 220-2", "EN 301 489-3", "EN 62368-1"]:
        d.text((620, y - 4), f"☒  {norm}", font=f_value, fill="black")
        y += 60

    y += 60
    d.text((140, y), "Essen, 14.09.2026", font=f_value, fill="black")
    d.text((1000, y), "A. Becker", font=ImageFont.truetype(FONT, 44), fill="#1a2a6c")
    d.line((980, y + 60, W - 140, y + 60), fill="black", width=2)
    d.text((1000, y + 70), "Unterschrift", font=f_label, fill="#555")

    scan_effect(img, seed=7).save(OUT / "pruefauftrag_scan.png")


def letter_scan() -> None:
    """Unstrukturierte Anfrage per Brief – mit absichtlich falscher Prüfziffer in der USt-IdNr."""
    img = Image.new("RGB", (W, H), "white")
    d = ImageDraw.Draw(img)
    f = ImageFont.truetype(FONT, 32)
    fb = ImageFont.truetype(FONT_BOLD, 34)
    lines = [
        ("Rheinwerk Medizintechnik AG · Königsallee 88 · 40212 Düsseldorf", f),
        ("", f),
        ("Anfrage: Funk- und EMV-Prüfung für BPM-Connect 2", fb),
        ("", f),
        ("Sehr geehrte Damen und Herren,", f),
        ("", f),
        ("wir planen die Markteinführung unseres Bluetooth-Blutdruckmess-", f),
        ("geräts BPM-Connect 2 und benötigen dafür Prüfungen nach", f),
        ("EN 300 328 sowie EN 60601-1-2.", f),
        ("", f),
        ("Bitte richten Sie Rückfragen und das Angebot an Herrn Markus", f),
        ("Olsen (m.olsen@rheinwerk-med.example, Tel. 0211 555 7730).", f),
        ("Für die Rechnungsstellung: USt-IdNr. DE293746105.", f),
        ("", f),
        ("Mit freundlichen Grüßen", f),
        ("", f),
        ("Markus Olsen", f),
        ("Leitung Zulassung", f),
    ]
    y = 160
    for text, font in lines:
        d.text((150, y), text, font=font, fill="black")
        y += 62
    scan_effect(img, seed=11).save(OUT / "anfrage_brief_scan.png")


def form_pdf() -> None:
    """Digital erzeugtes PDF mit Textebene – hier ist kein OCR nötig."""
    pdfmetrics.registerFont(TTFont("DejaVu", FONT))
    pdfmetrics.registerFont(TTFont("DejaVu-Bold", FONT_BOLD))
    c = canvas.Canvas(str(OUT / "pruefauftrag_digital.pdf"), pagesize=A4)
    _, h = A4
    c.setFont("DejaVu-Bold", 16)
    c.drawString(60, h - 70, "Prüfauftrag – Kundendaten")
    c.setFont("DejaVu", 11)
    rows = [
        ("Firma", "Kappa Automotive Systems GmbH"),
        ("Straße / Nr.", "Universitätsstraße 140"),
        ("PLZ / Ort", "44799 Bochum"),
        ("Land", "Deutschland"),
        ("USt-IdNr.", "DE445120871"),
        ("Ansprechpartner/in", "Julia Wendt"),
        ("E-Mail", "julia.wendt@kappa-automotive.example"),
        ("Telefon", "+49 234 555 4410"),
        ("Produkt", "KAS Keyless Entry Modul 433 MHz"),
        ("Gewünschte Normen", "EN 300 220-2, EN 301 489-3"),
    ]
    y = h - 120
    for label, value in rows:
        c.drawString(60, y, label)
        c.drawString(220, y, value)
        y -= 26
    c.save()


if __name__ == "__main__":
    OUT.mkdir(exist_ok=True)
    form_scan()
    letter_scan()
    form_pdf()
    print("Beispieldokumente erzeugt in", OUT)
