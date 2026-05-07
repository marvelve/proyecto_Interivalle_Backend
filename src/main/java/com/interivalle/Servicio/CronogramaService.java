/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.interivalle.Servicio;

import com.interivalle.DTO.ActualizarCronogramaDetalleRequest;
import com.interivalle.DTO.CrearCronogramaRequest;
import com.interivalle.DTO.CronogramaDetalleVistaDTO;
import com.interivalle.DTO.CronogramaListResponse;
import com.interivalle.DTO.CronogramaResponse;
import com.interivalle.DTO.CronogramaVistaResponse;
import com.interivalle.DTO.FechaInicioDisponibleResponse;
import com.interivalle.DTO.SemanaCronogramaDTO;
import com.interivalle.Modelo.AvanceSemanal;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.CotizacionDetalle;
import com.interivalle.Modelo.CotizacionPersonalizada;
import com.interivalle.Modelo.Cronograma;
import com.interivalle.Modelo.CronogramaDetalle;
import com.interivalle.Modelo.CarpinteriaPersonalizada;
import com.interivalle.Modelo.MesonGranito;
import com.interivalle.Modelo.ObraBlanca;
import com.interivalle.Modelo.Solicitud;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Modelo.Vidrio;
import com.interivalle.Modelo.enums.EstadoActividadCronograma;
import com.interivalle.Modelo.enums.EstadoCronograma;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.CarpinteriaPersonalizadaRepositorio;
import com.interivalle.Repositorio.CotizacionRepositorio;
import com.interivalle.Repositorio.CotizacionPersonalizadaRepositorio;
import com.interivalle.Repositorio.CronogramaDetalleRepositorio;
import com.interivalle.Repositorio.CronogramaRepositorio;
import com.interivalle.Repositorio.AvanceSemanalRepositorio;
import com.interivalle.Repositorio.MesonGranitoRepositorio;
import com.interivalle.Repositorio.ObraBlancaRepositorio;
import com.interivalle.Repositorio.UsuarioRepositorio;
import com.interivalle.Repositorio.VidrioRepositorio;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
/**
 *
 * @author mary_
 */


@Service
public class CronogramaService {

    private static final int SERVICIO_CARPINTERIA = 2;
    private static final int SERVICIO_VIDRIO = 3;
    private static final int SERVICIO_MEZON = 4;
    private static final int MAX_PROYECTOS_POR_FECHA_INICIO = 5;

    @Autowired
    private CronogramaRepositorio cronogramaRepo;

    @Autowired
    private CronogramaDetalleRepositorio cronogramaDetalleRepo;

    @Autowired
    private AvanceSemanalRepositorio avanceSemanalRepo;

    @Autowired
    private CotizacionRepositorio cotizacionRepo;
    
    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @Autowired
    private CotizacionPersonalizadaRepositorio cotizacionPersonalizadaRepo;

    @Autowired
    private ObraBlancaRepositorio obraBlancaRepo;

    @Autowired
    private CarpinteriaPersonalizadaRepositorio carpinteriaPersonalizadaRepo;

    @Autowired
    private VidrioRepositorio vidrioRepo;

    @Autowired
    private MesonGranitoRepositorio mesonGranitoRepo;

