package com.interivalle.Servicio;

import com.interivalle.DTO.CarpinteriaResponse;
import com.interivalle.DTO.CotizacionPersonalizadaDetalleResponse;
import com.interivalle.DTO.CotizacionPersonalizadaRequest;
import com.interivalle.DTO.CotizacionPersonalizadaResponse;
import com.interivalle.DTO.MesonGranitoResponse;
import com.interivalle.DTO.ObraBlancaResponse;
import com.interivalle.DTO.VidrioResponse;
import com.interivalle.Modelo.Carpinteria;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.CotizacionPersonalizada;
import com.interivalle.Modelo.MesonGranito;
import com.interivalle.Modelo.ObraBlanca;
import com.interivalle.Modelo.Solicitud;
import com.interivalle.Modelo.Vidrio;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Repositorio.CarpinteriaRepositorio;
import com.interivalle.Repositorio.CotizacionPersonalizadaRepositorio;
import com.interivalle.Repositorio.CotizacionRepositorio;
import com.interivalle.Repositorio.MesonGranitoRepositorio;
import com.interivalle.Repositorio.ObraBlancaRepositorio;
import com.interivalle.Repositorio.SolicitudRepositorio;
import com.interivalle.Repositorio.VidrioRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CotizacionPersonalizadaService {

    @Autowired
    private CotizacionPersonalizadaRepositorio cotizacionRepo;

    @Autowired
    private SolicitudRepositorio solicitudRepo;

    @Autowired
    private CotizacionRepositorio cotizacionBaseRepo;

    @Autowired
    private ObraBlancaRepositorio obraBlancaRepo;

    @Autowired
    private CarpinteriaRepositorio carpinteriaRepo;

    @Autowired
    private VidrioRepositorio vidrioRepo;

    @Autowired
    private MesonGranitoRepositorio mesonGranitoRepo;

    public CotizacionPersonalizadaResponse crear(CotizacionPersonalizadaRequest req) {
        // Validaciones principales para crear la cabecera de adicionales.
        if (req.getIdSolicitud() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idSolicitud es obligatorio");
        }

        if (req.getIdCotizacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idCotizacion es obligatorio");
        }

        // Si ya existe una cotizacion personalizada para la base, se reutiliza.
        CotizacionPersonalizada existente = cotizacionRepo
                .findByCotizacion_IdCotizacion(req.getIdCotizacion())
                .orElse(null);

        if (existente != null) {
            validarCotizacionBaseEditable(existente.getCotizacion());
            return toResponse(existente);
        }

        // Consulta la solicitud y la cotizacion base que van a quedar relacionadas.
        Solicitud solicitud = solicitudRepo.findById(req.getIdSolicitud())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada"
                ));

        Cotizacion cotizacionBase = cotizacionBaseRepo.findById(req.getIdCotizacion())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion base no encontrada"
                ));

        validarCotizacionBaseEditable(cotizacionBase);

        CotizacionPersonalizada cotizacion = new CotizacionPersonalizada();
        cotizacion.setSolicitud(solicitud);
        cotizacion.setCotizacion(cotizacionBase);
        cotizacion.setUsuario(solicitud.getUsuario());
        cotizacion.setNombreProyecto(obtenerNombreProyecto(req, solicitud));
        cotizacion.setFechaCotizacion(LocalDate.now());
        cotizacion.setEstado("PENDIENTE");
        cotizacion.setSubtotal(BigDecimal.ZERO);
        cotizacion.setTotal(BigDecimal.ZERO);
        cotizacion.setObservacionGeneral(req.getObservacionGeneral());

        CotizacionPersonalizada guardada = cotizacionRepo.save(cotizacion);
        return toResponse(guardada);
    }

    public List<CotizacionPersonalizadaResponse> listarTodas() {
        return cotizacionRepo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CotizacionPersonalizadaResponse obtenerPorId(Integer id) {
        CotizacionPersonalizada cotizacion = cotizacionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion no encontrada"
                ));

        return toResponse(cotizacion);
    }

    public List<CotizacionPersonalizadaResponse> listarPorUsuario(Integer idUsuario) {
        return cotizacionRepo.findByUsuario_IdUsuario(idUsuario)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CotizacionPersonalizadaResponse cambiarEstado(Integer id, String estado) {
        CotizacionPersonalizada cotizacion = cotizacionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion no encontrada"
                ));

        if (estado == null || estado.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado es obligatorio");
        }

        String nuevoEstado = estado.trim().toUpperCase();
        validarEstadoPersonalizado(nuevoEstado);

        cotizacion.setEstado(nuevoEstado);
        CotizacionPersonalizada actualizada = cotizacionRepo.save(cotizacion);

        return toResponse(actualizada);
    }

    public CotizacionPersonalizadaResponse recalcular(Integer idCotizacion) {
        CotizacionPersonalizada cotizacion = cotizacionRepo.findById(idCotizacion)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion no encontrada"
                ));

        validarCotizacionBaseEditable(cotizacion.getCotizacion());

        // Recalcula el total sumando todos los adicionales guardados por servicio.
        BigDecimal subtotal = calcularTotalAdicionales(idCotizacion);

        cotizacion.setSubtotal(subtotal);
        cotizacion.setTotal(subtotal);

        // Mantiene el mismo cambio de estado que ya tenia el flujo anterior.
        if ("BORRADOR".equalsIgnoreCase(cotizacion.getEstado())
                && subtotal.compareTo(BigDecimal.ZERO) > 0) {
            cotizacion.setEstado("GENERADA");
        }

        CotizacionPersonalizada actualizada = cotizacionRepo.save(cotizacion);
        return toResponse(actualizada);
    }

    public CotizacionPersonalizadaResponse toResponse(CotizacionPersonalizada cotizacion) {
        CotizacionPersonalizadaResponse dto = new CotizacionPersonalizadaResponse();
        dto.setIdCotizacionPersonalizada(cotizacion.getIdCotizacionPersonalizada());

        if (cotizacion.getCotizacion() != null) {
            dto.setIdCotizacion(cotizacion.getCotizacion().getIdCotizacion());
        }

        dto.setIdSolicitud(cotizacion.getSolicitud().getIdSolicitud());
        dto.setNombreProyecto(cotizacion.getNombreProyecto());
        dto.setFechaCotizacion(cotizacion.getFechaCotizacion());
        dto.setEstado(cotizacion.getEstado());
        dto.setSubtotal(cotizacion.getSubtotal());
        dto.setTotal(cotizacion.getTotal());
        dto.setObservacionGeneral(cotizacion.getObservacionGeneral());
        return dto;
    }

    public CotizacionPersonalizadaDetalleResponse obtenerDetalle(Integer idCotizacionPersonalizada) {
        CotizacionPersonalizada cotizacion = cotizacionRepo.findById(idCotizacionPersonalizada)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion no encontrada"
                ));

        return armarDetalle(cotizacion);
    }

    public CotizacionPersonalizadaDetalleResponse obtenerDetallePorCotizacion(Integer idCotizacion) {
        CotizacionPersonalizada cotizacion = cotizacionRepo
                .findByCotizacion_IdCotizacion(idCotizacion)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe cotizacion personalizada para la cotizacion base: " + idCotizacion
                ));

        return armarDetalle(cotizacion);
    }

    public CotizacionPersonalizada obtenerOCrearPorCotizacion(
            Integer idCotizacion,
            Integer idSolicitud,
            String nombreProyecto,
            Integer idUsuario
    ) {
        // Este metodo conserva el flujo auxiliar que crea la cabecera si aun no existe.
        CotizacionPersonalizada existente = cotizacionRepo
                .findByCotizacion_IdCotizacion(idCotizacion)
                .orElse(null);

        if (existente != null) {
            validarCotizacionBaseEditable(existente.getCotizacion());
            return existente;
        }

        Solicitud solicitud = solicitudRepo.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada"
                ));

        Cotizacion cotizacionBase = cotizacionBaseRepo.findById(idCotizacion)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cotizacion base no encontrada"
                ));

        validarCotizacionBaseEditable(cotizacionBase);

        CotizacionPersonalizada nueva = new CotizacionPersonalizada();
        nueva.setSolicitud(solicitud);
        nueva.setCotizacion(cotizacionBase);
        nueva.setUsuario(solicitud.getUsuario());
        nueva.setNombreProyecto(
                nombreProyecto != null && !nombreProyecto.trim().isEmpty()
                        ? nombreProyecto
                        : solicitud.getNombreProyectoUsuario()
        );
        nueva.setFechaCotizacion(LocalDate.now());
        nueva.setEstado("PENDIENTE");
        nueva.setSubtotal(BigDecimal.ZERO);
        nueva.setTotal(BigDecimal.ZERO);
        nueva.setObservacionGeneral("Adicion de actividades personalizadas");

        return cotizacionRepo.save(nueva);
    }

    private CotizacionPersonalizadaDetalleResponse armarDetalle(CotizacionPersonalizada cotizacion) {
        Integer idPersonalizada = cotizacion.getIdCotizacionPersonalizada();

        // Consulta los adicionales de cada servicio usando la cabecera personalizada.
        List<ObraBlanca> listaObraBlanca =
                obraBlancaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada);
        List<Carpinteria> listaCarpinteria =
                carpinteriaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada);
        List<Vidrio> listaVidrio =
                vidrioRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada);
        List<MesonGranito> listaMeson =
                mesonGranitoRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idPersonalizada);

        CotizacionPersonalizadaDetalleResponse dto = new CotizacionPersonalizadaDetalleResponse();
        dto.setIdCotizacionPersonalizada(idPersonalizada);

        if (cotizacion.getCotizacion() != null) {
            dto.setIdCotizacion(cotizacion.getCotizacion().getIdCotizacion());
        }

        dto.setIdSolicitud(cotizacion.getSolicitud().getIdSolicitud());
        dto.setIdUsuario(cotizacion.getUsuario().getIdUsuario());
        dto.setNombreProyecto(cotizacion.getNombreProyecto());
        dto.setFechaCotizacion(cotizacion.getFechaCotizacion());
        dto.setEstado(cotizacion.getEstado());

        BigDecimal totalAdicionales = calcularTotalAdicionales(idPersonalizada);
        dto.setSubtotal(totalAdicionales);
        dto.setTotal(totalAdicionales);
        dto.setObservacionGeneral(cotizacion.getObservacionGeneral());

        dto.setObraBlanca(listaObraBlanca.stream().map(this::toObraBlancaResponse).collect(Collectors.toList()));
        dto.setCarpinteria(listaCarpinteria.stream().map(this::toCarpinteriaResponse).collect(Collectors.toList()));
        dto.setVidrio(listaVidrio.stream().map(this::toVidrioResponse).collect(Collectors.toList()));
        dto.setMesonGranito(listaMeson.stream().map(this::toMesonGranitoResponse).collect(Collectors.toList()));

        return dto;
    }

    private String obtenerNombreProyecto(CotizacionPersonalizadaRequest req, Solicitud solicitud) {
        if (req.getNombreProyecto() != null && !req.getNombreProyecto().trim().isEmpty()) {
            return req.getNombreProyecto();
        }

        return solicitud.getNombreProyectoUsuario();
    }

    private void validarEstadoPersonalizado(String estado) {
        if (!estado.equals("PENDIENTE")
                && !estado.equals("BORRADOR")
                && !estado.equals("GENERADA")
                && !estado.equals("APROBADA")
                && !estado.equals("RECHAZADA")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Estado no valido. Use: PENDIENTE, BORRADOR, GENERADA, APROBADA o RECHAZADA"
            );
        }
    }

    private BigDecimal calcularTotalAdicionales(Integer idCotizacionPersonalizada) {
        return sumarObraBlanca(idCotizacionPersonalizada)
                .add(sumarCarpinteria(idCotizacionPersonalizada))
                .add(sumarVidrio(idCotizacionPersonalizada))
                .add(sumarMeson(idCotizacionPersonalizada));
    }

    private BigDecimal sumarObraBlanca(Integer idCotizacion) {
        List<ObraBlanca> items = obraBlancaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idCotizacion);

        BigDecimal total = BigDecimal.ZERO;
        for (ObraBlanca item : items) {
            if (item.getSubtotal() != null) {
                total = total.add(item.getSubtotal());
            }
        }

        return total;
    }

    private BigDecimal sumarCarpinteria(Integer idCotizacion) {
        List<Carpinteria> items = carpinteriaRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idCotizacion);

        BigDecimal total = BigDecimal.ZERO;
        for (Carpinteria item : items) {
            if (item.getSubtotal() != null) {
                total = total.add(item.getSubtotal());
            }
        }

        return total;
    }

    private BigDecimal sumarVidrio(Integer idCotizacion) {
        List<Vidrio> items = vidrioRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idCotizacion);

        BigDecimal total = BigDecimal.ZERO;
        for (Vidrio item : items) {
            if (item.getSubtotal() != null) {
                total = total.add(item.getSubtotal());
            }
        }

        return total;
    }

    private BigDecimal sumarMeson(Integer idCotizacion) {
        List<MesonGranito> items = mesonGranitoRepo.findByCotizacionPersonalizada_IdCotizacionPersonalizada(idCotizacion);

        BigDecimal total = BigDecimal.ZERO;
        for (MesonGranito item : items) {
            if (item.getSubtotal() != null) {
                total = total.add(item.getSubtotal());
            }
        }

        return total;
    }

    private ObraBlancaResponse toObraBlancaResponse(ObraBlanca item) {
        ObraBlancaResponse dto = new ObraBlancaResponse();
        dto.setIdObraBlanca(item.getIdObraBlanca());
        dto.setIdActividad(item.getIdActividad());
        dto.setActividad(item.getActividad());
        dto.setLugar(item.getLugar());
        dto.setUnidad(item.getUnidad());
        dto.setTipoCobro(item.getUnidad());
        dto.setCantidad(item.getCantidad());
        dto.setSemanas(item.getSemanas());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setMedida(item.getMedida());
        dto.setDescripcion(item.getDescripcion());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    private CarpinteriaResponse toCarpinteriaResponse(Carpinteria item) {
        CarpinteriaResponse dto = new CarpinteriaResponse();
        dto.setIdCarpinteria(item.getIdCarpinteria());
        dto.setTipoMueble(item.getTipoMueble());
        dto.setMaterial(item.getMaterial());
        dto.setLargo(item.getLargo());
        dto.setAncho(item.getAncho());
        dto.setAlto(item.getAlto());
        dto.setCantidad(item.getCantidad());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setDescripcion(item.getDescripcion());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    private VidrioResponse toVidrioResponse(Vidrio item) {
        VidrioResponse dto = new VidrioResponse();
        dto.setIdVidrio(item.getIdVidrio());
        dto.setTipoVidrio(item.getTipoVidrio());
        dto.setAncho(item.getAncho());
        dto.setAlto(item.getAlto());
        dto.setCantidad(item.getCantidad());
        dto.setInstalacion(item.getInstalacion());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setDescripcion(item.getDescripcion());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    private MesonGranitoResponse toMesonGranitoResponse(MesonGranito item) {
        MesonGranitoResponse dto = new MesonGranitoResponse();
        dto.setIdMeson(item.getIdMeson());
        dto.setTipoGranito(item.getTipoGranito());
        dto.setLargo(item.getLargo());
        dto.setAncho(item.getAncho());
        dto.setEspesor(item.getEspesor());
        dto.setCantidad(item.getCantidad());
        dto.setPrecioUnitario(item.getPrecioUnitario());
        dto.setDescripcion(item.getDescripcion());
        dto.setSubtotal(item.getSubtotal());
        return dto;
    }

    private void validarCotizacionBaseEditable(Cotizacion cotizacionBase) {
        if (cotizacionBase == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotizacion base no encontrada");
        }

        EstadoCotizacion estado = cotizacionBase.getEstado();
        if (estado == EstadoCotizacion.APROBADA || estado == EstadoCotizacion.RECHAZADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cotizacion no se puede modificar porque esta en estado " + estado.name()
            );
        }
    }
}
