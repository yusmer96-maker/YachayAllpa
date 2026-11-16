#!/usr/bin/env python3
"""Limpia un dibujo de cultivo con el fondo mal recortado y lo deja como los demás.

Problemas que corrige: halo semitransparente alrededor de la planta (restos del
fondo) y filos de colores puros (rojo, amarillo, verde) en el borde.

1. Se queda con lo opaco (alfa >= 128) y descarta las motas pequeñas.
2. Descontamina el borde: la franja exterior de 3 px toma el color de la planta
   que tiene justo dentro (así se van los filos de colores).
3. Suaviza el borde (desenfoque de 0,8 px en el alfa) y lo encuadra como los
   demás: cuadrado, transparente, margen del 6 %, 360 px.

Uso:  python3 -I herramientas/limpiar_dibujo.py ORIGEN.png cultivo_nombre.png
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage

RES = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable-nodpi"
LADO = 360


def limpiar(origen: Path, destino: Path):
    rgba = np.array(Image.open(origen).convert("RGBA")).astype(np.float32)
    alfa = rgba[:, :, 3]
    mascara = alfa >= 128
    # Fuera motas sueltas: solo piezas de al menos 0,05 % de la imagen.
    etiquetas, n = ndimage.label(mascara, structure=np.ones((3, 3)))
    tam = ndimage.sum(mascara, etiquetas, range(1, n + 1))
    minimo = mascara.size * 0.0005
    mascara = np.isin(etiquetas, [i + 1 for i, t in enumerate(tam) if t >= minimo])
    # Interior seguro: a más de 3 px del borde.
    dentro = ndimage.distance_transform_edt(mascara) > 3
    _, (iy, ix) = ndimage.distance_transform_edt(~dentro, return_indices=True)
    rgb = rgba[:, :, :3].copy()
    borde = mascara & ~dentro
    rgb[borde] = rgba[iy[borde], ix[borde], :3]
    nuevo_alfa = np.where(mascara, 255, 0).astype(np.uint8)
    alfa_img = Image.fromarray(nuevo_alfa).filter(ImageFilter.GaussianBlur(0.8))
    out = np.dstack([rgb.clip(0, 255).astype(np.uint8), np.array(alfa_img)])
    ys, xs = np.where(mascara)
    pieza = Image.fromarray(out[ys.min():ys.max() + 1, xs.min():xs.max() + 1], "RGBA")
    lado = int(max(pieza.size) * 1.12)
    lienzo = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
    lienzo.paste(pieza, ((lado - pieza.width) // 2, (lado - pieza.height) // 2))
    lienzo.resize((LADO, LADO), Image.LANCZOS).save(destino, optimize=True)
    print(f"{origen.name} -> {destino.name}: {pieza.width}x{pieza.height}, "
          f"{destino.stat().st_size // 1024} KB")


if __name__ == "__main__":
    limpiar(Path(sys.argv[1]), RES / sys.argv[2])