    public CronogramaVistaResponse obtenerVistaPorCotizacion(Integer idCotizacion) {

        Cotizacion cotizacion = cotizacionRepo.findById(idCotizacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CotizaciÃ³n no encontrada"));

        Cronograma cronograma = cronogramaRepo.findByCotizacion_IdCotizacion(idCotizacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cronograma no encontrado para la cotizaciÃ³n"));

        sincronizarCronogramaConAvances(cronograma);

        List<CronogramaDetalle> detalles = cronogramaDetalleRepo.findByCronograma_IdCronogramaOrderBySemanaAsc(cronograma.getIdCronograma());

        CronogramaVistaResponse response = new CronogramaVistaResponse();
        response.setIdCronograma(cronograma.getIdCronograma());
        response.setIdCotizacion(cotizacion.getIdCotizacion());
        response.setEstadoCronograma(cronograma.getEstadoCronograma()!= null ? cronograma.getEstadoCronograma().name() : "EN_PROCESO");
        response.setFechaInicio(cronograma.getFechaInicio());
        response.setFechaFin(cronograma.getFechaFinEstimada());

        String nombreProyecto = obtenerNombreProyecto(cotizacion);
        response.setNombreProyecto(nombreProyecto);

        response.setSemanas(generarSemanas(cronograma.getFechaInicio(), detalles));
        response.setDetalles(mapearDetalles(detalles));
        response.setAvanceGeneral(obtenerAvanceGeneral(cronograma, detalles));

        return response;
    }

    private String obtenerNombreProyecto(Cotizacion cotizacion) {
        if (cotizacion.getSolicitud() != null) {
            Solicitud solicitud = cotizacion.getSolicitud();

            if (solicitud.getNombreProyectoUsuario() != null && !solicitud.getNombreProyectoUsuario().isBlank()) {
                return solicitud.getNombreProyectoUsuario();
            }

            if (solicitud.getNombreProyectoUsuario()!= null && !solicitud.getNombreProyectoUsuario().isBlank()) {
                return solicitud.getNombreProyectoUsuario();
            }
        }
        return "Sin nombre";
    }

    private List<SemanaCronogramaDTO> generarSemanas(LocalDate fechaInicioCronograma, List<CronogramaDetalle> detalles) {
        List<SemanaCronogramaDTO> semanas = new ArrayList<>();

        if (fechaInicioCronograma == null) {
            return semanas;
        }

        int maxSemana = detalles.stream()
                .map(CronogramaDetalle::getSemana)
                .filter(s -> s != null)
                .max(Comparator.naturalOrder())
                .orElse(0);

        if (maxSemana == 0) {
            return semanas;
        }

        LocalDate inicioPlanificado = fechaInicioCronograma;

        for (int i = 1; i <= maxSemana; i++) {
            LocalDate inicioSemana = inicioPlanificado.plusWeeks(i - 1);
            LocalDate finSemana = inicioSemana.plusDays(5); // lunes a sÃ¡bado
            semanas.add(new SemanaCronogramaDTO(i, inicioSemana, finSemana));
        }

        return semanas;
    }

    private LocalDate ajustarFechaInicioObra(LocalDate fecha) {
        if (fecha == null) return null;

        DayOfWeek dia = fecha.getDayOfWeek();
        if (dia != DayOfWeek.FRIDAY && dia != DayOfWeek.SATURDAY) {
            return fecha;
        }

        while (fecha.getDayOfWeek() != DayOfWeek.MONDAY) {
            fecha = fecha.plusDays(1);
        }
        return fecha;
    }

    public List<FechaInicioDisponibleResponse> listarFechasInicioDisponibles(Integer dias) {
        int diasConsulta = dias != null ? Math.max(1, Math.min(dias, 365)) : 365;
        LocalDate hoy = LocalDate.now();
        Set<LocalDate> fechasEvaluadas = new LinkedHashSet<>();
        List<FechaInicioDisponibleResponse> fechasDisponibles = new ArrayList<>();

        for (int i = 1; i <= diasConsulta; i++) {
            LocalDate fechaInicio = ajustarFechaInicioObra(hoy.plusDays(i));

            if (fechaInicio == null
                    || !fechaInicio.isAfter(hoy)
                    || fechaInicio.getDayOfWeek() == DayOfWeek.SUNDAY
                    || !fechasEvaluadas.add(fechaInicio)) {
                continue;
            }

            long cantidadProyectos = cronogramaRepo.countByFechaInicio(fechaInicio);
            boolean disponible = cantidadProyectos < MAX_PROYECTOS_POR_FECHA_INICIO;

            if (disponible) {
                fechasDisponibles.add(new FechaInicioDisponibleResponse(
                        fechaInicio,
                        cantidadProyectos,
                        disponible
                ));
            }
        }

        fechasDisponibles.sort(Comparator.comparing(FechaInicioDisponibleResponse::getFechaInicio));
        return fechasDisponibles;
    }

    private void validarFechaInicioDisponible(LocalDate fechaInicio) {
        if (fechaInicio == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio es obligatoria");
        }

        if (!fechaInicio.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio debe ser futura");
        }

        if (fechaInicio.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio no puede ser domingo");
        }

        long cantidadProyectos = cronogramaRepo.countByFechaInicio(fechaInicio);
        if (cantidadProyectos >= MAX_PROYECTOS_POR_FECHA_INICIO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La fecha de inicio seleccionada ya tiene 5 proyectos asociados. Selecciona otra fecha."
            );
        }
    }

    private List<CronogramaDetalleVistaDTO> mapearDetalles(List<CronogramaDetalle> detalles) {
        List<CronogramaDetalleVistaDTO> lista = new ArrayList<>();

        for (CronogramaDetalle d : detalles) {
            lista.add(mapearDetalleVista(d));
        }

        return lista;
    }

    private CronogramaDetalleVistaDTO mapearDetalleVista(CronogramaDetalle d) {
        CronogramaDetalleVistaDTO dto = new CronogramaDetalleVistaDTO();
        dto.setIdDetalle(d.getIdCronogramaDetalle());
        dto.setActividad(d.getActividad());
        dto.setDescripcion(d.getDescripcion());
        dto.setSemana(d.getSemana());
        dto.setEstado(normalizarEstadoDetalle(d.getEstadoActividad()));
        dto.setTrabajador(d.getTrabajadorAsignado());
        dto.setPorcentaje(d.getPorcentaje() != null ? d.getPorcentaje().intValue() : 0);
        dto.setNovedades(d.getNovedades());
        return dto;
    }

    @Transactional
    public CronogramaDetalleVistaDTO actualizarDetalleEnProceso(
            Integer idDetalle,
            ActualizarCronogramaDetalleRequest req
    ) {
        if (idDetalle == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El detalle del cronograma es obligatorio");
        }

        CronogramaDetalle detalle = cronogramaDetalleRepo.findById(idDetalle)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Detalle del cronograma no encontrado"));

        if (detalle.getEstadoActividad() != EstadoActividadCronograma.EN_PROCESO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden editar actividades en estado EN_PROCESO"
            );
        }
        detalle.setTrabajadorAsignado(limpiarTexto(req != null ? req.getTrabajador() : null));
        detalle.setNovedades(limpiarTexto(req != null ? req.getNovedades() : null));

        CronogramaDetalle guardado = cronogramaDetalleRepo.save(detalle);
        sincronizarObservacionesSeguimiento(guardado);

        return mapearDetalleVista(guardado);
    }

