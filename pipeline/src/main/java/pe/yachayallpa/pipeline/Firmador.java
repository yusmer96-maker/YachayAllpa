package pe.yachayallpa.pipeline;
import pe.yachayallpa.core.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

public final class Firmador {
    public static void crearLlaves(Path dir) throws Exception {
        Files.createDirectories(dir);Path privada=dir.resolve("privada.key"),publica=dir.resolve("publica.key");
        if(Files.exists(privada)||Files.exists(publica))throw new IOException("Ya existe una llave: no se sobrescribe");
        KeyPairGenerator g=KeyPairGenerator.getInstance("EC");g.initialize(new ECGenParameterSpec("secp256r1"));KeyPair par=g.generateKeyPair();
        Files.write(privada,Base64.getEncoder().encode(par.getPrivate().getEncoded()),StandardOpenOption.CREATE_NEW);
        try{Files.setPosixFilePermissions(privada,PosixFilePermissions.fromString("rw-------"));}catch(UnsupportedOperationException ignored){}
        Files.write(publica,Base64.getEncoder().encode(par.getPublic().getEncoded()),StandardOpenOption.CREATE_NEW);
    }
    public static PrivateKey leerPrivada(Path ruta) throws Exception {
        byte[] datos=Base64.getDecoder().decode(Files.readString(ruta).trim());
        return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(datos));
    }
    public static byte[] firmar(Pack pack,PrivateKey privada) throws Exception {
        byte[] cuerpo=PackFormato.cuerpo(pack);Signature s=Signature.getInstance("SHA256withECDSA");s.initSign(privada);s.update(cuerpo);
        byte[] pie=("firma="+Base64.getEncoder().encodeToString(s.sign())+"\n").getBytes(StandardCharsets.US_ASCII);
        byte[] archivo=new byte[cuerpo.length+pie.length];System.arraycopy(cuerpo,0,archivo,0,cuerpo.length);System.arraycopy(pie,0,archivo,cuerpo.length,pie.length);
        if(archivo.length>65536)throw new IOException("Pack supera 64 KB");return archivo;
    }
}
