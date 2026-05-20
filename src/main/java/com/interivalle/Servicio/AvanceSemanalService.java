/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.interivalle.Servicio;

import com.interivalle.DTO.AvanceSemanalRequest;
import com.interivalle.DTO.AvanceSemanalResponse;
import com.interivalle.DTO.ComentarioAvanceRequest;
import com.interivalle.DTO.ComentarioAvanceResponse;
import com.interivalle.Modelo.AvanceSemanal;
import com.interivalle.Modelo.ComentarioAvance;
import com.interivalle.Modelo.Cronograma;
import com.interivalle.Modelo.CronogramaDetalle;
import com.interivalle.Modelo.EvidenciaAvance;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Modelo.enums.EstadoActividadCronograma;
import com.interivalle.Modelo.enums.EstadoCronograma;
import com.interivalle.Modelo.enums.ModuloNotificacion;
import com.interivalle.Modelo.enums.TipoNotificacion;
import com.interivalle.Repositorio.AvanceSemanalRepositorio;
import com.interivalle.Repositorio.ComentarioAvanceRepositorio;
import com.interivalle.Repositorio.CronogramaDetalleRepositorio;
import com.interivalle.Repositorio.CronogramaRepositorio;
import com.interivalle.Repositorio.EvidenciaAvanceRepositorio;
import com.interivalle.Repositorio.UsuarioRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 *
 * @author mary_
 */

@Service
public class AvanceSemanalService {

    @Autowired
    private AvanceSemanalRepositorio avanceRepo;

    @Autowired
    private ComentarioAvanceRepositorio comentarioRepo;

    @Autowired
    private EvidenciaAvanceRepositorio evidenciaRepo;

    @Autowired
    private CronogramaRepositorio cronogramaRepo;

    @Autowired
    private CronogramaDetalleRepositorio cronogramaDetalleRepo;

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @Autowired
    private NotificacionService notificacionService;

    @Transactional
    public AvanceSemanalResponse registrarAvance(AvanceSemanalRequest req, Integer idUsuario) {
        ResultadoGuardado resultado = guardarAvance(req, idUsuario, null);
        notificarCliente(resultado.avance, resultado.nuevo);
        return mapToResponse(resultado.avance);
    }

    @Transactional
    public AvanceSemanalResponse actualizarAvance(Integer idAvance, AvanceSemanalRequest req, Integer idUsuario) {
        if (idAvance == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El avance es obligatorio");
        }

        AvanceSemanal avanceActual = avanceRepo.findById(idAvance)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avance no encontrado"));

        AvanceSemanalRequest request = completarRequestActualizacion(req, avanceActual);
        ResultadoGuardado resultado = guardarAvance(request, idUsuario, avanceActual);
        notificarCliente(resultado.avance, false);

        return mapToResponse(resultado.avance);
    }

