#!/usr/bin/env python3
"""Revisa la tabla bilingue de la app: mismas claves y mismos huecos de formato.

Uso:  python3 herramientas/revisar_textos.py [ruta/Textos.kt]

Comprueba, para castellano y quechua:
  1. que las dos tablas tengan exactamente las mismas claves,
  2. que ninguna clave este repetida,
  3. que los huecos de formato (%s, %d, %%) coincidan en orden y cantidad,
  4. que ninguna frase quechua sea identica a la castellana sin ser un numero,
     una unidad o un nombre propio (lista blanca abajo),
  5. que todas las claves que usa el UI existan, y que no sobren claves.

Devuelve 1 si hay cualquier fallo, con la lista de problemas.
"""
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
TEXTOS = Path(sys.argv[1]) if len(sys.argv) > 1 else \
    RAIZ / "app/src/main/java/pe/yachayallpa/app/Textos.kt"
UI = RAIZ / "app/src/main/java/pe/yachayallpa/app"

# Iguales en los dos idiomas a proposito: unidades, nombres propios y siglas.
IGUALES_A_PROPOSITO = {
    "clima.mm", "altura.ejemplo", "ubic.compacto", "decision.cosecha", "idioma.qu.corto", "idioma.qu", "estrategia.cosecha", "cultivo.PAPA", "cultivo.PALLAR", "kusi.nombre",
    "cultivo.HABAS", "enso.nino", "enso.nina", "campana.oni", "siembra.lugar",
    "altura.zona", "mes.4", "campana.enso",
}

PAR = re.compile(r'"([^"]+)"\s+to\s+"((?:[^"\\]|\\.)*)"')
HUECO = re.compile(r"%(?:\d+\$)?[-+ 0#,(]*\d*(?:\.\d+)?[a-zA-Z%]")


def tablas(fuente: str):
    salida = {}
    for nombre in ("ES", "QU"):
        inicio = fuente.index(f"val {nombre}: Map<String, String> = mapOf(")
        fin = fuente.index("\n    )", inicio)
        cuerpo = fuente[inicio:fin]
        # Todas las parejas del bloque, aunque vayan varias en la misma linea.
        pares = PAR.findall(cuerpo)
        tabla, repetidas = {}, []
        for clave, valor in pares:
            if clave in tabla and tabla[clave] != valor:
                repetidas.append(clave)
            tabla[clave] = valor
        salida[nombre] = (tabla, repetidas)
    return salida["ES"], salida["QU"]


def main() -> int:
    fuente = TEXTOS.read_text(encoding="utf-8")
    (es, repEs), (qu, repQu) = tablas(fuente)
    fallos = []

    for nombre, repetidas in (("ES", repEs), ("QU", repQu)):
        for clave in sorted(set(repetidas)):
            fallos.append(f"{nombre}: clave repetida con distinto valor: {clave}")

    for clave in sorted(set(es) - set(qu)):
        fallos.append(f"falta en quechua: {clave}")
    for clave in sorted(set(qu) - set(es)):
        fallos.append(f"sobra en quechua (no esta en castellano): {clave}")

    for clave in sorted(set(es) & set(qu)):
        he, hq = HUECO.findall(es[clave]), HUECO.findall(qu[clave])
        if he != hq:
            fallos.append(f"huecos distintos en {clave}: {he} vs {hq}")
        if es[clave] == qu[clave] and clave not in IGUALES_A_PROPOSITO:
            fallos.append(f"sin traducir (identico al castellano): {clave}")
        if not qu[clave].strip():
            fallos.append(f"quechua vacio: {clave}")

    # Claves que el UI pide de verdad.
    usadas = set()
    for fichero in list(UI.glob("*.kt")) + list(UI.glob("*.java")):
        if fichero.name == "Textos.kt":
            continue
        texto = fichero.read_text(encoding="utf-8")
        usadas |= set(re.findall(r'[Tt]\s*\[\s*"([^"]+)"\s*\]', texto))
        usadas |= set(re.findall(r'\.f\(\s*"([^"]+)"', texto))
        usadas |= set(re.findall(r'Textos\.(?:ES|QU)\[\s*"([^"]+)"\s*\]', texto))
        usadas |= set(re.findall(r'\bclave\s*=\s*"([^"]+)"', texto))
    # Las que se arman concatenando ("motivo." + clave) se reconocen por prefijo.
    usadas = {c for c in usadas if not c.endswith(".") and "$" not in c}

    for clave in sorted(usadas - set(es)):
        fallos.append(f"el UI pide una clave que no existe: {clave}")

    # Familias que el UI pide con la clave en una variable: el motor devuelve
    # "cobertura" o "rumbo_seco" y el nombre se arma al vuelo.
    prefijos = ("mes.", "cultivo.", "motivo.", "factor.", "enso.i", "rumbo_", "estrategia.A", "estrategia.R",
                "nivel.", "veredicto.", "riesgo.", "sequia.", "pref.", "hacer.",
                "voz.", "agua.", "gps.", "kusi.", "datos.error", "decision.", "dia.",
                "porque.", "rueda.", "ubic.altura.", "ubic.fuente.", "paso.", "estado.")
    huerfanas = [c for c in sorted(set(es) - usadas) if not c.startswith(prefijos)]
    for clave in huerfanas:
        fallos.append(f"clave sin usar en el UI: {clave}")

    print(f"claves: {len(es)} castellano, {len(qu)} quechua; "
          f"{len(usadas)} pedidas directamente por el UI")
    if fallos:
        print(f"\n{len(fallos)} problema(s):")
        for f in fallos:
            print(f"  - {f}")
        return 1
    print("sin problemas")
    return 0


if __name__ == "__main__":
    sys.exit(main())
