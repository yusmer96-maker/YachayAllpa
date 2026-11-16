package pe.yachayallpa.core;

import java.util.Arrays;

public enum Cultivo {
    // Valores de referencia: un agrónomo local debe validar agua, altitud, ciclo y tolerancia antes de uso productivo.
    PAPA("Papa","la papa",Familia.SOLANACEA,Etapa.CABECERA,2500,4200,500,5,.20,false,true,.50,1.15,.75,9,10,11),
    QUINUA("Quinua","la quinua",Familia.AMARANTACEA,Etapa.SEGUNDA,2400,4100,350,5,.35,false,false,.35,1.05,.45,9,10,11),
    OCA("Oca","la oca",Familia.OXALIDACEA,Etapa.SEGUNDA,2800,4000,450,7,.30,false,false,.50,1.05,.75,9,10),
    OLLUCO("Olluco","el olluco",Familia.BASELLACEA,Etapa.SEGUNDA,2600,4000,420,7,.30,false,false,.50,1.05,.75,9,10),
    MAIZ("Maíz amiláceo","el maíz amiláceo",Familia.POACEA,Etapa.CEREAL,1800,3200,500,6,.08,false,true,.30,1.20,.35,9,10,11),
    CEBADA("Cebada","la cebada",Familia.POACEA,Etapa.CEREAL,2500,4200,300,5,.45,false,true,.30,1.15,.25,9,10,11),
    HABAS("Habas","las habas",Familia.FABACEA,Etapa.LEGUMINOSA,2300,3900,380,5,.35,true,false,.40,1.15,.40,9,10,11),
    TARWI("Tarwi","el tarwi",Familia.FABACEA,Etapa.LEGUMINOSA,2500,3900,330,6,.40,true,false,.35,1.10,.45,9,10),
    ARVEJA("Arveja","la arveja",Familia.FABACEA,Etapa.LEGUMINOSA,1800,3300,320,4,.20,true,false,.40,1.10,.35,9,10,11),
    DESCANSO("Descanso","el descanso",Familia.NINGUNA,Etapa.DESCANSO,0,6000,0,0,1,false,false,0,0,0),
    // Estos tres cultivos de campaña cubren zonas bajas de Huánuco; calendario y altura son hipótesis locales.
    MAIZ_AMARILLO("Maíz amarillo","el maíz amarillo",Familia.POACEA,Etapa.CEREAL,50,1800,600,5,.05,false,true,.30,1.20,.35,9,10,11,12,1),
    FRIJOL("Frijol","el frijol",Familia.FABACEA,Etapa.LEGUMINOSA,50,2200,400,4,.08,true,false,.35,1.10,.30,9,10,11,12,1,2),
    YUCA("Yuca","la yuca",Familia.EUPHORBIACEA,Etapa.SEGUNDA,50,1500,900,10,.02,false,true,.30,1.10,.50,9,10,11,12,1),
    // SENASA registra estos cultivos en Huánuco; rangos de FAO/INIA y Kc provisional hasta validación local.
    MASHUA("Mashua","la mashua",Familia.TROPAEOLACEA,Etapa.SEGUNDA,2800,4100,500,7,.45,false,false,.45,1.10,.70,8,9,10),
    CAMOTE("Camote","el camote",Familia.CONVOLVULACEA,Etapa.SEGUNDA,50,2500,550,5,.05,false,true,.45,1.15,.65,9,10,11,12,1),
    YACON("Yacón","el yacón",Familia.ASTERACEA,Etapa.SEGUNDA,500,3200,650,8,.12,false,false,.40,1.10,.70,9,10),
    ARRACACHA("Arracacha","la arracacha",Familia.APIACEA,Etapa.SEGUNDA,1000,3200,650,9,.10,false,false,.45,1.10,.65,9,10,11),
    // SENASA confirma estos cultivos en ferias de Huánuco; los umbrales siguen siendo hipótesis de secano.
    PALLAR("Pallar","el pallar",Familia.FABACEA,Etapa.LEGUMINOSA,50,2200,450,5,.05,true,false,.40,1.15,.40,9,10,11,12,1),
    TRIGO("Trigo","el trigo",Familia.POACEA,Etapa.CEREAL,1800,4000,450,5,.35,false,true,.30,1.15,.25,9,10,11),
    MANI("Maní","el maní",Familia.FABACEA,Etapa.LEGUMINOSA,50,1800,500,5,.02,true,false,.40,1.15,.50,9,10,11,12,1),
    KIWICHA("Kiwicha","la kiwicha",Familia.AMARANTACEA,Etapa.SEGUNDA,1500,3500,400,5,.20,false,false,.35,1.05,.40,9,10,11),
    CALABAZA("Calabaza","la calabaza",Familia.CUCURBITACEA,Etapa.SEGUNDA,50,2800,550,5,.02,false,true,.50,1.05,.60,9,10,11,12,1),
    // Superficie sembrada en Huánuco 2024-2025 (INEI, Compendio Estadístico 2025, cuadro 12.2,
    // fuente DRA Huánuco): arroz cáscara 13 644 ha, el tercer cultivo de la región, en la selva;
    // avena 2 637 ha entre grano y forraje, en la sierra alta. Kc de FAO-56; rangos orientativos.
    ARROZ("Arroz","el arroz",Familia.POACEA,Etapa.CEREAL,50,1200,1000,5,.02,false,true,1.05,1.20,.90,10,11,12,1),
    AVENA("Avena","la avena",Familia.POACEA,Etapa.CEREAL,2500,4000,400,5,.45,false,true,.30,1.15,.25,10,11,12);
    public final String nombre, articulo;
    public final Familia familia;
    public final Etapa etapa;
    public final int altitudMin, altitudMax, cicloMeses;
    public final double aguaCicloMm, toleranciaHelada;
    // Coeficientes inicial, medio y final de ETc=Kc×ETo: referencias que requiere revisar un agrónomo local.
    public final double kcInicial, kcMedio, kcFinal;
    public final boolean fijaNitrogeno, consumeNitrogeno;
    private final int[] meses;
    Cultivo(String nombre,String articulo,Familia familia,Etapa etapa,int min,int max,double agua,int ciclo,double tolerancia,boolean fija,boolean consume,double kcInicial,double kcMedio,double kcFinal,int... meses) {
        this.nombre=nombre; this.articulo=articulo; this.familia=familia; this.etapa=etapa;
        altitudMin=min; altitudMax=max; aguaCicloMm=agua; cicloMeses=ciclo; toleranciaHelada=tolerancia;
        fijaNitrogeno=fija; consumeNitrogeno=consume; this.kcInicial=kcInicial;
        this.kcMedio=kcMedio; this.kcFinal=kcFinal; this.meses=meses.clone();
    }
    /**
     * Margen de altura fuera del rango del cultivo: 300 m, unos 2 °C con el gradiente de
     * -6,5 °C/km que usa el motor para la helada y la ETo. Un solo margen para el filtro de
     * opciones, el puntaje, los motivos y la rueda.
     */
    public static final int MARGEN_ALTURA = 300;