    @Transactional
    public List<AvanceSemanalResponse> listarPorCronograma(Integer idCronograma) {
        if (idCronograma == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cronograma es obligatorio");
        }

        consolidarAvancesDuplicados(idCronograma);

        Cronograma cronograma = cronogramaRepo.findById(idCronograma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cronograma no encontrado"));

        List<AvanceSemanal> avancesUnicos = recalcularPorcentajesAvances(cronograma);
        actualizarCronograma(cronograma, obtenerPorcentajeGeneralCronograma(avancesUnicos, obtenerTotalSemanasCalculo(cronograma)));

        return avancesUnicos.stream()
                .sorted(Comparator.comparing(AvanceSemanal::getNumeroSemana))
                .map(this::mapToResponse)
                .toList();
    }

    public AvanceSemanalResponse obtenerPorId(Integer idAvance) {
        AvanceSemanal avance = avanceRepo.findById(idAvance)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avance no encontrado"));

        return mapToResponse(avance);
    }

    @Transactional
    public ComentarioAvanceResponse comentarAvance(ComentarioAvanceRequest req, Integer idUsuario) {
        if (req == null || req.getIdAvance() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El avance es obligatorio");
        }

        if (req.getComentario() == null || req.getComentario().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario es obligatorio");
        }

        AvanceSemanal avance = avanceRepo.findById(req.getIdAvance())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avance no encontrado"));

        Usuario usuario = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        ComentarioAvance comentario = new ComentarioAvance();
        comentario.setAvanceSemanal(avance);
        comentario.setUsuario(usuario);
        comentario.setComentario(req.getComentario().trim());
        comentario.setFechaComentario(LocalDateTime.now());

        ComentarioAvance guardado = comentarioRepo.save(comentario);
        notificarSupervisoresComentarioCliente(avance, usuario, guardado);

        return mapComentarioToResponse(guardado);
    }

    public List<ComentarioAvanceResponse> listarComentarios(Integer idAvance) {
        return comentarioRepo.findByAvanceSemanal_IdAvanceOrderByFechaComentarioAsc(idAvance)
                .stream()
                .map(this::mapComentarioToResponse)
                .toList();
    }

    private ResultadoGuardado guardarAvance(
            AvanceSemanalRequest req,
            Integer idUsuario,
            AvanceSemanal avanceActual
    ) {
        Cronograma cronograma = validarYObtenerCronograma(req);
        AvanceSemanal avance = resolverAvanceUnico(cronograma, req.getNumeroSemana(), avanceActual);
        boolean nuevo = avance.getIdAvance() == null;

        avance.setCronograma(cronograma);
        avance.setNumeroSemana(req.getNumeroSemana());
        avance.setFechaRegistro(LocalDateTime.now());
        avance.setTitulo(limpiarTexto(req.getTitulo()));
        avance.setDescripcion(limpiarTexto(req.getDescripcion()));
        avance.setObservaciones(limpiarTexto(req.getObservaciones()));
        avance.setPorcentajeSemana(req.getPorcentajeSemana());
        avance.setRegistradoPor(idUsuario);
        avance.setEstado(determinarEstadoSemana(req.getPorcentajeSemana()));

        AvanceSemanal guardado = avanceRepo.save(avance);

        eliminarDuplicadosSemana(cronograma.getIdCronograma(), req.getNumeroSemana(), guardado);
        sincronizarDetalleCronograma(cronograma.getIdCronograma(), req.getNumeroSemana(), req.getPorcentajeSemana());

        List<AvanceSemanal> avancesRecalculados = recalcularPorcentajesAvances(cronograma);
        BigDecimal porcentajeGeneral = obtenerPorcentajeGeneralCronograma(
                avancesRecalculados,
                obtenerTotalSemanasCalculo(cronograma)
        );
        actualizarCronograma(cronograma, porcentajeGeneral);

        AvanceSemanal avanceRespuesta = avancesRecalculados.stream()
                .filter(item -> item.getIdAvance() != null && item.getIdAvance().equals(guardado.getIdAvance()))
                .findFirst()
                .orElse(guardado);

        return new ResultadoGuardado(avanceRespuesta, nuevo);
    }

    private Cronograma validarYObtenerCronograma(AvanceSemanalRequest req) {
        if (req == null || req.getIdCronograma() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cronograma es obligatorio");
        }

        if (req.getNumeroSemana() == null || req.getNumeroSemana() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La semana es obligatoria");
        }

        if (req.getPorcentajeSemana() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El porcentaje semanal es obligatorio");
        }

        if (req.getPorcentajeSemana().compareTo(BigDecimal.ZERO) < 0
                || req.getPorcentajeSemana().compareTo(new BigDecimal("100")) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El porcentaje semanal debe estar entre 0 y 100");
        }

        Cronograma cronograma = cronogramaRepo.findById(req.getIdCronograma())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cronograma no encontrado"));

        int totalSemanas = obtenerTotalSemanasCalculo(cronograma);

        if (totalSemanas <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cronograma no tiene total de semanas definido");
        }

        if (req.getNumeroSemana() > totalSemanas) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La semana ingresada supera el total de semanas del cronograma");
        }

        return cronograma;
    }

