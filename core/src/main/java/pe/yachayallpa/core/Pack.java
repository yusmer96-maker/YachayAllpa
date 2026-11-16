package pe.yachayallpa.core;
public final class Pack {
    public final int version;
    public final String distrito, fuente;
    public final double lat, lon, altitudReferencia, elevacionCelda;
    public final PerfilClima ahora, antes;
    public Pack(int version,String distrito,double lat,double lon,double altitudReferencia,double elevacionCelda,String fuente,PerfilClima ahora,PerfilClima antes) {
        if(version<19000101 || version>29991231 || distrito==null || distrito.isEmpty() || fuente==null || ahora==null || antes==null || !Double.isFinite(lat) || !Double.isFinite(lon) || !Double.isFinite(altitudReferencia) || !Double.isFinite(elevacionCelda)) throw new IllegalArgumentException("Pack inválido");
        this.version=version; this.distrito=distrito; this.lat=lat; this.lon=lon; this.altitudReferencia=altitudReferencia; this.elevacionCelda=elevacionCelda; this.fuente=fuente; this.ahora=ahora; this.antes=antes;
    }
}
