package pe.yachayallpa.pipeline;
public enum Distrito {
    // Las seis referencias locales iniciales son orientativas: contrastar altitudes con INEI/SRTM.
    // Las ZONA son centros de la grilla climática NASA, no alturas de chacras.
    AMARILIS("Amarilis",-9.94,-76.24,1900),
    HUANUCO("Huánuco",-9.93,-76.24,1900),
    CHURUBAMBA("Churubamba",-9.83,-76.13,2700),
    SANTA_MARIA_DEL_VALLE("Santa María del Valle",-9.86,-76.16,1950),
    PILLCO_MARCA("Pillco Marca",-9.96,-76.25,1950),
    SAN_FRANCISCO_DE_CAYRAN("San Francisco de Cayrán",-10.02,-76.29,2800),
    // Celdas POWER que intersectan las 11 provincias de Huánuco; son zonas climáticas, no distritos.
    ZONA_01("Zona climática 01",-10.5,-76.875,0),
    ZONA_02("Zona climática 02",-10.5,-76.25,0),
    ZONA_03("Zona climática 03",-10.5,-75.625,0),
    ZONA_04("Zona climática 04",-10,-76.875,0),
    ZONA_05("Zona climática 05",-10,-76.25,0),
    ZONA_06("Zona climática 06",-10,-75.625,0),
    ZONA_07("Zona climática 07",-10,-75,0),
    ZONA_08("Zona climática 08",-9.5,-76.875,0),
    ZONA_09("Zona climática 09",-9.5,-76.25,0),
    ZONA_10("Zona climática 10",-9.5,-75.625,0),
    ZONA_11("Zona climática 11",-9.5,-75,0),
    ZONA_12("Zona climática 12",-9.5,-74.375,0),
    ZONA_13("Zona climática 13",-9,-77.5,0),
    ZONA_14("Zona climática 14",-9,-76.875,0),
    ZONA_15("Zona climática 15",-9,-76.25,0),
    ZONA_16("Zona climática 16",-9,-75.625,0),
    ZONA_17("Zona climática 17",-9,-75,0),
    ZONA_18("Zona climática 18",-9,-74.375,0),
    ZONA_19("Zona climática 19",-8.5,-77.5,0),
    ZONA_20("Zona climática 20",-8.5,-76.875,0),
    ZONA_21("Zona climática 21",-8.5,-76.25,0),
    ZONA_22("Zona climática 22",-8.5,-75,0),
    ZONA_23("Zona climática 23",-8.5,-74.375,0);
    public final String nombre;public final double lat,lon;public final int altitud;
    Distrito(String n,double lat,double lon,int alt){nombre=n;this.lat=lat;this.lon=lon;altitud=alt;}
}
