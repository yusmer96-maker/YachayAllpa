#!/usr/bin/env python3
"""Teselas del mapa de la chacra: relieve NASA (ASTER GDEM) con la paleta de la app.

Baja el sombreado en grises de ASTER GDEM (NASA/METI) desde NASA GIBS para toda la
región de Huánuco, niveles 8 a 11, y lo tiñe por altura con los mismos tonos que
usa la app (MapaRelieve.java), tomando la altura de las teselas Terrarium que ya
viajan en el APK. Resultado: assets/mapa/{z}/{x}_{y}.jpg, para usar sin conexión.

Uso:  python3 -I herramientas/teselas_nasa.py
Las teselas crudas se guardan en datos/gibs/ y no se vuelven a pedir.
"""
import io
import math
import time
import urllib.request
from pathlib import Path

from PIL import Image

RAIZ = Path(__file__).resolve().parent.parent
CRUDO = RAIZ / "datos/gibs"
SALIDA = RAIZ / "app/src/main/assets/mapa"
RELIEVE = RAIZ / "app/src/main/assets/relieve"
URL = ("https://gibs.earthdata.nasa.gov/wmts/epsg3857/best/ASTER_GDEM_Greyscale_Shaded_Relief/"
       "default/GoogleMapsCompatible_Level12/{z}/{y}/{x}.jpg")
NIVELES = range(8, 12)
CAJA = (-8.2, -77.5, -10.7, -74.2)  # norte, oeste, sur, este

COTAS = [0, 800, 1800, 2800, 3600, 4400, 5200]
TONOS = [0xD2E6CC, 0xDCE8C7, 0xE8E4C2, 0xEBD9BB, 0xE2CDB6, 0xEDE6DE, 0xFAF8F5]


def tesela(lat, lon, z):
    n = 2 ** z
    r = math.radians(lat)
    return (int((lon + 180) / 360 * n),
            int((1 - math.log(math.tan(math.pi / 4 + r / 2)) / math.pi) / 2 * n))


_terrarium = {}


def altura(gx9, gy9):
    """Altura en metros en coordenadas globales de píxel a zoom 9 (Terrarium del APK)."""
    tx, ty = int(gx9 // 256), int(gy9 // 256)
    clave = (tx, ty)
    if clave not in _terrarium:
        f = RELIEVE / f"9_{tx}_{ty}.png"
        _terrarium[clave] = Image.open(f).convert("RGB").load() if f.exists() else None
    t = _terrarium[clave]
    if t is None:
        return 2500.0
    r, g, b = t[int(gx9) % 256, int(gy9) % 256]
    return r * 256 + g + b / 256 - 32768


def tinte(e, luz):
    i = 0
    while i < len(COTAS) - 2 and e > COTAS[i + 1]:
        i += 1
    t = max(0.0, min(1.0, (e - COTAS[i]) / (COTAS[i + 1] - COTAS[i])))
    a, b = TONOS[i], TONOS[i + 1]
    canal = lambda s: min(255, int((((a >> s) & 255) * (1 - t) + ((b >> s) & 255) * t) * luz))
    return canal(16), canal(8), canal(0)


def main():
    CRUDO.mkdir(parents=True, exist_ok=True)
    total = bytes_ = pedidas = 0
    for z in NIVELES:
        x0, y0 = tesela(CAJA[0], CAJA[1], z)
        x1, y1 = tesela(CAJA[2], CAJA[3], z)
        (SALIDA / str(z)).mkdir(parents=True, exist_ok=True)
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                cruda = CRUDO / f"{z}_{x}_{y}.jpg"
                if not cruda.exists():
                    with urllib.request.urlopen(URL.format(z=z, x=x, y=y), timeout=60) as r:
                        cruda.write_bytes(r.read())
                    pedidas += 1
                    time.sleep(0.15)  # sin castigar el servicio
                gris = Image.open(cruda).convert("L").load()
                img = Image.new("RGB", (256, 256))
                px = img.load()
                k = 2 ** (9 - z)
                for j in range(256):
                    for i in range(256):
                        e = altura((x * 256 + i) * k, (y * 256 + j) * k)
                        px[i, j] = tinte(e, 0.45 + 0.6 * gris[i, j] / 255)
                buf = io.BytesIO()
                img.save(buf, "JPEG", quality=78, optimize=True)
                (SALIDA / str(z) / f"{x}_{y}.jpg").write_bytes(buf.getvalue())
                total += 1
                bytes_ += buf.tell()
        print(f"z{z}: hecho ({total} teselas, {bytes_ / 1e6:.1f} MB)")
    print(f"listo: {total} teselas, {bytes_ / 1e6:.1f} MB, {pedidas} pedidas a NASA GIBS")


if __name__ == "__main__":
    main()
