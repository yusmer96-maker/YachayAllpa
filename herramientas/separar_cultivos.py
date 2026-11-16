#!/usr/bin/env python3
"""Separa las láminas botánicas en un PNG limpio por cultivo.

Las láminas tienen varios dibujos en fila y algunos se invaden entre sí (las hojas
del trigo entran en la casilla del maní). Cortar en partes iguales arrastra trozos
del vecino. Aquí cada mancha conectada del dibujo se asigna al cultivo cuya casilla
tiene más cerca su centro; el resto se borra. Resultado:
app/src/main/res/drawable-nodpi/cultivo_<nombre>.png (cuadrado, transparente).

Uso:  python3 -I herramientas/separar_cultivos.py
"""
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

RAIZ = Path(__file__).resolve().parent.parent
# Las laminas de origen viven fuera del APK: solo entran los cultivo_*.png recortados.
LAMINAS_DIR = RAIZ / "datos/laminas-origen"
RES = RAIZ / "app/src/main/res/drawable-nodpi"
LADO = 360  # píxeles del PNG final

# lámina -> (columnas, filas, cultivos en orden de lectura)
LAMINAS = {
    "cultivos_transparentes.png": (5, 2, ["papa", "quinua", "oca", "olluco", "maiz",
                                          "cebada", "habas", "tarwi", "arveja", "descanso"]),
    "cultivos_bajos_lamina.png": (3, 1, ["maiz_amarillo", "frijol", "yuca"]),
    "tuberculos_curvos_transparentes.png": (4, 1, ["mashua", "camote", "yacon", "arracacha"]),
    "cultivos_extra_transparentes.png": (5, 1, ["pallar", "trigo", "mani", "kiwicha", "calabaza"]),
}


def separar(nombre, columnas, filas, cultivos):
    img = Image.open(LAMINAS_DIR / nombre).convert("RGBA")
    rgba = np.array(img)
    alfa = rgba[:, :, 3]
    h, w = alfa.shape
    etiquetas, n = ndimage.label(alfa > 16, structure=np.ones((3, 3)))
    centros = ndimage.center_of_mass(alfa > 16, etiquetas, range(1, n + 1))
    cw, ch = w / columnas, h / filas
    duenio = np.zeros(n + 1, dtype=int) - 1
    for k, (cy, cx) in enumerate(centros, start=1):
        col = min(columnas - 1, int(cx // cw))
        fil = min(filas - 1, int(cy // ch))
        duenio[k] = fil * columnas + col
    for i, cultivo in enumerate(cultivos):
        mascara = np.isin(etiquetas, np.where(duenio == i)[0])
        if not mascara.any():
            print(f"  {cultivo}: sin dibujo"); continue
        ys, xs = np.where(mascara)
        y0, y1, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
        recorte = rgba[y0:y1 + 1, x0:x1 + 1].copy()
        recorte[~mascara[y0:y1 + 1, x0:x1 + 1]] = 0   # fuera lo que es del vecino
        pieza = Image.fromarray(recorte, "RGBA")
        # Cuadrado con un margen del 6 % y el dibujo centrado.
        lado = int(max(pieza.size) * 1.12)
        lienzo = Image.new("RGBA", (lado, lado), (0, 0, 0, 0))
        lienzo.paste(pieza, ((lado - pieza.width) // 2, (lado - pieza.height) // 2))
        lienzo = lienzo.resize((LADO, LADO), Image.LANCZOS)
        destino = RES / f"cultivo_{cultivo}.png"
        lienzo.save(destino, optimize=True)
        print(f"  {cultivo}: {pieza.width}x{pieza.height} -> {destino.name} "
              f"({destino.stat().st_size // 1024} KB)")


def main():
    for nombre, (c, f, cultivos) in LAMINAS.items():
        print(nombre)
        separar(nombre, c, f, cultivos)


if __name__ == "__main__":
    main()