    private AvanceSemanalRequest completarRequestActualizacion(AvanceSemanalRequest req, AvanceSemanal avanceActual) {
        AvanceSemanalRequest request = req != null ? req : new AvanceSemanalRequest();

        if (request.getIdCronograma() == null) {
            request.setIdCronograma(avanceActual.getCronograma().getIdCronograma());
        }

        if (request.getNumeroSemana() == null) {
            request.setNumeroSemana(avanceActual.getNumeroSemana());
        }

        if (request.getTitulo() == null) {
            request.setTitulo(avanceActual.getTitulo());
        }

        if (request.getDescripcion() == null) {
            request.setDescripcion(avanceActual.getDescripcion());
        }

        if (request.getObservaciones() == null) {
            request.setObservaciones(avanceActual.getObservaciones());
        }

        if (request.getPorcentajeSemana() == null) {
            request.setPorcentajeSemana(avanceActual.getPorcentajeSemana());
        }

        return request;
    }

    private AvanceSemanal resolverAvanceUnico(
            Cronograma cronograma,
            Integer numeroSemana,
            AvanceSemanal avanceActual
    ) {
        List<AvanceSemanal> avancesSemana = avanceRepo
                .findByCronograma_IdCronogramaAndNumeroSemanaOrderByIdAvanceAsc(
                        cronograma.getIdCronograma(),
                        numeroSemana
                );

        if (!avancesSemana.isEmpty()) {
            return avancesSemana.get(0);
        }

        if (avanceActual != null) {
            return avanceActual;
        }

        return new AvanceSemanal();
    }

    private List<AvanceSemanal> consolidarAvancesDuplicados(Integer idCronograma) {
        List<AvanceSemanal> avances = avanceRepo.findByCronograma_IdCronogramaOrderByNumeroSemanaAsc(idCronograma);
        Map<Integer, List<AvanceSemanal>> avancesPorSemana = new LinkedHashMap<>();

        for (AvanceSemanal avance : avances) {
            if (avance.getNumeroSemana() == null) {
                continue;
            }

            avancesPorSemana
                    .computeIfAbsent(avance.getNumeroSemana(), semana -> new ArrayList<>())
                    .add(avance);
        }

        List<AvanceSemanal> avancesUnicos = new ArrayList<>();

        for (List<AvanceSemanal> grupo : avancesPorSemana.values()) {
            grupo.sort(Comparator.comparing(AvanceSemanal::getIdAvance));
            AvanceSemanal principal = grupo.get(0);

            if (grupo.size() > 1) {
                AvanceSemanal masAvanzado = seleccionarAvanceMasRepresentativo(grupo);

                if (!principal.getIdAvance().equals(masAvanzado.getIdAvance())) {
                    copiarDatosAvance(masAvanzado, principal);
                    principal = avanceRepo.save(principal);
                }

                for (AvanceSemanal duplicado : grupo) {
                    if (!duplicado.getIdAvance().equals(principal.getIdAvance())) {
                        moverRelacionesAvance(duplicado, principal);
                        avanceRepo.delete(duplicado);
                    }
                }
            }

            avancesUnicos.add(principal);
        }

        return avancesUnicos;
    }

    private AvanceSemanal seleccionarAvanceMasRepresentativo(List<AvanceSemanal> avances) {
        return avances.stream()
                .max(Comparator
                        .comparing((AvanceSemanal avance) -> valorPorcentaje(avance.getPorcentajeSemana()))
                        .thenComparing(avance -> avance.getFechaRegistro() != null
                                ? avance.getFechaRegistro()
                                : LocalDateTime.MIN)
                        .thenComparing(AvanceSemanal::getIdAvance))
                .orElse(avances.get(0));
    }