    private void sincronizarObservacionesSeguimiento(CronogramaDetalle detalle) {
        if (detalle == null || detalle.getCronograma() == null ||
                detalle.getCronograma().getIdCronograma() == null || detalle.getSemana() == null) {
            return;
        }

        Optional<AvanceSemanal> avanceOpt = avanceSemanalRepo
                .findByCronograma_IdCronogramaAndNumeroSemana(
                        detalle.getCronograma().getIdCronograma(),
                        detalle.getSemana()
                );

        if (avanceOpt.isEmpty()) {
            return;
        }

        List<CronogramaDetalle> detallesSemana = cronogramaDetalleRepo
                .findByCronograma_IdCronogramaAndSemana(
                        detalle.getCronograma().getIdCronograma(),
                        detalle.getSemana()
                );

        AvanceSemanal avance = avanceOpt.get();
        avance.setObservaciones(construirObservacionesSemana(detallesSemana));
        avanceSemanalRepo.save(avance);
    }

    private String construirObservacionesSemana(List<CronogramaDetalle> detallesSemana) {
        if (detallesSemana == null || detallesSemana.isEmpty()) {
            return null;
        }

        StringBuilder observaciones = new StringBuilder();

        for (CronogramaDetalle item : detallesSemana) {
            String novedad = limpiarTexto(item.getNovedades());
            if (novedad == null) {
                continue;
            }

            if (observaciones.length() > 0) {
                observaciones.append(System.lineSeparator());
            }

            observaciones
                    .append(textoPrincipal(item.getActividad(), item.getDescripcion(), "Actividad"))
                    .append(": ")
                    .append(novedad);
        }

        return observaciones.length() > 0 ? observaciones.toString() : null;
    }

