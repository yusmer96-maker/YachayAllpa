package pe.yachayallpa.core;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.interfaces.ECPublicKey;
import java.util.*;

public final class PackVerificador {
    private final PublicKey publica;
    public PackVerificador(String base64){
        try{publica=KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(base64.trim())));
            AlgorithmParameters parametros=AlgorithmParameters.getInstance("EC");parametros.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec esperado=parametros.getParameterSpec(ECParameterSpec.class), actual=((ECPublicKey)publica).getParams();
            if(!actual.getCurve().equals(esperado.getCurve())||!actual.getGenerator().equals(esperado.getGenerator())||!actual.getOrder().equals(esperado.getOrder())||actual.getCofactor()!=esperado.getCofactor())throw new IllegalArgumentException("Se requiere P-256");}
        catch(GeneralSecurityException|IllegalArgumentException e){throw new IllegalArgumentException("Llave pública inválida",e);}
    }
    public Pack verificar(byte[] archivo,int versionInstalada) throws PackInvalidoException {
        if(archivo==null||archivo.length>65536||archivo.length<20)throw new PackInvalidoException(PackInvalidoException.Motivo.FORMATO,"Tamaño inválido");
        byte[] marca="firma=".getBytes(StandardCharsets.US_ASCII);
        int inicio=-1;
        for(int i=0;i<=archivo.length-marca.length;i++){
            if(i>0&&archivo[i-1]=='\n'){
                boolean coincide=true;for(int j=0;j<marca.length;j++)if(archivo[i+j]!=marca[j])coincide=false;
                if(coincide)inicio=i;
            }
        }
        if(inicio<0||archivo[archivo.length-1]!='\n')throw new PackInvalidoException(PackInvalidoException.Motivo.FORMATO,"Falta firma final");
        int fin=archivo.length-1;
        for(int i=inicio+marca.length;i<fin;i++)if(archivo[i]=='\n'||archivo[i]=='\r')throw new PackInvalidoException(PackInvalidoException.Motivo.FORMATO,"Firma no final");
        byte[] cuerpo=Arrays.copyOfRange(archivo,0,inicio);
        String firmaTexto=new String(archivo,inicio+marca.length,fin-inicio-marca.length,StandardCharsets.US_ASCII);
        byte[] firma;
        try{firma=Base64.getDecoder().decode(firmaTexto);}catch(IllegalArgumentException e){throw new PackInvalidoException(PackInvalidoException.Motivo.FORMATO,"Base64 inválida");}
        try{
            Signature s=Signature.getInstance("SHA256withECDSA");s.initVerify(publica);s.update(cuerpo);
            if(!s.verify(firma))throw new PackInvalidoException(PackInvalidoException.Motivo.FIRMA,"Firma no coincide");
        }catch(PackInvalidoException e){throw e;}
        catch(GeneralSecurityException e){throw new PackInvalidoException(PackInvalidoException.Motivo.FIRMA,"Firma inválida");}
        // Los campos del pack se interpretan solo después de autenticar sus bytes.
        Pack p=PackFormato.interpretar(cuerpo);
        if(p.version<versionInstalada)throw new PackInvalidoException(PackInvalidoException.Motivo.VERSION_ANTIGUA,"Pack anterior al instalado");
        return p;
    }
}
