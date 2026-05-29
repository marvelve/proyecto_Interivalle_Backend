package com.interivalle.Servicio;

import com.interivalle.DTO.CrearSolicitud;
import com.interivalle.DTO.ReprogramarVisitaRequest;
import com.interivalle.DTO.SolicitudResponse;
import com.interivalle.DTO.SolicitudServicioItem;
import com.interivalle.Modelo.Servicios;
import com.interivalle.Modelo.Solicitud;
import com.interivalle.Modelo.SolicitudServicios;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Modelo.VisitaTecnica;
import com.interivalle.Modelo.enums.ModuloNotificacion;
import com.interivalle.Modelo.enums.TipoNotificacion;
import com.interivalle.Repositorio.ServiciosRepositorio;
import com.interivalle.Repositorio.SolicitudRepositorio;
import com.interivalle.Repositorio.SolicitudServiciosRepositorio;
import com.interivalle.Repositorio.UsuarioRepositorio;
import com.interivalle.Repositorio.VisitaTecnicaRepositorio;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolicitudService {

    private static final String TIPO_COTIZACION_BASE = "COTIZACION_BASE";
    private static final String TIPO_VISITA_TECNICA = "VISITA_TECNICA";

    private static final String ESTADO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_GENERADA = "GENERADA";
    private static final String ESTADO_REPROGRAMADA = "REPROGRAMADA";
    private static final String ESTADO_CONFIRMADA = "CONFIRMADA";
    private static final String ESTADO_REALIZADA = "REALIZADA";

    private static final int ROL_ADMIN = 1;
    private static final int ROL_SUPERVISOR = 2;
    private static final int ROL_CLIENTE = 3;

    @Autowired
    private UsuarioRepositorio usuarioRepo;

    @Autowired
    private SolicitudRepositorio solicitudRepo;

    @Autowired
    private ServiciosRepositorio serviciosRepo;

    @Autowired
    private SolicitudServiciosRepositorio solicitudServicioRepo;

    @Autowired
    private VisitaTecnicaRepositorio visitaTecnicaRepo;

    @Autowired
    private NotificacionService notificacionService;

    @Transactional
    public SolicitudResponse crearSolicitud(CrearSolicitud dto) {
        validarDatosGenerales(dto);

        String tipoSolicitud = dto.getTipoSolicitud().trim().toUpperCase();

        if (TIPO_COTIZACION_BASE.equals(tipoSolicitud)) {
            validarDatosCotizacionBase(dto);
        }

        if (TIPO_VISITA_TECNICA.equals(tipoSolicitud)) {
            validarDatosVisitaTecnica(dto);
            validarDisponibilidadVisita(dto);
        }

        // Consulta el usuario dueño de la solicitud.
        Usuario usuario = usuarioRepo.findByCorreoUsuario(dto.getCorreoUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Crea la solicitud principal en estado PENDIENTE.
        Solicitud solicitud = new Solicitud();
        solicitud.setUsuario(usuario);
        solicitud.setNombreProyectoUsuario(dto.getNombreProyecto().trim());
        solicitud.setTipoSolicitud(tipoSolicitud);
        solicitud.setEstado(ESTADO_PENDIENTE);
        solicitud.setFechaSolicitud(LocalDate.now());

        solicitud = solicitudRepo.save(solicitud);

        if (TIPO_COTIZACION_BASE.equals(tipoSolicitud)) {
            guardarServiciosCotizacion(dto, solicitud);
        }

        if (TIPO_VISITA_TECNICA.equals(tipoSolicitud)) {
            guardarVisitaTecnica(dto, solicitud);
            notificarVisitaCreada(usuario, solicitud);
        }

        return buildResponseFromSolicitud(solicitud.getIdSolicitud());
    }

    public SolicitudResponse obtenerSolicitud(Integer idSolicitud) {
        Solicitud solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        return buildResponseFromSolicitud(solicitud.getIdSolicitud());
    }

    @Transactional
    public SolicitudResponse generarCotizacion(Integer idSolicitud) {
        Solicitud solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        // Solo se genera cotizacion desde solicitudes pendientes.
        if (!ESTADO_PENDIENTE.equalsIgnoreCase(solicitud.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se puede generar cotización para solicitudes en estado PENDIENTE"
            );
        }

        if (!TIPO_COTIZACION_BASE.equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo las solicitudes de tipo COTIZACION_BASE pueden generar cotización"
            );
        }

        solicitud.setEstado(ESTADO_GENERADA);
        solicitud = solicitudRepo.save(solicitud);

        return buildResponseFromSolicitud(solicitud.getIdSolicitud());
    }

    public List<SolicitudResponse> listarTodas() {
        return solicitudRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<SolicitudResponse> listarPorCorreoUsuario(String correoUsuario) {
        return solicitudRepo.findByUsuarioCorreoUsuario(correoUsuario)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SolicitudResponse reprogramarVisita(
            Integer idSolicitud,
            ReprogramarVisitaRequest req,
            Integer idUsuario
    ) {
        Solicitud solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (!TIPO_VISITA_TECNICA.equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden reprogramar solicitudes de visita técnica"
            );
        }

        Usuario usuarioAccion = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Integer rolAccion = usuarioAccion.getIdRol();
        boolean esAdminOSupervisor = rolAccion != null && (rolAccion == ROL_ADMIN || rolAccion == ROL_SUPERVISOR);
        boolean estadoPendiente = ESTADO_PENDIENTE.equalsIgnoreCase(solicitud.getEstado());
        boolean estadoReprogramada = ESTADO_REPROGRAMADA.equalsIgnoreCase(solicitud.getEstado());

        // Cliente solo puede reprogramar pendientes. Admin/Supervisor tambien reprogramadas.
        if (!estadoPendiente && !(esAdminOSupervisor && estadoReprogramada)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El cliente solo puede reprogramar visitas pendientes; Admin o Supervisor pueden reprogramar visitas pendientes o reprogramadas"
            );
        }

        validarDatosReprogramacion(req);
        validarClienteDuenoDeVisita(solicitud, usuarioAccion, rolAccion);

        VisitaTecnica visita = visitaTecnicaRepo.findBySolicitud_IdSolicitud(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita técnica no encontrada"));

        validarDisponibilidadReprogramacion(solicitud, visita, req);

        // Actualiza fecha, hora y estados de la solicitud y la visita.
        visita.setFechaVisita(req.getFechaVisita());
        visita.setHoraVisita(req.getHoraVisita());
        visita.setEstadoVisita(ESTADO_REPROGRAMADA);
        solicitud.setEstado(ESTADO_REPROGRAMADA);

        visitaTecnicaRepo.save(visita);
        solicitudRepo.save(solicitud);

        notificarVisitaReprogramada(solicitud, usuarioAccion, rolAccion, req);

        return buildResponseFromSolicitud(idSolicitud);
    }

    @Transactional
    public SolicitudResponse confirmarVisitaTecnica(Integer idSolicitud) {
        Solicitud solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (!TIPO_VISITA_TECNICA.equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo las visitas tecnicas pueden confirmarse"
            );
        }

        if (!ESTADO_PENDIENTE.equalsIgnoreCase(solicitud.getEstado())
                && !ESTADO_REPROGRAMADA.equalsIgnoreCase(solicitud.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden confirmar visitas en estado PENDIENTE o REPROGRAMADA"
            );
        }

        VisitaTecnica visita = visitaTecnicaRepo.findBySolicitud_IdSolicitud(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita tecnica no encontrada"));

        // Cambia ambos estados para mantener sincronizadas las tablas.
        solicitud.setEstado(ESTADO_CONFIRMADA);
        visita.setEstadoVisita(ESTADO_CONFIRMADA);

        solicitudRepo.save(solicitud);
        visitaTecnicaRepo.save(visita);

        notificarVisitaConfirmada(solicitud, visita);

        return buildResponseFromSolicitud(idSolicitud);
    }

    @Transactional
    public SolicitudResponse marcarVisitaRealizada(Integer idSolicitud) {
        Solicitud solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (!TIPO_VISITA_TECNICA.equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo las visitas técnicas pueden marcarse como realizadas"
            );
        }

        VisitaTecnica visita = visitaTecnicaRepo.findBySolicitud_IdSolicitud(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita técnica no encontrada"));

        if (!ESTADO_PENDIENTE.equalsIgnoreCase(solicitud.getEstado())
                && !ESTADO_REPROGRAMADA.equalsIgnoreCase(solicitud.getEstado())
                && !ESTADO_CONFIRMADA.equalsIgnoreCase(solicitud.getEstado())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La visita tecnica solo puede marcarse como realizada desde PENDIENTE, REPROGRAMADA o CONFIRMADA"
            );
        }

        solicitud.setEstado(ESTADO_REALIZADA);
        visita.setEstadoVisita(ESTADO_REALIZADA);

        solicitudRepo.save(solicitud);
        visitaTecnicaRepo.save(visita);

        return buildResponseFromSolicitud(idSolicitud);
    }

    private void validarDatosGenerales(CrearSolicitud dto) {
        if (dto.getCorreoUsuario() == null || dto.getCorreoUsuario().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "correoUsuario es requerido");
        }

        if (dto.getNombreProyecto() == null || dto.getNombreProyecto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nombreProyecto es requerido");
        }

        if (dto.getTipoSolicitud() == null || dto.getTipoSolicitud().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipoSolicitud es requerido");
        }
    }

    private void validarDatosCotizacionBase(CrearSolicitud dto) {
        if (dto.getServicios() == null || dto.getServicios().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar al menos un servicio");
        }
    }

    private void validarDatosVisitaTecnica(CrearSolicitud dto) {
        if (dto.getFechaVisita() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fechaVisita es requerida");
        }

        if (!dto.getFechaVisita().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de visita debe ser futura");
        }

        if (dto.getHoraVisita() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horaVisita es requerida");
        }

        if (!esHoraVisitaValida(dto.getHoraVisita())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La hora de visita debe estar entre 08:00-12:00 o 14:00-17:00"
            );
        }

        if (dto.getDireccionVisita() == null || dto.getDireccionVisita().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direccionVisita es requerida");
        }

        if (dto.getCelularCliente() == null || dto.getCelularCliente().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "celularCliente es requerido");
        }
    }

    private void validarDisponibilidadVisita(CrearSolicitud dto) {
        // Misma visita: cliente, proyecto, fecha y hora.
        boolean visitaExistente = visitaTecnicaRepo
                .existsBySolicitud_Usuario_CorreoUsuarioAndSolicitud_NombreProyectoUsuarioAndFechaVisitaAndHoraVisita(
                        dto.getCorreoUsuario(),
                        dto.getNombreProyecto().trim(),
                        dto.getFechaVisita(),
                        dto.getHoraVisita()
                );

        if (visitaExistente) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una visita técnica para este proyecto en la misma fecha y hora"
            );
        }

        // Evita que un cliente tenga dos visitas en la misma fecha y hora.
        boolean yaExisteVisitaMismaHora = visitaTecnicaRepo
                .existsBySolicitud_Usuario_CorreoUsuarioAndFechaVisitaAndHoraVisita(
                        dto.getCorreoUsuario(),
                        dto.getFechaVisita(),
                        dto.getHoraVisita()
                );

        if (yaExisteVisitaMismaHora) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya tienes una visita técnica agendada para esa fecha y hora"
            );
        }

        Optional<VisitaTecnica> visitaExistenteProyecto = visitaTecnicaRepo
                .findBySolicitud_Usuario_CorreoUsuarioAndSolicitud_NombreProyectoUsuario(
                        dto.getCorreoUsuario(),
                        dto.getNombreProyecto().trim()
                );

        if (visitaExistenteProyecto.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una visita técnica para este proyecto. Debe reprogramarla y no crear una nueva."
            );
        }
    }

    private void guardarServiciosCotizacion(CrearSolicitud dto, Solicitud solicitud) {
        for (Integer idServicio : dto.getServicios()) {
            // Valida que no exista el mismo servicio para el mismo proyecto.
            long existe = solicitudServicioRepo.existeServicioEnProyecto(
                    dto.getCorreoUsuario(),
                    solicitud.getNombreProyectoUsuario(),
                    solicitud.getTipoSolicitud(),
                    idServicio
            );

            if (existe > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Ya existe una solicitud para este proyecto con el servicio ID: " + idServicio
                );
            }

            Servicios servicio = serviciosRepo.findById(idServicio)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Servicio no encontrado: " + idServicio
                    ));

            SolicitudServicios detalle = new SolicitudServicios();
            detalle.setSolicitud(solicitud);
            detalle.setServicios(servicio);
            detalle.setEstado(ESTADO_PENDIENTE);

            solicitudServicioRepo.save(detalle);
        }
    }

    private void guardarVisitaTecnica(CrearSolicitud dto, Solicitud solicitud) {
        VisitaTecnica visita = new VisitaTecnica();
        visita.setSolicitud(solicitud);
        visita.setFechaVisita(dto.getFechaVisita());
        visita.setHoraVisita(dto.getHoraVisita());
        visita.setDireccionVisita(dto.getDireccionVisita().trim());
        visita.setCelularCliente(dto.getCelularCliente().trim());
        visita.setEstadoVisita(ESTADO_PENDIENTE);

        visitaTecnicaRepo.save(visita);
    }

    private void validarDatosReprogramacion(ReprogramarVisitaRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de reprogramación son obligatorios");
        }

        if (req.getFechaVisita() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fechaVisita es requerida");
        }

        if (!req.getFechaVisita().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha debe ser futura");
        }

        if (req.getHoraVisita() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horaVisita es requerida");
        }

        if (!esHoraVisitaValida(req.getHoraVisita())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La hora de visita debe estar entre 08:00-12:00 o 14:00-17:00"
            );
        }
    }

    private void validarClienteDuenoDeVisita(Solicitud solicitud, Usuario usuarioAccion, Integer rolAccion) {
        if (rolAccion != null && rolAccion == ROL_CLIENTE) {
            Integer idClienteSolicitud = solicitud.getUsuario() != null
                    ? solicitud.getUsuario().getIdUsuario()
                    : null;

            if (!usuarioAccion.getIdUsuario().equals(idClienteSolicitud)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "El cliente solo puede reprogramar sus propias visitas tecnicas"
                );
            }
        }
    }

    private void validarDisponibilidadReprogramacion(
            Solicitud solicitud,
            VisitaTecnica visita,
            ReprogramarVisitaRequest req
    ) {
        boolean yaExisteVisitaMismaHora = visitaTecnicaRepo
                .existsBySolicitud_Usuario_CorreoUsuarioAndFechaVisitaAndHoraVisita(
                        solicitud.getUsuario().getCorreoUsuario(),
                        req.getFechaVisita(),
                        req.getHoraVisita()
                );

        boolean mismaFechaHora =
                visita.getFechaVisita().equals(req.getFechaVisita())
                        && visita.getHoraVisita().equals(req.getHoraVisita());

        if (yaExisteVisitaMismaHora && !mismaFechaHora) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya tienes una visita técnica agendada para esa fecha y hora"
            );
        }
    }

    private boolean esHoraVisitaValida(LocalTime hora) {
        return (!hora.isBefore(LocalTime.of(8, 0)) && !hora.isAfter(LocalTime.of(12, 0)))
                || (!hora.isBefore(LocalTime.of(14, 0)) && !hora.isAfter(LocalTime.of(17, 0)));
    }

    private void cargarDatosVisitaTecnica(Solicitud solicitud, SolicitudResponse resp) {
        if (TIPO_VISITA_TECNICA.equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            visitaTecnicaRepo.findBySolicitud_IdSolicitud(solicitud.getIdSolicitud())
                    .ifPresent(visita -> {
                        resp.setFechaVisita(visita.getFechaVisita());
                        resp.setHoraVisita(visita.getHoraVisita());
                        resp.setDireccionVisita(visita.getDireccionVisita());
                        resp.setCelularCliente(visita.getCelularCliente());
                    });
        }
    }

    private SolicitudResponse toResponse(Solicitud solicitud) {
        SolicitudResponse dto = new SolicitudResponse();
        dto.setIdSolicitud(solicitud.getIdSolicitud());
        dto.setTipoSolicitud(solicitud.getTipoSolicitud());
        dto.setEstado(solicitud.getEstado());
        dto.setNombreProyecto(solicitud.getNombreProyectoUsuario());
        dto.setFechaSolicitud(solicitud.getFechaSolicitud());

        if (solicitud.getUsuario() != null) {
            dto.setCorreoUsuario(solicitud.getUsuario().getCorreoUsuario());
        }

        if (solicitud.getServiciosSeleccionados() != null && !solicitud.getServiciosSeleccionados().isEmpty()) {
            List<SolicitudServicioItem> servicios = solicitud.getServiciosSeleccionados()
                    .stream()
                    .map(item -> {
                        SolicitudServicioItem dtoItem = new SolicitudServicioItem();
                        dtoItem.setIdServicio(item.getServicios().getIdServicio());
                        dtoItem.setNombreServicio(item.getServicios().getNombreServicio());
                        dtoItem.setEstado(item.getEstado());
                        return dtoItem;
                    })
                    .collect(Collectors.toList());

            dto.setSolicitudServicios(servicios);
        }

        cargarDatosVisitaTecnica(solicitud, dto);
        return dto;
    }

    private SolicitudResponse buildResponseFromSolicitud(Integer idSolicitud) {
        Solicitud solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        List<SolicitudServicios> detalles = solicitudServicioRepo.findBySolicitud_IdSolicitud(idSolicitud);

        List<SolicitudServicioItem> items = detalles.stream()
                .map(ss -> {
                    SolicitudServicioItem item = new SolicitudServicioItem();
                    item.setIdSolicitudServicio(ss.getIdSolicitudServicio());
                    item.setIdServicio(ss.getServicios().getIdServicio());
                    item.setNombreServicio(ss.getServicios().getNombreServicio());
                    item.setEstado(ss.getEstado());
                    return item;
                })
                .collect(Collectors.toList());

        SolicitudResponse resp = new SolicitudResponse();
        resp.setIdSolicitud(solicitud.getIdSolicitud());
        resp.setTipoSolicitud(solicitud.getTipoSolicitud());
        resp.setEstado(solicitud.getEstado());
        resp.setNombreProyecto(solicitud.getNombreProyectoUsuario());
        resp.setFechaSolicitud(solicitud.getFechaSolicitud());

        if (solicitud.getUsuario() != null) {
            resp.setCorreoUsuario(solicitud.getUsuario().getCorreoUsuario());
        }

        resp.setSolicitudServicios(items);
        cargarDatosVisitaTecnica(solicitud, resp);

        return resp;
    }

    private void notificarVisitaCreada(Usuario usuario, Solicitud solicitud) {
        // Notifica a supervisores cuando un cliente crea visita tecnica.
        List<Usuario> supervisores = usuarioRepo.findByIdRol(ROL_SUPERVISOR);

        String titulo = "Nueva visita técnica solicitada";
        String mensaje = "El cliente " + usuario.getNombreUsuario()
                + " solicitó una visita técnica para el proyecto '"
                + solicitud.getNombreProyectoUsuario() + "'.";

        notificacionService.crearNotificacionParaVarios(
                supervisores,
                TipoNotificacion.VISITA_TECNICA_CREADA,
                ModuloNotificacion.VISITA_TECNICA,
                titulo,
                mensaje,
                solicitud.getIdSolicitud()
        );
    }

    private void notificarVisitaReprogramada(
            Solicitud solicitud,
            Usuario usuarioAccion,
            Integer rolAccion,
            ReprogramarVisitaRequest req
    ) {
        String nombreProyecto = solicitud.getNombreProyectoUsuario();
        String fechaTexto = req.getFechaVisita().toString();
        String horaTexto = req.getHoraVisita().toString();

        if (rolAccion != null && rolAccion == ROL_CLIENTE) {
            notificarSupervisoresPorReprogramacionCliente(solicitud, usuarioAccion, nombreProyecto, fechaTexto, horaTexto);
        } else if (rolAccion != null && (rolAccion == ROL_ADMIN || rolAccion == ROL_SUPERVISOR)) {
            notificarClientePorReprogramacionInterna(solicitud, rolAccion, nombreProyecto, fechaTexto, horaTexto);
        }
    }

    private void notificarSupervisoresPorReprogramacionCliente(
            Solicitud solicitud,
            Usuario usuarioAccion,
            String nombreProyecto,
            String fechaTexto,
            String horaTexto
    ) {
        List<Usuario> supervisores = usuarioRepo.findByIdRol(ROL_SUPERVISOR);

        String titulo = "Visita técnica reprogramada por el cliente";
        String mensaje = "El cliente " + usuarioAccion.getNombreUsuario()
                + " reprogramó la visita técnica del proyecto '" + nombreProyecto
                + "' para la fecha " + fechaTexto
                + " a las " + horaTexto + ".";

        notificacionService.crearNotificacionParaVarios(
                supervisores,
                TipoNotificacion.VISITA_TECNICA_REPROGRAMADA,
                ModuloNotificacion.VISITA_TECNICA,
                titulo,
                mensaje,
                solicitud.getIdSolicitud()
        );
    }

    private void notificarClientePorReprogramacionInterna(
            Solicitud solicitud,
            Integer rolAccion,
            String nombreProyecto,
            String fechaTexto,
            String horaTexto
    ) {
        if (solicitud.getUsuario() == null) {
            return;
        }

        Usuario cliente = solicitud.getUsuario();
        String rolTexto = rolAccion == ROL_ADMIN ? "Admin" : "Supervisor";
        String titulo = "Visita tecnica reprogramada por " + rolTexto;
        String mensaje = "La visita tecnica del proyecto '" + nombreProyecto
                + "' fue reprogramada por " + rolTexto
                + " para la fecha " + fechaTexto
                + " a las " + horaTexto + ".";

        notificacionService.crearNotificacion(
                cliente,
                TipoNotificacion.VISITA_TECNICA_REPROGRAMADA,
                ModuloNotificacion.VISITA_TECNICA,
                titulo,
                mensaje,
                solicitud.getIdSolicitud()
        );
    }

    private void notificarVisitaConfirmada(Solicitud solicitud, VisitaTecnica visita) {
        if (solicitud.getUsuario() == null) {
            return;
        }

        String nombreProyecto = solicitud.getNombreProyectoUsuario();
        String fechaTexto = visita.getFechaVisita() != null ? visita.getFechaVisita().toString() : "-";
        String horaTexto = visita.getHoraVisita() != null ? visita.getHoraVisita().toString() : "-";

        String titulo = "Visita tecnica confirmada";
        String mensaje = "La visita tecnica del proyecto '" + nombreProyecto
                + "' esta confirmada para la fecha " + fechaTexto
                + " a las " + horaTexto + ".";

        notificacionService.crearNotificacion(
                solicitud.getUsuario(),
                TipoNotificacion.VISITA_TECNICA_CONFIRMADA,
                ModuloNotificacion.VISITA_TECNICA,
                titulo,
                mensaje,
                solicitud.getIdSolicitud()
        );
    }
}