    private String limpiarTexto(String texto) {
        if (texto == null) {
            return null;
        }

        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private Integer calcularAvanceGeneral(List<CronogramaDetalle> detalles) {
        if (detalles == null || detalles.isEmpty()) {
            return 0;
        }

        int suma = detalles.stream()
                .map(d -> d.getPorcentaje() != null ? d.getPorcentaje().intValue() : 0)
                .reduce(0, Integer::sum);

        return suma / detalles.size();
    }

    private Integer obtenerAvanceGeneral(Cronograma cronograma, List<CronogramaDetalle> detalles) {
        if (cronograma != null && cronograma.getAvanceGeneral() != null) {
            return cronograma.getAvanceGeneral().intValue();
        }

        return calcularAvanceGeneral(detalles);
    }

    private String normalizarEstadoDetalle(EstadoActividadCronograma estado) {
        if (estado == null) {
            return "PENDIENTE";
        }

        if (estado == EstadoActividadCronograma.TERMINADA) {
            return "COMPLETADA";
        }

        return estado.toString();
    }

    private void sincronizarCronogramaConAvances(Cronograma cronograma) {
        if (cronograma == null || cronograma.getIdCronograma() == null) {
            return;
        }

        List<AvanceSemanal> avances = avanceSemanalRepo
                .findByCronograma_IdCronogramaOrderByNumeroSemanaAsc(cronograma.getIdCronograma());

        if (avances == null || avances.isEmpty()) {
            return;
        }

        BigDecimal sumaSemanas = BigDecimal.ZERO;

        for (AvanceSemanal avance : avances) {
            BigDecimal porcentajeSemana = avance.getPorcentajeSemana() != null
                    ? avance.getPorcentajeSemana()
                    : BigDecimal.ZERO;

            sumaSemanas = sumaSemanas.add(porcentajeSemana);

            List<CronogramaDetalle> detallesSemana = cronogramaDetalleRepo
                    .findByCronograma_IdCronogramaAndSemana(
                            cronograma.getIdCronograma(),
                            avance.getNumeroSemana()
                    );

            if (detallesSemana == null || detallesSemana.isEmpty()) {
                continue;
            }

            EstadoActividadCronograma estadoSemana = estadoDetallePorPorcentaje(porcentajeSemana);

            for (CronogramaDetalle detalle : detallesSemana) {
                detalle.setPorcentaje(porcentajeSemana);
                detalle.setEstadoActividad(estadoSemana);
            }

            cronogramaDetalleRepo.saveAll(detallesSemana);
        }

        Integer totalSemanas = cronograma.getTotalSemanas();
        if (totalSemanas == null || totalSemanas <= 0) {
            totalSemanas = avances.stream()
                    .map(AvanceSemanal::getNumeroSemana)
                    .filter(semana -> semana != null)
                    .max(Integer::compareTo)
                    .orElse(1);
        }

        BigDecimal avanceGeneral = sumaSemanas.divide(
                BigDecimal.valueOf(totalSemanas),
                2,
                java.math.RoundingMode.HALF_UP
        );

        if (avanceGeneral.compareTo(new BigDecimal("100")) > 0) {
            avanceGeneral = new BigDecimal("100.00");
        }

        cronograma.setAvanceGeneral(avanceGeneral);
        cronograma.setEstado(avanceGeneral.compareTo(new BigDecimal("100")) >= 0 ? "FINALIZADO" : "EN_PROCESO");
        cronograma.setEstadoCronograma(
                avanceGeneral.compareTo(new BigDecimal("100")) >= 0
                        ? EstadoCronograma.FINALIZADO
                        : EstadoCronograma.EN_PROCESO
        );

        cronogramaRepo.save(cronograma);
    }

    private EstadoActividadCronograma estadoDetallePorPorcentaje(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.compareTo(BigDecimal.ZERO) <= 0) {
            return EstadoActividadCronograma.PENDIENTE;
        }

        if (porcentaje.compareTo(new BigDecimal("100")) >= 0) {
            return EstadoActividadCronograma.TERMINADA;
        }

        return EstadoActividadCronograma.EN_PROCESO;
    }
    