    private void copiarDatosAvance(AvanceSemanal origen, AvanceSemanal destino) {
        destino.setFechaRegistro(origen.getFechaRegistro() != null ? origen.getFechaRegistro() : LocalDateTime.now());
        destino.setTitulo(origen.getTitulo());
        destino.setDescripcion(origen.getDescripcion());
        destino.setObservaciones(origen.getObservaciones());
        destino.setPorcentajeSemana(origen.getPorcentajeSemana());
        destino.setPorcentajeGeneral(origen.getPorcentajeGeneral());
        destino.setRegistradoPor(origen.getRegistradoPor());
        destino.setEstado(determinarEstadoSemana(origen.getPorcentajeSemana()));
    }

    private void eliminarDuplicadosSemana(Integer idCronograma, Integer numeroSemana, AvanceSemanal principal) {
        List<AvanceSemanal> avancesSemana = avanceRepo
                .findByCronograma_IdCronogramaAndNumeroSemanaOrderByIdAvanceAsc(idCronograma, numeroSemana);

        for (AvanceSemanal duplicado : avancesSemana) {
            if (!duplicado.getIdAvance().equals(principal.getIdAvance())) {
                moverRelacionesAvance(duplicado, principal);
                avanceRepo.delete(duplicado);
            }
        }
    }

    private void moverRelacionesAvance(AvanceSemanal origen, AvanceSemanal destino) {
        if (origen.getIdAvance() == null || destino.getIdAvance() == null
                || origen.getIdAvance().equals(destino.getIdAvance())) {
            return;
        }

        List<ComentarioAvance> comentarios = comentarioRepo
                .findByAvanceSemanal_IdAvanceOrderByFechaComentarioAsc(origen.getIdAvance());
        for (ComentarioAvance comentario : comentarios) {
            comentario.setAvanceSemanal(destino);
        }
        comentarioRepo.saveAll(comentarios);

        List<EvidenciaAvance> evidencias = evidenciaRepo
                .findByAvanceSemanal_IdAvanceOrderByIdEvidenciaAsc(origen.getIdAvance());
        for (EvidenciaAvance evidencia : evidencias) {
            evidencia.setAvanceSemanal(destino);
        }
        evidenciaRepo.saveAll(evidencias);
    }

    private BigDecimal calcularPorcentajeGeneral(
            Integer idCronograma,
            Integer numeroSemanaActual,
            BigDecimal porcentajeSemanaActual,
            Integer totalSemanas
    ) {
        if (totalSemanas == null || totalSemanas <= 0) {
            totalSemanas = 1;
        }

        List<AvanceSemanal> avances = avanceRepo.findByCronograma_IdCronogramaOrderByNumeroSemanaAsc(idCronograma);
        Map<Integer, BigDecimal> porcentajesPorSemana = new LinkedHashMap<>();

        for (AvanceSemanal avance : avances) {
            Integer semana = avance.getNumeroSemana();
            if (semana == null) {
                continue;
            }

            if (numeroSemanaActual != null && semana > numeroSemanaActual) {
                continue;
            }

            BigDecimal porcentaje = semana.equals(numeroSemanaActual)
                    ? porcentajeSemanaActual
                    : valorPorcentaje(avance.getPorcentajeSemana());

            porcentajesPorSemana.merge(semana, porcentaje, this::mayorPorcentaje);
        }

        if (numeroSemanaActual != null && !porcentajesPorSemana.containsKey(numeroSemanaActual)) {
            porcentajesPorSemana.put(numeroSemanaActual, porcentajeSemanaActual);
        }

        BigDecimal suma = porcentajesPorSemana.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal porcentajeGeneral = suma.divide(
                BigDecimal.valueOf(totalSemanas),
                2,
                RoundingMode.HALF_UP
        );

        if (porcentajeGeneral.compareTo(new BigDecimal("100")) > 0) {
            return new BigDecimal("100.00");
        }

        return porcentajeGeneral;
    }