    /** Metros que la chacra está fuera del rango del cultivo (0 si está dentro). */
    public int fueraDeRango(int altitud) {
        return altitud < altitudMin ? altitudMin - altitud : Math.max(0, altitud - altitudMax);
    }

    /** Si se da a esta altura: dentro del rango o a menos de MARGEN_ALTURA de él. */
    public boolean seDaA(int altitud) {
        return this == DESCANSO || fueraDeRango(altitud) <= MARGEN_ALTURA;
    }

    /**
     * Factor de respuesta del rendimiento al agua (Ky estacional, FAO-33: Doorenbos
     * y Kassam, 1979). Cosecha relativa = 1 − Ky·(1 − ETa/ETc). Valores de FAO-33:
     * papa 1,10; maíz 1,25; frijol y pallar 1,15; arveja 1,15; trigo 1,15; maní 0,70.
     * Provisionales por cultivo parecido (a validar con INIA): oca, olluco, mashua,
     * yacón y arracacha como papa; habas como arveja; cebada, avena, tarwi y camote 1,00;
     * quinua y kiwicha 0,90 (toleran sequía); yuca 0,80; calabaza 1,10; arroz 1,20.
     */
    public double ky() {
        switch (this) {
            case PAPA: return 1.10;
            case QUINUA: return 0.90;
            case OCA: return 1.10;
            case OLLUCO: return 1.10;
            case MAIZ: return 1.25;
            case CEBADA: return 1.00;
            case HABAS: return 1.15;
            case TARWI: return 1.00;
            case ARVEJA: return 1.15;
            case DESCANSO: return 0;
            case MAIZ_AMARILLO: return 1.25;
            case FRIJOL: return 1.15;
            case YUCA: return 0.80;
            case MASHUA: return 1.10;
            case CAMOTE: return 1.00;
            case YACON: return 1.10;
            case ARRACACHA: return 1.10;
            case PALLAR: return 1.15;
            case TRIGO: return 1.15;
            case MANI: return 0.70;
            case KIWICHA: return 0.90;
            case CALABAZA: return 1.10;
            case ARROZ: return 1.20;
            case AVENA: return 1.00;
            default: return 1.0;
        }
    }
    public int[] mesesSiembra() { return Arrays.copyOf(meses,meses.length); }
}
