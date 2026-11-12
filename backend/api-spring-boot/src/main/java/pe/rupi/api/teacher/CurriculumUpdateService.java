package pe.rupi.api.teacher;

import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * HU-51: cuando el MINEDU publica una resolución y la consola retira la versión del catálogo, las rutas del
 * docente siguen en la versión anterior. Aquí el docente revisa y aplica la correspondencia publicada por
 * Django (curriculo_correspondencia): la ruta pasa a la versión vigente, cada parada a la unidad equivalente
 * y las paradas de unidades retiradas quedan en «Otras paradas». Spring solo escribe aprendizaje_* y escuela_*.
 */
@Service
public class CurriculumUpdateService {
    public record Resolution(String number,LocalDate issuedOn,String summary,String officialLink) {}
    public record UnitChange(String fromTitle,String toTitle,String change,int stops) {}
    public record Plan(boolean available,String message,String fromVersion,String toVersion,String fromArea,String toArea,
                       List<Resolution> resolutions,List<UnitChange> units,int stopsWithoutUnit) {}

    private record Header(String versionRoute,String catalog,String catalogLabel,String catalogState,String area,String areaName,String grade) {}
    private record Hop(String resolution,String number,LocalDate issuedOn,String summary,String link,String to,String toLabel,String toState) {}
    private record Mapping(Plan plan,String versionRoute,String toCatalog,String toArea,Map<String,String> units) {}

    private final NamedParameterJdbcTemplate jdbc;
    public CurriculumUpdateService(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }

    public Plan plan(String teacher,String route) { return map(teacher,route,false).plan(); }

    @Transactional
    public Plan apply(String teacher,String route) {
        Mapping mapping=map(teacher,route,true);
        if(!mapping.plan().available())
            throw new ResponseStatusException(HttpStatus.CONFLICT,mapping.plan().message());
        var params=new MapSqlParameterSource("route",mapping.versionRoute()).addValue("catalog",mapping.toCatalog())
            .addValue("area",mapping.toArea());
        jdbc.update("UPDATE aprendizaje_version_ruta SET version_catalogo_id=:catalog,area_id=:area WHERE id=:route",params);
        for(var entry:mapping.units().entrySet()) {
            var unit=new MapSqlParameterSource("route",mapping.versionRoute()).addValue("from",entry.getKey())
                .addValue("to",entry.getValue());
            jdbc.update("UPDATE aprendizaje_nodo_ruta SET unidad_id=:to WHERE version_ruta_id=:route AND unidad_id=:from",unit);
            // Lo ya programado en el calendario del aula sigue a su unidad; si se retiró, conserva su título.
            if(entry.getValue()!=null)
                jdbc.update("""
                    UPDATE escuela_planificacion_ruta SET referencia_id=:to
                    WHERE version_ruta_id=:route AND tipo='UNIDAD' AND referencia_id=:from
                    """,unit);
        }
        return new Plan(false,"La ruta ya sigue la versión vigente "+mapping.plan().toVersion()+".",
            mapping.plan().fromVersion(),mapping.plan().toVersion(),mapping.plan().fromArea(),mapping.plan().toArea(),
            mapping.plan().resolutions(),mapping.plan().units(),mapping.plan().stopsWithoutUnit());
    }