    @Transactional
    public CronogramaResponse crearDesdeCotizacionAprobada(Integer idCotizacion, LocalDate fechaInicio) {

    if (idCotizacion == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cotizaciÃ³n es obligatoria");
    }

    if (fechaInicio == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio es obligatoria");
    }

    CrearCronogramaRequest req = new CrearCronogramaRequest();
    req.setIdCotizacion(idCotizacion);
    req.setFechaInicio(fechaInicio);

    return crearCronograma(req);
}

    @Transactional
    public CronogramaResponse crearCronograma(CrearCronogramaRequest req) {

        if (req == null || req.getIdCotizacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cotizaciÃ³n es obligatoria");
        }

        if (req.getFechaInicio() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de inicio es obligatoria");
        }

        Cotizacion cotizacion = cotizacionRepo.findById(req.getIdCotizacion())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "CotizaciÃ³n no encontrada"));


        Optional<Cronograma> existente = cronogramaRepo.findByCotizacion_IdCotizacion(req.getIdCotizacion());
        if (existente.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ya existe un cronograma para esta cotizaciÃ³n"
            );
        }

        LocalDate inicioPlanificado = ajustarFechaInicioObra(req.getFechaInicio());
        validarFechaInicioDisponible(inicioPlanificado);

        Cronograma cronograma = new Cronograma();
        cronograma.setCotizacion(cotizacion);
        cronograma.setFechaInicio(inicioPlanificado);
        cronograma.setFechaInicioPlanificada(inicioPlanificado);

        List<CotizacionDetalle> detallesCotizacion = cotizacion.getDetalles();

        int maxSemanaActividades = 0;
        if (detallesCotizacion != null && !detallesCotizacion.isEmpty()) {
            maxSemanaActividades = detallesCotizacion.stream()
                    .filter(det -> det.getTipoItem() == TipoItemCotizacion.ACTIVIDAD)
                    .map(CotizacionDetalle::getSemana)
                    .filter(s -> s != null)
                    .max(Integer::compareTo)
                    .orElse(0);
        }

        int totalSemanas = Math.max(maxSemanaActividades, 0);
        totalSemanas += semanasProductos(detallesCotizacion);
        totalSemanas += contarActividadesAdicionales(cotizacion.getIdCotizacion());
        if (totalSemanas <= 0) {
            totalSemanas = 1;
        }
        cronograma.setTotalSemanas(totalSemanas);

        cronograma.setFechaFinEstimada(
                inicioPlanificado.plusWeeks(totalSemanas - 1).plusDays(5)
        );

        if (cronograma.getEstadoCronograma() == null) {
            cronograma.setEstadoCronograma(EstadoCronograma.EN_PROCESO);
        }

        Cronograma guardado = cronogramaRepo.save(cronograma);

        if (detallesCotizacion != null && !detallesCotizacion.isEmpty()) {
            for (CotizacionDetalle det : detallesCotizacion) {

                if (det.getTipoItem() == null || !"ACTIVIDAD".equals(det.getTipoItem().name())) {
            continue;
        }
                Integer semanaDetalle = (det.getSemana() != null && det.getSemana() > 0)
                        ? det.getSemana()
                        : 1;

                LocalDate fechaInicioSemana = inicioPlanificado.plusWeeks(semanaDetalle - 1);
                LocalDate fechaFinSemana = fechaInicioSemana.plusDays(5);

                CronogramaDetalle cd = new CronogramaDetalle();
                cd.setCronograma(guardado);

                cd.setServicio(
                        det.getServicio() != null
                                ? det.getServicio().getNombreServicio()
                                : "Sin servicio"
                );

                cd.setActividad(
                    det.getDescripcion() != null && !det.getDescripcion().isBlank()
                ? det.getDescripcion()
                : "Actividad"
                );

                cd.setDescripcion(det.getDescripcion());
                cd.setSemana(semanaDetalle);
                cd.setFechaInicioSemana(fechaInicioSemana);
                cd.setFechaFinSemana(fechaFinSemana);
                cd.setTrabajadorAsignado(null);
                cd.setEstadoActividad(EstadoActividadCronograma.PENDIENTE);
                cd.setPorcentaje(BigDecimal.ZERO);
                cd.setNovedades(null);

                cronogramaDetalleRepo.save(cd);
            }
        }

        int siguienteSemana = Math.max(maxSemanaActividades, 0);
        siguienteSemana = agregarSemanasProductos(guardado, detallesCotizacion, inicioPlanificado, siguienteSemana);
        agregarActividadesAdicionales(guardado, cotizacion.getIdCotizacion(), inicioPlanificado, siguienteSemana);

        CronogramaResponse response = new CronogramaResponse();
        response.setIdCronograma(guardado.getIdCronograma());
        response.setFechaInicio(guardado.getFechaInicio());
        response.setFechaFinEstimada(guardado.getFechaFinEstimada());
        response.setEstadoCronograma(
                guardado.getEstadoCronograma() != null
                        ? guardado.getEstadoCronograma().name()
                        : "EN_PROCESO"
        );

        return response;
    }

    private int semanasProductos(List<CotizacionDetalle> detallesCotizacion) {
        int semanas = 0;

        if (tieneProductoServicio(detallesCotizacion, SERVICIO_CARPINTERIA, "CARPINTER")) {
            semanas += 2;
        }

        if (tieneProductoServicio(detallesCotizacion, SERVICIO_VIDRIO, "VIDRIO")) {
            semanas += 1;
        }

        if (tieneProductoServicio(detallesCotizacion, SERVICIO_MEZON, "MESON", "MARMOL", "GRANITO")) {
            semanas += 1;
        }

        return semanas;
    }

    private int agregarSemanasProductos(
            Cronograma cronograma,
            List<CotizacionDetalle> detallesCotizacion,
            LocalDate inicioPlanificado,
            int semanaActual
    ) {
        if (tieneProductoServicio(detallesCotizacion, SERVICIO_CARPINTERIA, "CARPINTER")) {
            semanaActual++;
            crearDetalleCronograma(
                    cronograma,
                    "Carpinteria",
                    "Carpinteria - Semana 1",
                    "Instalacion de productos de carpinteria",
                    semanaActual,
                    inicioPlanificado
            );

            semanaActual++;
            crearDetalleCronograma(
                    cronograma,
                    "Carpinteria",
                    "Carpinteria - Semana 2",
                    "Instalacion de productos de carpinteria",
                    semanaActual,
                    inicioPlanificado
            );
        }

        if (tieneProductoServicio(detallesCotizacion, SERVICIO_VIDRIO, "VIDRIO")) {
            semanaActual++;
            crearDetalleCronograma(
                    cronograma,
                    "Divisiones en Vidrio",
                    "Divisiones en Vidrio",
                    "Instalacion de divisiones en vidrio",
                    semanaActual,
                    inicioPlanificado
            );
        }

        if (tieneProductoServicio(detallesCotizacion, SERVICIO_MEZON, "MESON", "MARMOL", "GRANITO")) {
            semanaActual++;
            crearDetalleCronograma(
                    cronograma,
                    "Mesones en Marmol",
                    "Mesones en Marmol",
                    "Instalacion de mesones en marmol",
                    semanaActual,
                    inicioPlanificado
            );
        }

        return semanaActual;
    }

    private boolean tieneProductoServicio(List<CotizacionDetalle> detallesCotizacion, Integer idServicio, String... palabrasClave) {
        if (detallesCotizacion == null || detallesCotizacion.isEmpty()) {
            return false;
        }

        for (CotizacionDetalle det : detallesCotizacion) {
            if (det.getTipoItem() != TipoItemCotizacion.PRODUCTO) {
                continue;
            }

            Integer servicioId = det.getServicio() != null ? det.getServicio().getIdServicio() : null;
            if (idServicio.equals(servicioId)) {
                return true;
            }

            String texto = normalizarTexto(
                    (det.getServicio() != null ? det.getServicio().getNombreServicio() : "") + " " +
                    (det.getCategoria() != null ? det.getCategoria() : "") + " " +
                    (det.getDescripcion() != null ? det.getDescripcion() : "") + " " +
                    (det.getActividadMaterial() != null ? det.getActividadMaterial() : "")
            );

            for (String palabra : palabrasClave) {
                if (texto.contains(normalizarTexto(palabra))) {
                    return true;
                }
            }
        }

        return false;
    }

    private int contarActividadesAdicionales(Integer idCotizacion) {
        Optional<CotizacionPersonalizada> personalizada =
                cotizacionPersonalizadaRepo.findTopByCotizacion_IdCotizacionOrderByIdCotizacionPersonalizadaDesc(idCotizacion);

        if (personalizada.isEmpty()) {
            return 0;
        }

        Integer idPersonalizada = personalizada.get().getIdCotizacionPersonalizada();
        return obraBlancaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada).size()
                + carpinteriaPersonalizadaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada).size()
                + vidrioRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada).size()
                + mesonGranitoRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada).size();
    }

    private int agregarActividadesAdicionales(
            Cronograma cronograma,
            Integer idCotizacion,
            LocalDate inicioPlanificado,
            int semanaActual
    ) {
        Optional<CotizacionPersonalizada> personalizada =
                cotizacionPersonalizadaRepo.findTopByCotizacion_IdCotizacionOrderByIdCotizacionPersonalizadaDesc(idCotizacion);

        if (personalizada.isEmpty()) {
            return semanaActual;
        }

        Integer idPersonalizada = personalizada.get().getIdCotizacionPersonalizada();

        for (ObraBlanca item : obraBlancaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada)) {
            semanaActual++;
            crearDetalleCronograma(
                    cronograma,
                    "Actividad adicional",
                    textoPrincipal(item.getActividad(), item.getDescripcion(), "Actividad adicional"),
                    descripcionAdicional(item.getDescripcion(), item.getLugar()),
                    semanaActual,
                    inicioPlanificado
            );
        }

        for (CarpinteriaPersonalizada item : carpinteriaPersonalizadaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada)) {
            semanaActual++;
            crearDetalleCronograma(
                    cronograma,
                    "Carpinteria adicional",
                    textoPrincipal(item.getTipoMueble(), item.getDescripcion(), "Carpinteria adicional"),
                    descripcionAdicional(item.getDescripcion(), item.getMaterial()),
                    semanaActual,
                    inicioPlanificado
            );
        }

        for (Vidrio item : vidrioRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada)) {
            semanaActual++;
            crearDetalleCronograma(
                    cronograma,
                    "Vidrio adicional",
                    textoPrincipal(item.getTipoVidrio(), item.getDescripcion(), "Vidrio adicional"),
                    item.getDescripcion(),
                    semanaActual,
                    inicioPlanificado
            );
        }

        for (MesonGranito item : mesonGranitoRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada)) {
            semanaActual++;
            crearDetalleCronograma(
                    cronograma,
                    "Meson marmol adicional",
                    textoPrincipal(item.getTipoGranito(), item.getDescripcion(), "Meson marmol adicional"),
                    item.getDescripcion(),
                    semanaActual,
                    inicioPlanificado
            );
        }

        return semanaActual;
    }

    private CronogramaDetalle crearDetalleCronograma(
            Cronograma cronograma,
            String servicio,
            String actividad,
            String descripcion,
            Integer semana,
            LocalDate inicioPlanificado
    ) {
        LocalDate fechaInicioSemana = inicioPlanificado.plusWeeks(semana - 1);
        LocalDate fechaFinSemana = fechaInicioSemana.plusDays(5);

        CronogramaDetalle detalle = new CronogramaDetalle();
        detalle.setCronograma(cronograma);
        detalle.setServicio(servicio);
        detalle.setActividad(textoPrincipal(actividad, descripcion, "Actividad"));
        detalle.setDescripcion(descripcion);
        detalle.setSemana(semana);
        detalle.setFechaInicioSemana(fechaInicioSemana);
        detalle.setFechaFinSemana(fechaFinSemana);
        detalle.setTrabajadorAsignado(null);
        detalle.setEstadoActividad(EstadoActividadCronograma.PENDIENTE);
        detalle.setPorcentaje(BigDecimal.ZERO);
        detalle.setNovedades(null);

        return cronogramaDetalleRepo.save(detalle);
    }

    private String descripcionAdicional(String descripcion, String lugar) {
        if (textoTieneValor(descripcion) && textoTieneValor(lugar)) {
            return descripcion.trim() + " - " + lugar.trim();
        }

        if (textoTieneValor(descripcion)) {
            return descripcion.trim();
        }

        return textoTieneValor(lugar) ? lugar.trim() : null;
    }

    private String textoPrincipal(String principal, String respaldo, String defecto) {
        if (textoTieneValor(principal)) {
            return principal.trim();
        }

        if (textoTieneValor(respaldo)) {
            return respaldo.trim();
        }

        return defecto;
    }

    private boolean textoTieneValor(String texto) {
        return texto != null && !texto.trim().isEmpty();
    }

    private String normalizarTexto(String texto) {
        return texto == null
                ? ""
                : texto.trim().toUpperCase()
                        .replace("Ã", "A")
                        .replace("Ã‰", "E")
                        .replace("Ã", "I")
                        .replace("Ã“", "O")
                        .replace("Ãš", "U")
                        .replace("Ã‘", "N");
    }
    
    
     public List<CronogramaListResponse> listarCronogramasPorUsuario(String correoUsuario) {
        Usuario usuario = usuarioRepo.findByCorreoUsuario(correoUsuario)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado"
                ));

        List<Cronograma> cronogramas;

        if (usuario.getIdRol() == 1 || usuario.getIdRol() == 2) {
            cronogramas = cronogramaRepo.findAllByOrderByIdCronogramaDesc();
        } else {
            cronogramas = cronogramaRepo
                    .findByCotizacion_Solicitud_Usuario_IdUsuarioOrderByIdCronogramaDesc(usuario.getIdUsuario());
        }

        return cronogramas.stream()
                .map(this::mapToListResponse)
                .toList();
    }

    private CronogramaListResponse mapToListResponse(Cronograma cronograma) {
        CronogramaListResponse res = new CronogramaListResponse();

        res.setId(cronograma.getIdCronograma());
        res.setIdCronograma(cronograma.getIdCronograma());

        Cotizacion cotizacion = cronograma.getCotizacion();

        if (cotizacion != null) {
            res.setIdCotizacion(cotizacion.getIdCotizacion());

            if (cotizacion.getSolicitud() != null) {
                res.setNombreProyecto(
                        cotizacion.getSolicitud().getNombreProyectoUsuario()
                );

                if (cotizacion.getSolicitud().getUsuario() != null) {
                    res.setNombreCliente(
                            cotizacion.getSolicitud().getUsuario().getNombreUsuario()
                    );
                }
            }
        }

        res.setEstadoCronograma(
                cronograma.getEstadoCronograma() != null
                        ? cronograma.getEstadoCronograma().name()
                        : null
        );

        res.setFechaInicio(cronograma.getFechaInicio());
        res.setFechaFin(cronograma.getFechaFinEstimada());

        res.setAvanceGeneral(
                cronograma.getAvanceGeneral() != null
                        ? cronograma.getAvanceGeneral().intValue()
                        : calcularAvanceGeneral(cronograma)
        );

        return res;
    }

    private Integer calcularAvanceGeneral(Cronograma cronograma) {
        if (cronograma.getDetalles() == null || cronograma.getDetalles().isEmpty()) {
            return 0;
        }

        int cantidadConPorcentaje = 0;
        int suma = 0;

        for (CronogramaDetalle detalle : cronograma.getDetalles()) {
            if (detalle.getPorcentaje() != null) {
                suma += detalle.getPorcentaje().intValue();
                cantidadConPorcentaje++;
            }
        }

        if (cantidadConPorcentaje == 0) {
            return 0;
        }

        return Math.round((float) suma / cantidadConPorcentaje);
    }
}