    private List<AvanceSemanal> recalcularPorcentajesAvances(Cronograma cronograma) {
        if (cronograma == null || cronograma.getIdCronograma() == null) {
            return new ArrayList<>();
        }

        Integer totalSemanas = obtenerTotalSemanasCalculo(cronograma);
        List<AvanceSemanal> avances = avanceRepo
                .findByCronograma_IdCronogramaOrderByNumeroSemanaAsc(cronograma.getIdCronograma());

        for (AvanceSemanal avance : avances) {
            BigDecimal porcentajeGeneral = calcularPorcentajeGeneral(
                    cronograma.getIdCronograma(),
                    avance.getNumeroSemana(),
                    avance.getPorcentajeSemana(),
                    totalSemanas
            );

            avance.setPorcentajeGeneral(porcentajeGeneral);
            avance.setEstado(determinarEstadoSemana(avance.getPorcentajeSemana()));
        }

        return avanceRepo.saveAll(avances).stream()
                .sorted(Comparator.comparing(AvanceSemanal::getNumeroSemana))
                .toList();
    }

    private BigDecimal obtenerPorcentajeGeneralCronograma(List<AvanceSemanal> avances, Integer totalSemanas) {
        if (totalSemanas == null || totalSemanas <= 0) {
            totalSemanas = 1;
        }

        Map<Integer, BigDecimal> porcentajesPorSemana = new LinkedHashMap<>();

        for (AvanceSemanal avance : avances) {
            Integer semana = avance.getNumeroSemana();
            if (semana == null) {
                continue;
            }

            porcentajesPorSemana.merge(
                    semana,
                    valorPorcentaje(avance.getPorcentajeSemana()),
                    this::mayorPorcentaje
            );
        }

        BigDecimal suma = porcentajesPorSemana.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal porcentajeGeneral = suma.divide(
                BigDecimal.valueOf(totalSemanas),
                2,
                RoundingMode.HALF_UP
        );

        if (porcentajeGeneral.compareTo(new BigDecimal("100")) > 0) {
            return new BigDecimal("100.00");
        }

        return porcentajeGeneral;
    }

    private int obtenerTotalSemanasCalculo(Cronograma cronograma) {
        if (cronograma == null || cronograma.getIdCronograma() == null) {
            return 1;
        }

        int totalDetalles = cronogramaDetalleRepo
                .findByCronograma_IdCronogramaOrderBySemanaAsc(cronograma.getIdCronograma())
                .stream()
                .map(CronogramaDetalle::getSemana)
                .filter(semana -> semana != null && semana > 0)
                .max(Integer::compareTo)
                .orElse(0);

        if (totalDetalles > 0) {
            sincronizarTotalSemanasCronograma(cronograma, totalDetalles);
            return totalDetalles;
        }

        Integer totalSemanas = cronograma.getTotalSemanas();
        return totalSemanas != null && totalSemanas > 0 ? totalSemanas : 1;
    }

    private void sincronizarTotalSemanasCronograma(Cronograma cronograma, int totalSemanas) {
        if (cronograma == null || totalSemanas <= 0) {
            return;
        }

        if (cronograma.getTotalSemanas() == null || cronograma.getTotalSemanas() != totalSemanas) {
            cronograma.setTotalSemanas(totalSemanas);

            if (cronograma.getFechaInicio() != null) {
                cronograma.setFechaFinEstimada(
                        cronograma.getFechaInicio().plusWeeks(totalSemanas - 1).plusDays(5)
                );
            }
        }
    }