    private Header header(String teacher,String route,boolean lock) {
        return jdbc.query("""
            SELECT vr.id,vr.version_catalogo_id,vc.etiqueta_version,vc.estado,vr.area_id,a.nombre,vr.grado_id
            FROM aprendizaje_version_ruta vr
            JOIN aprendizaje_ruta r ON r.id=vr.ruta_id
            JOIN curriculo_version_catalogo vc ON vc.id=vr.version_catalogo_id
            JOIN curriculo_area a ON a.id=vr.area_id
            WHERE r.id=:route AND r.propietario_id=:teacher AND r.archivado_en IS NULL
            ORDER BY vr.numero_version DESC LIMIT 1""" + (lock?" FOR UPDATE OF vr":""),
            new MapSqlParameterSource("route",route).addValue("teacher",teacher),
            (rs,n)->new Header(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),
                rs.getString(6),rs.getString(7))).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ruta no encontrada o no pertenece al docente."));
    }

    /** Resoluciones publicadas desde la versión de la ruta hasta la vigente (una por año, en orden). */
    private List<Hop> chain(String catalog) {
        List<Hop> hops=new ArrayList<>();
        Set<String> seen=new HashSet<>(Set.of(catalog));
        String current=catalog;
        while(hops.size()<20) {
            var next=jdbc.query("""
                SELECT r.id,r.numero,r.fecha_emision,r.resumen,r.enlace_oficial,d.id,d.etiqueta_version,d.estado
                FROM curriculo_resolucion r JOIN curriculo_version_catalogo d ON d.id=r.version_destino_id
                WHERE r.version_origen_id=:from AND r.publicado_en IS NOT NULL ORDER BY r.publicado_en DESC LIMIT 1""",
                Map.of("from",current),(rs,n)->new Hop(rs.getString(1),rs.getString(2),rs.getObject(3,LocalDate.class),
                    rs.getString(4),rs.getString(5),rs.getString(6),rs.getString(7),rs.getString(8)))
                .stream().findFirst();
            if(next.isEmpty()||!seen.add(next.get().to())) break;
            hops.add(next.get());
            if("ACTIVO".equals(next.get().toState())) break;
            current=next.get().to();
        }
        return hops;
    }

    private record Step(String to,String change) {}
    private Optional<Step> step(String resolution,String type,String from) {
        return jdbc.query("""
            SELECT destino_id,cambio FROM curriculo_correspondencia
            WHERE resolucion_id=:resolution AND tipo=:type AND origen_id=:from""",
            new MapSqlParameterSource("resolution",resolution).addValue("type",type).addValue("from",from),
            (rs,n)->new Step(rs.getString(1),rs.getString(2))).stream().findFirst();
    }

    private String unitTitle(String id) {
        return jdbc.query("SELECT titulo FROM curriculo_unidad WHERE id=:id",Map.of("id",id),(rs,n)->rs.getString(1))
            .stream().findFirst().orElse(null);
    }

    private Mapping map(String teacher,String route,boolean lock) {
        Header h=header(teacher,route,lock);
        if("ACTIVO".equals(h.catalogState()))
            return new Mapping(new Plan(false,"La ruta ya sigue la versión vigente del catálogo.",h.catalogLabel(),
                h.catalogLabel(),h.areaName(),h.areaName(),List.of(),List.of(),0),h.versionRoute(),h.catalog(),h.area(),Map.of());
        List<Hop> hops=chain(h.catalog());
        List<Resolution> resolutions=hops.stream().map(x->new Resolution(x.number(),x.issuedOn(),x.summary(),x.link())).toList();
        if(hops.isEmpty()||!"ACTIVO".equals(hops.get(hops.size()-1).toState()))
            return new Mapping(new Plan(false,"Todavía no hay una resolución publicada que lleve esta ruta a la versión vigente. "
                + "Consulta con la administración.",h.catalogLabel(),null,h.areaName(),null,resolutions,List.of(),0),
                h.versionRoute(),null,null,Map.of());
        Hop last=hops.get(hops.size()-1);

        String area=h.area();
        for(Hop hop:hops) {
            var next=step(hop.resolution(),"AREA",area);
            if(next.isEmpty()||next.get().to()==null)
                return new Mapping(new Plan(false,"La resolución retiró el área de esta ruta. Crea una ruta nueva en el área vigente.",
                    h.catalogLabel(),last.toLabel(),h.areaName(),null,resolutions,List.of(),0),h.versionRoute(),null,null,Map.of());
            area=next.get().to();
        }
        String toArea=area;
        String toAreaName=jdbc.queryForObject("SELECT nombre FROM curriculo_area WHERE id=:id",Map.of("id",toArea),String.class);

        var used=jdbc.query("""
            SELECT n.unidad_id,COUNT(*) FROM aprendizaje_nodo_ruta n
            JOIN curriculo_unidad u ON u.id=n.unidad_id
            WHERE n.version_ruta_id=:route AND n.unidad_id IS NOT NULL
            GROUP BY n.unidad_id,u.numero_secuencia,u.titulo ORDER BY u.numero_secuencia,u.titulo""",
            Map.of("route",h.versionRoute()),(rs,n)->Map.entry(rs.getString(1),rs.getInt(2)));
        Map<String,String> units=new LinkedHashMap<>();
        List<UnitChange> changes=new ArrayList<>();
        int withoutUnit=0;
        for(var entry:used) {
            String unit=entry.getKey();String change="SE_MANTIENE";
            for(Hop hop:hops) {
                if(unit==null) break;
                var next=step(hop.resolution(),"UNIDAD",unit);
                if(next.isEmpty()||next.get().to()==null) { unit=null;change="RETIRADA"; }
                else { if("MODIFICADA".equals(next.get().change())) change="MODIFICADA"; unit=next.get().to(); }
            }
            if(unit!=null&&!valid(unit,last.to(),toArea,h.grade())) { unit=null;change="RETIRADA"; }
            String fromTitle=unitTitle(entry.getKey());String toTitle=unit==null?null:unitTitle(unit);
            if("SE_MANTIENE".equals(change)&&toTitle!=null&&!toTitle.equals(fromTitle)) change="MODIFICADA";
            if(unit==null) withoutUnit+=entry.getValue();
            units.put(entry.getKey(),unit);
            changes.add(new UnitChange(fromTitle,toTitle,change,entry.getValue()));
        }
        String message=withoutUnit==0?null:withoutUnit+(withoutUnit==1?" parada quedará":" paradas quedarán")
            +" en «Otras paradas» porque su unidad se retiró. Podrás asignarles otra unidad al editarlas.";
        return new Mapping(new Plan(true,message,h.catalogLabel(),last.toLabel(),h.areaName(),toAreaName,resolutions,changes,withoutUnit),
            h.versionRoute(),last.to(),toArea,units);
    }

    private boolean valid(String unit,String catalog,String area,String grade) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
            SELECT COUNT(*)>0 FROM curriculo_unidad WHERE id=:unit AND version_catalogo_id=:catalog AND area_id=:area AND grado_id=:grade""",
            new MapSqlParameterSource("unit",unit).addValue("catalog",catalog).addValue("area",area).addValue("grade",grade),Boolean.class));
    }
}
