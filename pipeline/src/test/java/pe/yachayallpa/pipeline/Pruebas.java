package pe.yachayallpa.pipeline;
import pe.yachayallpa.core.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.time.*;
import java.util.*;

public final class Pruebas {
    private static void afirmar(boolean condicion,String mensaje){if(!condicion)throw new AssertionError(mensaje);}
    private static String sintetico(){
        StringBuilder b=new StringBuilder("-BEGIN HEADER-\nNASA/POWER Source Native Resolution Daily Data\nElevation from MERRA-2 = 2584.67 meters\n-END HEADER-\nYEAR,DOY,PRECTOTCORR,T2M_MIN\n");
        for(LocalDate d=LocalDate.of(1991,1,1);!d.isAfter(LocalDate.of(2025,12,31));d=d.plusDays(1)){
            int m=d.getMonthValue();double lluvia=m==1?5:m==7?.4:m==9?(d.getYear()>=2011?1:2):3;
            double t=2+(d.getYear()-1991)*.04+(m==7?-2:0)+(d.getDayOfMonth()%5-2)*.3;
            b.append(String.format(Locale.ROOT,"%d,%d,%.2f,%.2f\n",d.getYear(),d.getDayOfYear(),lluvia,t));
        }
        return b.toString();
    }
    private static void rechaza(PackVerificador v,byte[] bytes,int instalada,PackInvalidoException.Motivo motivo)throws Exception{
        try{v.verificar(bytes,instalada);throw new AssertionError("No rechazó "+motivo);}catch(PackInvalidoException e){afirmar(e.motivo==motivo,"Motivo: "+e.motivo);}
    }
    public static void main(String[] args)throws Exception{
        NasaPower.Datos datos=NasaPower.parsear(sintetico());
        afirmar(Math.abs(datos.elevacionCelda-2584.67)<.01,"Elevación");
        String repetido=sintetico()+"1991,1,1.00,2.00\n";
        try{NasaPower.parsear(repetido);throw new AssertionError("Aceptó un día repetido");}
        catch(IllegalArgumentException esperado){afirmar(esperado.getMessage().contains("repetida"),"Fecha repetida");}
        Agregador a=new Agregador();PerfilClima antes=a.agregar(datos,1991,2005),ahora=a.agregar(datos,2011,2025);
        List<NasaPower.Dia> unAno=new ArrayList<>();
        for(NasaPower.Dia dia:datos.dias)if(dia.fecha.getYear()==1991)unAno.add(dia);
        try{a.agregar(new NasaPower.Datos(datos.elevacionCelda,unAno),1991,2005);
            throw new AssertionError("Aceptó un año como clima de 15 años");}
        catch(IllegalArgumentException esperado){afirmar(esperado.getMessage().contains("años válidos"),"Cobertura histórica");}
        afirmar(ahora.lluvia(1)>ahora.lluvia(7),"Estación lluviosa");
        afirmar(ahora.tmin(1)>antes.tmin(1),"Calentamiento");
        afirmar(ahora.lluvia(9)<antes.lluvia(9),"Septiembre seco");
        Pack pack=new Pack(20261114,"Amarilis",-9.94,-76.24,1900,datos.elevacionCelda,"CSV sintético SOLO pruebas",ahora,antes);
        KeyPairGenerator g=KeyPairGenerator.getInstance("EC");g.initialize(new ECGenParameterSpec("secp256r1"));KeyPair par=g.generateKeyPair(),otra=g.generateKeyPair();
        PackVerificador v=new PackVerificador(Base64.getEncoder().encodeToString(par.getPublic().getEncoded()));
        byte[] bytes=Firmador.firmar(pack,par.getPrivate());
        Pack leído=v.verificar(bytes,0);afirmar(leído.distrito.equals("Amarilis")&&Math.abs(leído.ahora.lluvia(9)-ahora.lluvia(9))<.01,"Pack firmado");
        byte[] alterado=bytes.clone();String texto=new String(alterado,StandardCharsets.UTF_8);int pos=texto.indexOf("lat=-9.94");alterado[pos+5]='8';
        rechaza(v,alterado,0,PackInvalidoException.Motivo.FIRMA);
        rechaza(v,bytes,20261115,PackInvalidoException.Motivo.VERSION_ANTIGUA);
        rechaza(v,"basura".getBytes(StandardCharsets.UTF_8),0,PackInvalidoException.Motivo.FORMATO);
        rechaza(v,Firmador.firmar(pack,otra.getPrivate()),0,PackInvalidoException.Motivo.FIRMA);
        afirmar(Math.abs(Clima.normalCdf(0)-.5)<.00001&&Math.abs(Clima.normalCdf(1.96)-.975)<.001,"CDF normal");
        afirmar(Clima.probHelada(2,2,2600,3500)>Clima.probHelada(2,2,2600,2500),"Helada por altura");
        MotorRotacion motor=new MotorRotacion();
        List<Recomendacion> papa=motor.recomendar(new Contexto(pack,3000,Cultivo.PAPA,1));
        afirmar(papa.get(0).cultivo!=Cultivo.PAPA,"No repetir papa");
        afirmar(puntos(papa,Cultivo.QUINUA)>puntos(papa,Cultivo.PAPA),"Quinua sobre papa");
        List<Recomendacion> habas=motor.recomendar(new Contexto(pack,3000,Cultivo.HABAS,5));
        afirmar(habas.get(0).cultivo==Cultivo.DESCANSO,"Descanso primero");
        List<Recomendacion> bajo=motor.recomendar(new Contexto(pack,1900,Cultivo.PAPA,1));
        afirmar(puntos(bajo,Cultivo.OCA)<puntos(bajo,Cultivo.ARVEJA),"Oca peor que arveja a 1900 m");
        pruebasTemporada(par,otra);
        imprimir("Después de papa, 3000 m",papa);imprimir("Después de habas, 5 siembras seguidas",habas);
        System.out.println("TODAS LAS PRUEBAS PASARON");
    }
    private static void pruebasTemporada(KeyPair llave,KeyPair otra)throws Exception {
        double[] seco=new double[Temporada.ESCENARIOS*Temporada.BLOQUES];
        double[] humedo=new double[seco.length],eto=new double[seco.length];
        double[] tmin=new double[seco.length],desv=new double[seco.length];
        Arrays.fill(seco,4);Arrays.fill(humedo,55);Arrays.fill(eto,28);
        Arrays.fill(tmin,2);Arrays.fill(desv,2);
        double[] grafico=new double[]{3,8,14,6,20,11};
        Temporada a=new Temporada(20261114,"Zona de prueba",LocalDate.of(2026,11,11),
                -9.9,-76.2,2600,"NASA sintético","solo prueba",10,20,35,75,
                seco,eto,tmin,desv,grafico,grafico);
        Temporada b=new Temporada(20261114,"Zona de prueba",LocalDate.of(2026,11,11),
                -9.9,-76.2,2600,"NASA sintético","solo prueba",70,120,300,75,
                humedo,eto,tmin,desv,grafico,grafico);
        String publica=Base64.getEncoder().encodeToString(llave.getPublic().getEncoded());
        byte[] firmado=TemporadaFormato.firmar(a,llave.getPrivate());
        Temporada verificado=TemporadaFormato.verificar(firmado,publica,0);
        afirmar(verificado.lluvia(0,0)==4&&verificado.fechaCorte.equals(a.fechaCorte)
                &&Math.abs(verificado.lon-a.lon)<.00001,"Temporada firmada");
        StringBuilder csvNuevo = new StringBuilder("-BEGIN HEADER-\nLocation: latitude -9.9 longitude -76.2\n-END HEADER-\nYEAR,DOY,PRECTOTCORR\n");
        for (LocalDate dia=LocalDate.of(2011,1,1); !dia.isAfter(a.fechaCorte); dia=dia.plusDays(1))
            csvNuevo.append(dia.getYear()).append(',').append(dia.getDayOfYear())
                    .append(',').append(dia.getYear()==2026?"5.00":"2.00").append('\n');
        // La base instalada llega al 30-sep: lo reciente solo se compara hasta
        // donde llega MERRA-2 (el 14-nov corta el 30-sep).
        Temporada baseSep=new Temporada(20261001,"Zona de prueba",LocalDate.of(2026,9,30),
                -9.9,-76.2,2600,"NASA sintético","solo prueba",10,20,35,75,
                seco,eto,tmin,desv,grafico,grafico);
        Temporada renovada=PowerActualizador.actualizar(baseSep,csvNuevo.toString(),
                LocalDate.of(2026,11,14));
        afirmar(renovada.lluvia30==150 && renovada.promedio30==60
                && renovada.fechaCorte.equals(LocalDate.of(2026,9,30)),
                "CSV NASA renueva lluvia real y compara años, cortando en MERRA-2");
        afirmar(PowerActualizador.confiableHasta(LocalDate.of(2026,11,15)).equals(LocalDate.of(2026,9,30))
                && PowerActualizador.confiableHasta(LocalDate.of(2026,12,1)).equals(LocalDate.of(2026,10,31))
                && PowerActualizador.confiableHasta(LocalDate.of(2027,1,31)).equals(LocalDate.of(2026,11,30)),
                "Límite MERRA-2: fin del mes de hace dos meses");
        // Octubre y noviembre con lluvia diaria de GEOS-IT no pueden entrar en la cuenta.
        StringBuilder csvGeos = new StringBuilder("-BEGIN HEADER-\nLocation: latitude -9.9 longitude -76.2\n-END HEADER-\nYEAR,DOY,PRECTOTCORR\n");
        for (LocalDate dia=LocalDate.of(2011,1,1); !dia.isAfter(LocalDate.of(2026,11,12)); dia=dia.plusDays(1))
            csvGeos.append(dia.getYear()).append(',').append(dia.getDayOfYear()).append(',')
                    .append(dia.isAfter(LocalDate.of(2026,9,30))?"20.00":"1.00").append('\n');
        Temporada sinGeos=PowerActualizador.actualizar(baseSep,csvGeos.toString(),LocalDate.of(2026,11,14));
        afirmar(sinGeos.lluvia30==30 && sinGeos.fechaCorte.equals(LocalDate.of(2026,9,30)),
                "La lluvia GEOS-IT posterior a MERRA-2 queda fuera de la comparación");
        try { PowerActualizador.actualizar(verificado,
                    csvNuevo.toString().replace("Location: latitude -9.9 longitude -76.2", "sin lugar"),
                    LocalDate.of(2026,11,14));
            throw new AssertionError("Aceptó CSV sin ubicación");
        } catch (IllegalArgumentException esperado) { afirmar(esperado.getMessage().contains("zona"),"Ubicación NASA obligatoria"); }
        try { PowerActualizador.actualizar(verificado,
                    csvNuevo.toString().replace("latitude -9.9", "latitude -8.0"),
                    LocalDate.of(2026,11,14));
            throw new AssertionError("Aceptó otra celda NASA");
        } catch (IllegalArgumentException esperado) { afirmar(esperado.getMessage().contains("zona"),"Celda NASA fija"); }
        try{TemporadaFormato.verificar(firmado,publica,20261115);throw new AssertionError("Rollback temporada");}
        catch(PackInvalidoException e){afirmar(e.motivo==PackInvalidoException.Motivo.VERSION_ANTIGUA,"Rollback temporada");}
        byte[] cambiado=firmado.clone();int pos=new String(cambiado,StandardCharsets.UTF_8).indexOf("lluvia30=35.00");
        cambiado[pos+9]='9';
        try{TemporadaFormato.verificar(cambiado,publica,0);throw new AssertionError("Alteración temporada");}
        catch(PackInvalidoException e){afirmar(e.motivo==PackInvalidoException.Motivo.FIRMA,"Firma temporada");}
        try{TemporadaFormato.verificar(TemporadaFormato.firmar(a,otra.getPrivate()),publica,0);
            throw new AssertionError("Otra llave temporada");}
        catch(PackInvalidoException e){afirmar(e.motivo==PackInvalidoException.Motivo.FIRMA,"Otra llave temporada");}
        MotorTemporada motor=new MotorTemporada();LocalDate hoy=LocalDate.of(2026,11,14);
        List<ConsejoTemporada> opcionesSecas=motor.recomendar(a,3000,Cultivo.PAPA,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy);
        List<ConsejoTemporada> opcionesHum=motor.recomendar(b,3000,Cultivo.PAPA,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy);
        afirmar(puntosNuevos(opcionesHum,Cultivo.QUINUA)>puntosNuevos(opcionesSecas,Cultivo.QUINUA),
                "La lluvia cambia el resultado de quinua");
        afirmar(opcionesSecas.get(0).cultivo!=Cultivo.PAPA,"No repetir papa en motor nuevo");
        List<ConsejoTemporada> descanso=motor.recomendar(b,3000,Cultivo.HABAS,5,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.CUIDAR_SUELO,hoy);
        afirmar(descanso.get(0).cultivo==Cultivo.DESCANSO,"Descanso primero en motor nuevo");
        List<ConsejoTemporada> valle=motor.recomendar(b,1900,Cultivo.PAPA,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy);
        List<ConsejoTemporada> altura=motor.recomendar(b,3800,Cultivo.PAPA,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy);
        afirmar(valle.get(0).cultivo!=altura.get(0).cultivo,
                "La altura cambia la primera opción");
        List<ConsejoTemporada> selva=motor.recomendar(b,300,Cultivo.MAIZ_AMARILLO,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy);
        afirmar(selva.stream().anyMatch(r->r.cultivo==Cultivo.YUCA)
                &&selva.stream().anyMatch(r->r.cultivo==Cultivo.FRIJOL),
                "Las zonas bajas incluyen cultivos de campaña propios de la región");
        afirmar(selva.stream().noneMatch(r->r.cultivo==Cultivo.PAPA || r.cultivo==Cultivo.QUINUA),
                "No se ofrece papa o quinua en la selva baja");
        coherenciaDelVeredicto(a,b);
        // Plan de rotación: 4 campañas encadenadas, años seguidos, y tras 4 siembras
        // seguidas sin descanso el motor acaba pidiendo descansar.
        List<ConsejoTemporada> hoyOpc=motor.recomendar(b,3000,Cultivo.PAPA,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy);
        Decision ahoraD=Decision.tomar(hoyOpc,null,3000,hoy);
        List<PlanRotacion.Campana> plan=PlanRotacion.armar(b,3000,Cultivo.PAPA,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy,ahoraD,4);
        afirmar(plan.size()==4&&plan.get(0).consejo==ahoraD.consejo
                &&plan.get(3).anio==hoy.getYear()+3,"Plan de 4 campañas encadenadas");
        for(int k=1;k<plan.size();k++)
            afirmar(plan.get(k).consejo.cultivo!=plan.get(k-1).consejo.cultivo
                    ||plan.get(k).consejo.cultivo==Cultivo.DESCANSO,"El plan no repite cultivo");
        StringBuilder rotacion=new StringBuilder();
        for(PlanRotacion.Campana c:plan)rotacion.append(c.anio).append(' ').append(c.consejo.cultivo).append("  ");
        System.out.println("Plan de rotación: "+rotacion);
        double[] media=new double[seco.length];Arrays.fill(media,8);
        // Lluvia justa (20 mm por bloque frente a 28 de ETo): ahí la demanda decide.
        double[] justa=new double[seco.length];Arrays.fill(justa,20);
        Temporada conMedia=new Temporada(20261115,"Zona de prueba",LocalDate.of(2026,9,30),
                -9.9,-76.2,3400,"NASA sintético","solo prueba",10,20,35,75,
                justa,eto,tmin,desv,media,grafico,grafico);
        Temporada leida=TemporadaFormato.verificar(TemporadaFormato.firmar(conMedia,llave.getPrivate()),publica,0);
        afirmar(leida.tieneTmedia()&&leida.tmedia(3,7)==8,"Formato 2 firmado conserva la media");
        afirmar(!TemporadaFormato.verificar(firmado,publica,0).tieneTmedia(),"El formato 1 se sigue leyendo");
        // 1.500 m por debajo de una celda a 8 °C: 17,75 °C → (17,75+17,8)/(8+17,8) = 1,378.
        Temporada medio=leida;
        double cobAlta=coberturaDe(motor.recomendar(medio,3400,Cultivo.PAPA,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy),Cultivo.QUINUA);
        double cobBaja=coberturaDe(motor.recomendar(medio,2400,Cultivo.PAPA,1,
                MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy),Cultivo.QUINUA);
        afirmar(cobBaja<cobAlta,"Más abajo hace más calor: la demanda sube y la cobertura baja");
        System.out.printf(java.util.Locale.ROOT,"Altura y agua: quinua %.1f %% a 3400 m, %.1f %% a 2400 m%n",
                cobAlta*100,cobBaja*100);
        pruebasDecision();
        System.out.println("Motor por temporadas: seco " + opcionesSecas.get(0).cultivo.nombre
                + ", húmedo " + opcionesHum.get(0).cultivo.nombre
                + ", tras habas " + descanso.get(0).cultivo.nombre
                + ", zona baja " + selva.get(0).cultivo.nombre);
    }
    /**
     * Lo que la pantalla NO puede hacer: decir "muy buena" encima de una barra
     * roja, ni poner primera una opcion peor que las dos alternativas que lleva
     * al lado. Las dos cosas salen del mismo veredicto, asi que se comprueban
     * aqui, barriendo alturas y cultivos anteriores sobre las dos temporadas.
     */
    private static void coherenciaDelVeredicto(Temporada seca,Temporada humeda) {
        MotorTemporada motor=new MotorTemporada();LocalDate hoy=LocalDate.of(2026,11,14);
        int revisados=0;
        for(Temporada t:new Temporada[]{seca,humeda})
            for(int altitud:new int[]{300,900,2000,2800,3500,4100})
                for(Cultivo anterior:Cultivo.values()) {
                    if(altitud<anterior.altitudMin-500||altitud>anterior.altitudMax+500)continue;
                    List<ConsejoTemporada> r=motor.recomendar(t,altitud,anterior,1,
                            MotorTemporada.Suelo.NO_SE,MotorTemporada.Prioridad.EQUILIBRIO,hoy);
                    for(ConsejoTemporada c:r) {
                        Veredicto v=Veredicto.de(c);revisados++;
                        afirmar(v.nivel!=Veredicto.Nivel.BUENA
                                ||(v.agua==Veredicto.Nivel.BUENA&&v.helada==Veredicto.Nivel.BUENA),
                                "Opción buena con agua o helada floja: "+c.cultivo.nombre);
                        afirmar(v.faltaAgua>=0&&v.faltaAgua<=1&&v.excesoHelada>=0&&v.excesoHelada<=1,
                                "Índice de barra fuera de 0..1: "+c.cultivo.nombre);
                    }
                    List<ConsejoTemporada> tres=r.subList(0,Math.min(3,r.size()));
                    int primera=Veredicto.de(tres.get(0)).nivel.ordinal();
                    for(ConsejoTemporada otra:tres)
                        afirmar(Veredicto.de(otra).nivel.ordinal()<=primera,
                                "Una alternativa sale mejor que la primera opción");
                }
        afirmar(revisados>1000,"El barrido de coherencia revisó muy poco");
        System.out.println("Coherencia del veredicto: "+revisados+" opciones sin contradicción");
    }
    /** Cada regla de la recomendación, con opciones armadas a mano. */
    private static void pruebasDecision() {
        LocalDate hoy=LocalDate.of(2026,11,15);
        ConsejoTemporada quinuaHoy=new ConsejoTemporada(Cultivo.QUINUA,hoy,80,0,15,1.0,0,0,List.of());
        ConsejoTemporada quinuaMarzo=new ConsejoTemporada(Cultivo.QUINUA,LocalDate.of(2027,3,1),80,0,15,1.0,0,0,List.of());
        ConsejoTemporada descanso=new ConsejoTemporada(Cultivo.DESCANSO,null,70,0,0,1,0,0,
                List.of(new Motivo("descanso_seguidas")));
        // Papa con 40 % del agua: cosecha 1 − 1,10·0,60 = 34 %, por debajo de la mitad.
        ConsejoTemporada papaSeca=new ConsejoTemporada(Cultivo.PAPA,hoy,60,12,1,.40,0,0,List.of());
        // Pronóstico a 3000 m de celda, chacra a 3000 m: la mínima no se corrige.
        Decision.Pronostico lluvioso=new Decision.Pronostico(30,24,Double.NaN,6,hoy.plusDays(2),3000);
        Decision.Pronostico seco=new Decision.Pronostico(3,3,Double.NaN,6,hoy.plusDays(2),3000);
        Decision.Pronostico helado=new Decision.Pronostico(30,24,Double.NaN,-1,hoy.plusDays(3),3000);
        // 25 mm en 7 días pero repartidos: ningún tramo de 3 días llega a 20 -> esperar.
        Decision.Pronostico repartido=new Decision.Pronostico(25,12,Double.NaN,6,hoy.plusDays(2),3000);
        // Pronóstico seco, pero ya cayeron 22 mm medidos en 3 días -> sembrar.
        Decision.Pronostico yaLlovio=new Decision.Pronostico(3,3,22,6,hoy.plusDays(2),3000);
        afirmar(Decision.tomar(List.of(quinuaHoy,descanso),lluvioso,3000,hoy).accion
                ==Decision.Accion.SEMBRAR_YA,"Con lluvia prevista se siembra ya");
        afirmar(Decision.tomar(List.of(quinuaHoy,descanso),seco,3000,hoy).accion
                ==Decision.Accion.ESPERAR_LLUVIA,"Sin lluvia prevista se espera");
        afirmar(Decision.tomar(List.of(quinuaHoy,descanso),repartido,3000,hoy).accion
                ==Decision.Accion.ESPERAR_LLUVIA,"Lluvia repartida sin 20 mm en 3 días: esperar");
        Decision medida=Decision.tomar(List.of(quinuaHoy,descanso),yaLlovio,3000,hoy);
        afirmar(medida.accion==Decision.Accion.SEMBRAR_YA&&medida.porque.get(0).clave.equals("lluvia_medida"),
                "20 mm ya medidos en 3 días: sembrar");
        afirmar(Cultivo.PAPA.seDaA(2200)&&!Cultivo.PAPA.seDaA(2199)&&Cultivo.PAPA.seDaA(4500)
                &&!Cultivo.PAPA.seDaA(4501),"Margen de altura único de 300 m");
        // La quinua aguanta frío (tolerancia .35 < .40): -1 °C ya es esperar.
        afirmar(Decision.tomar(List.of(quinuaHoy,descanso),helado,3000,hoy).accion
                ==Decision.Accion.ESPERAR_HELADA,"Helada prevista: esperar");
        // La misma helada a 3000 m de celda no llega a una chacra 600 m más abajo (+3,9 °C).
        afirmar(Decision.tomar(List.of(quinuaHoy,descanso),helado,2400,hoy).accion
                ==Decision.Accion.SEMBRAR_YA,"La altura corrige la helada prevista");
        afirmar(Decision.tomar(List.of(quinuaMarzo,descanso),lluvioso,3000,hoy).accion
                ==Decision.Accion.SEMBRAR_EN_MES,"Fecha lejana: se dice el mes");
        afirmar(Decision.tomar(List.of(descanso,quinuaHoy),lluvioso,3000,hoy).accion
                ==Decision.Accion.DESCANSAR,"Descanso primero: descansar");
        Decision sinAgua=Decision.tomar(List.of(papaSeca,descanso),lluvioso,3000,hoy);
        afirmar(sinAgua.accion==Decision.Accion.DESCANSAR&&sinAgua.consejo.cultivo==Cultivo.DESCANSO,
                "Si ningún cultivo encaja, se recomienda descansar");
        afirmar(Decision.tomar(List.of(quinuaHoy,descanso),null,3000,hoy).accion
                ==Decision.Accion.SEMBRAR_CUANDO_LLUEVA,"Sin pronóstico, sembrar cuando llueva");
        // Cosecha FAO-33: frijol (Ky 1,15) con 74 % del agua → 1 − 1,15·0,26 = 70 %.
        ConsejoTemporada frijol=new ConsejoTemporada(Cultivo.FRIJOL,hoy,60,8,3,.74,0,0,List.of());
        afirmar(Math.abs(frijol.rendimientoEsperado-.701)<.001,"Cosecha esperada FAO-33");
        afirmar(Veredicto.de(frijol).agua==Veredicto.Nivel.CUIDADO,"70 % de cosecha es cuidado, no descanso");
        Decision conFrijol=Decision.tomar(List.of(frijol,descanso),lluvioso,3000,hoy);
        afirmar(conFrijol.accion==Decision.Accion.SEMBRAR_YA
                &&conFrijol.porque.stream().anyMatch(m->m.clave.equals("riego")),
                "Con 70 % de cosecha se siembra y se avisa del riego");
        System.out.println("Recomendación: 10 reglas comprobadas");
    }
    private static double coberturaDe(List<ConsejoTemporada> l,Cultivo c) {
        for(ConsejoTemporada r:l)if(r.cultivo==c)return r.coberturaAgua;
        throw new AssertionError("Falta "+c);
    }
    private static int puntosNuevos(List<ConsejoTemporada> l,Cultivo c) {
        for(ConsejoTemporada r:l)if(r.cultivo==c)return r.puntaje;
        throw new AssertionError("Falta cultivo");
    }
    private static int puntos(List<Recomendacion> l,Cultivo c){for(Recomendacion r:l)if(r.cultivo==c)return r.puntaje;throw new AssertionError();}
    private static void imprimir(String titulo,List<Recomendacion> l){
        System.out.println("\n"+titulo);
        for(int i=0;i<4;i++){Recomendacion r=l.get(i);StringBuilder puntos=new StringBuilder();for(int j=0;j<5;j++)puntos.append(j<r.circulos()?"●":"○");
            System.out.println(r.cultivo.nombre+" "+puntos+" ("+r.puntaje+")");for(Razon z:r.razones)System.out.println("  "+(z.favorable?"+ ":"! ")+z.texto);}
    }
}