    private void sincronizarDetalleCronograma(
            Integer idCronograma,
            Integer numeroSemana,
            BigDecimal porcentajeSemana
    ) {
        List<CronogramaDetalle> detallesSemana = cronogramaDetalleRepo
                .findByCronograma_IdCronogramaAndSemana(idCronograma, numeroSemana);

        if (detallesSemana == null || detallesSemana.isEmpty()) {
            return;
        }

        EstadoActividadCronograma estado = determinarEstadoActividad(porcentajeSemana);
        BigDecimal porcentaje = porcentajeSemana != null ? porcentajeSemana : BigDecimal.ZERO;

        for (CronogramaDetalle detalle : detallesSemana) {
            detalle.setPorcentaje(porcentaje);
            detalle.setEstadoActividad(estado);
        }

        cronogramaDetalleRepo.saveAll(detallesSemana);
    }

    private void actualizarEstadoCronogramaDesdeAvances(Integer idCronograma) {
        Cronograma cronograma = cronogramaRepo.findById(idCronograma)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cronograma no encontrado"));

        BigDecimal porcentajeGeneral = calcularPorcentajeGeneral(
                idCronograma,
                null,
                null,
                obtenerTotalSemanasCalculo(cronograma)
        );

        actualizarCronograma(cronograma, porcentajeGeneral);
    }

    private void actualizarCronograma(Cronograma cronograma, BigDecimal porcentajeGeneral) {
        cronograma.setAvanceGeneral(porcentajeGeneral);
        cronograma.setEstado(determinarEstadoCronograma(porcentajeGeneral));
        cronograma.setEstadoCronograma(determinarEstadoCronogramaEnum(porcentajeGeneral));
        cronogramaRepo.save(cronograma);
    }

    private void notificarCliente(AvanceSemanal avance, boolean nuevo) {
        Cronograma cronograma = avance.getCronograma();

        if (cronograma.getCotizacion() == null
                || cronograma.getCotizacion().getSolicitud() == null
                || cronograma.getCotizacion().getSolicitud().getUsuario() == null) {
            return;
        }

        Usuario cliente = cronograma.getCotizacion().getSolicitud().getUsuario();
        String nombreProyecto = cronograma.getCotizacion().getSolicitud().getNombreProyectoUsuario();
        String titulo = nuevo ? "Nuevo avance registrado" : "Avance actualizado";
        String accion = nuevo ? "registro" : "actualizo";
        String mensaje = "Se " + accion + " el avance del proyecto '" + nombreProyecto
                + "', correspondiente a la semana " + avance.getNumeroSemana() + ".";

        notificacionService.crearNotificacion(
                cliente,
                TipoNotificacion.AVANCE_REGISTRADO,
                ModuloNotificacion.AVANCE,
                titulo,
                mensaje,
                avance.getIdAvance()
        );
    }

    private void notificarSupervisoresComentarioCliente(
            AvanceSemanal avance,
            Usuario cliente,
            ComentarioAvance comentario
    ) {
        if (avance == null || cliente == null || comentario == null || !Integer.valueOf(3).equals(cliente.getIdRol())) {
            return;
        }

        List<Usuario> supervisores = usuarioRepo.findByIdRol(2);
        if (supervisores == null || supervisores.isEmpty()) {
            return;
        }

        Cronograma cronograma = avance.getCronograma();
        String nombreProyecto = "-";
        if (cronograma != null
                && cronograma.getCotizacion() != null
                && cronograma.getCotizacion().getSolicitud() != null) {
            nombreProyecto = textoNotificacion(
                    cronograma.getCotizacion().getSolicitud().getNombreProyectoUsuario()
            );
        }

        String titulo = "Comentario de cliente";
        String mensaje = "El cliente " + textoNotificacion(cliente.getNombreUsuario())
                + " comento el avance de la semana " + avance.getNumeroSemana()
                + " del proyecto '" + nombreProyecto + "': "
                + resumirComentario(comentario.getComentario());

        notificacionService.crearNotificacionParaVarios(
                supervisores,
                TipoNotificacion.COMENTARIO_AVANCE,
                ModuloNotificacion.AVANCE,
                titulo,
                mensaje,
                avance.getIdAvance()
        );
    }

    private String resumirComentario(String comentario) {
        String texto = textoNotificacion(comentario);
        return texto.length() <= 120 ? texto : texto.substring(0, 117) + "...";
    }

    private AvanceSemanalResponse mapToResponse(AvanceSemanal avance) {
        AvanceSemanalResponse res = new AvanceSemanalResponse();
        res.setIdAvance(avance.getIdAvance());
        res.setIdCronograma(avance.getCronograma().getIdCronograma());
        res.setNumeroSemana(avance.getNumeroSemana());
        res.setFechaRegistro(avance.getFechaRegistro());
        res.setTitulo(avance.getTitulo());
        res.setDescripcion(avance.getDescripcion());
        res.setObservaciones(avance.getObservaciones());
        res.setPorcentajeSemana(avance.getPorcentajeSemana());
        res.setPorcentajeGeneral(avance.getPorcentajeGeneral());
        res.setRegistradoPor(avance.getRegistradoPor());
        res.setEstado(avance.getEstado());
        return res;
    }

    private ComentarioAvanceResponse mapComentarioToResponse(ComentarioAvance comentario) {
        ComentarioAvanceResponse res = new ComentarioAvanceResponse();
        res.setIdComentario(comentario.getIdComentario());
        res.setIdAvance(comentario.getAvanceSemanal().getIdAvance());
        res.setIdUsuario(comentario.getUsuario().getIdUsuario());
        res.setNombreUsuario(comentario.getUsuario().getNombreUsuario());
        res.setComentario(comentario.getComentario());
        res.setFechaComentario(comentario.getFechaComentario());
        return res;
    }

    private String determinarEstadoSemana(BigDecimal porcentajeSemana) {
        if (porcentajeSemana == null || porcentajeSemana.compareTo(BigDecimal.ZERO) == 0) {
            return "PENDIENTE";
        }
        if (porcentajeSemana.compareTo(new BigDecimal("100")) < 0) {
            return "EN_PROCESO";
        }
        return "COMPLETADA";
    }

    private EstadoActividadCronograma determinarEstadoActividad(BigDecimal porcentajeSemana) {
        if (porcentajeSemana == null || porcentajeSemana.compareTo(BigDecimal.ZERO) <= 0) {
            return EstadoActividadCronograma.PENDIENTE;
        }

        if (porcentajeSemana.compareTo(new BigDecimal("100")) >= 0) {
            return EstadoActividadCronograma.TERMINADA;
        }

        return EstadoActividadCronograma.EN_PROCESO;
    }

    private String determinarEstadoCronograma(BigDecimal porcentajeGeneral) {
        if (porcentajeGeneral == null || porcentajeGeneral.compareTo(BigDecimal.ZERO) == 0) {
            return "PENDIENTE";
        }
        if (porcentajeGeneral.compareTo(new BigDecimal("100")) < 0) {
            return "EN_PROCESO";
        }
        return "FINALIZADO";
    }

    private EstadoCronograma determinarEstadoCronogramaEnum(BigDecimal porcentajeGeneral) {
        if (porcentajeGeneral != null && porcentajeGeneral.compareTo(new BigDecimal("100")) >= 0) {
            return EstadoCronograma.FINALIZADO;
        }

        return EstadoCronograma.EN_PROCESO;
    }

    private BigDecimal valorPorcentaje(BigDecimal porcentaje) {
        return porcentaje != null ? porcentaje : BigDecimal.ZERO;
    }

    private BigDecimal mayorPorcentaje(BigDecimal actual, BigDecimal nuevo) {
        return actual.compareTo(nuevo) >= 0 ? actual : nuevo;
    }

    private String limpiarTexto(String texto) {
        return texto != null ? texto.trim() : null;
    }

    private String textoNotificacion(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto.trim();
    }

    private static class ResultadoGuardado {
        private final AvanceSemanal avance;
        private final boolean nuevo;

        private ResultadoGuardado(AvanceSemanal avance, boolean nuevo) {
            this.avance = avance;
            this.nuevo = nuevo;
        }
    }
}
