package com.interivalle.Servicio;

import com.interivalle.DTO.ReporteMaterialesCotizacionResponse;
import com.interivalle.DTO.ReporteMaterialesCotizacionResponse.ActividadAdicional;
import com.interivalle.DTO.ReporteMaterialesCotizacionResponse.MaterialCotizacion;
import com.interivalle.Modelo.ActividadPersonalizada;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.CotizacionDetalle;
import com.interivalle.Modelo.ObraBlanca;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.ActividadPersonalizadaRepositorio;
import com.interivalle.Repositorio.CotizacionDetalleRepositorio;
import com.interivalle.Repositorio.CotizacionRepositorio;
import com.interivalle.Repositorio.ObraBlancaRepositorio;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReporteMaterialesCotizacionService {

    private final CotizacionRepositorio cotizacionRepositorio;
    private final CotizacionDetalleRepositorio detalleRepositorio;
    private final ObraBlancaRepositorio obraBlancaRepositorio;
    private final ActividadPersonalizadaRepositorio actividadRepositorio;

    public ReporteMaterialesCotizacionService(
            CotizacionRepositorio cotizacionRepositorio,
            CotizacionDetalleRepositorio detalleRepositorio,
            ObraBlancaRepositorio obraBlancaRepositorio,
            ActividadPersonalizadaRepositorio actividadRepositorio
    ) {
        this.cotizacionRepositorio = cotizacionRepositorio;
        this.detalleRepositorio = detalleRepositorio;
        this.obraBlancaRepositorio = obraBlancaRepositorio;
        this.actividadRepositorio = actividadRepositorio;
    }

    @Transactional(readOnly = true)
    public List<ReporteMaterialesCotizacionResponse> consultar(
            Integer idCotizacion,
            String nombreProyecto,
            String cliente,
            String servicio,
            String estado
    ) {
        EstadoCotizacion estadoFiltro = convertirEstado(estado);
        List<Cotizacion> cotizaciones = cotizacionRepositorio.buscarParaReporteMateriales(
                idCotizacion,
                limpiar(nombreProyecto),
                limpiar(cliente),
                limpiar(servicio),
                estadoFiltro
        );
        return cotizaciones.stream().map(this::armarReporte).collect(Collectors.toList());
    }

    private ReporteMaterialesCotizacionResponse armarReporte(Cotizacion cotizacion) {
        String proyecto = cotizacion.getSolicitud().getNombreProyectoUsuario();
        String cliente = cotizacion.getSolicitud().getUsuario().getNombreUsuario();
        List<CotizacionDetalle> detalles = detalleRepositorio
                .findByCotizacion_IdCotizacionOrderByServicio_NombreServicioAsc(cotizacion.getIdCotizacion());

        List<MaterialCotizacion> materiales = detalles.stream()
                .filter(item -> item.getTipoItem() == TipoItemCotizacion.MATERIAL)
                .map(item -> toMaterial(item, proyecto, cliente))
                .collect(Collectors.toList());

        List<ObraBlanca> adicionales = obraBlancaRepositorio
                .findByCotizacionPersonalizada_Cotizacion_IdCotizacionOrderByIdObraBlancaAsc(
                        cotizacion.getIdCotizacion()
                );
        Map<Integer, ActividadPersonalizada> actividades = adicionales.isEmpty()
                ? Collections.emptyMap()
                : actividadRepositorio.findAllById(
                        adicionales.stream().map(ObraBlanca::getIdActividad)
                                .filter(Objects::nonNull).collect(Collectors.toSet())
                ).stream().collect(Collectors.toMap(ActividadPersonalizada::getIdActividad, Function.identity()));

        List<ActividadAdicional> actividadesAdicionales = adicionales.stream()
                .map(item -> toActividadAdicional(item, actividades.get(item.getIdActividad())))
                .collect(Collectors.toList());

        BigDecimal totalMateriales = sumarMateriales(materiales);
        BigDecimal totalAdicionales = sumarAdicionales(actividadesAdicionales);

        ReporteMaterialesCotizacionResponse response = new ReporteMaterialesCotizacionResponse();
        response.setIdCotizacion(cotizacion.getIdCotizacion());
        response.setNombreProyecto(proyecto);
        response.setCliente(cliente);
        response.setEstado(cotizacion.getEstado().name());
        response.setMateriales(materiales);
        response.setActividadesAdicionales(actividadesAdicionales);
        response.setTotalMateriales(totalMateriales);
        response.setTotalActividadesAdicionales(totalAdicionales);
        response.setTotalGeneral(valor(cotizacion.getTotalEstimado()).add(totalAdicionales));
        return response;
    }

    private MaterialCotizacion toMaterial(CotizacionDetalle item, String proyecto, String cliente) {
        MaterialCotizacion dto = new MaterialCotizacion();
        dto.setNombreProyecto(proyecto);
        dto.setNumeroCotizacion(item.getCotizacion().getIdCotizacion());
        dto.setCliente(cliente);
        dto.setServicio(item.getServicio().getNombreServicio());
        dto.setActividad(item.getActividadMaterial());
        dto.setMaterial(item.getDescripcion());
        dto.setUnidad(item.getUnidad());
        dto.setCantidad(valor(item.getCantidad()));
        dto.setPrecioUnitario(valor(item.getPrecioUnitarioVenta()));
        dto.setSubtotal(valor(item.getSubtotalVenta()));
        return dto;
    }

    private ActividadAdicional toActividadAdicional(
            ObraBlanca item,
            ActividadPersonalizada actividadCatalogo
    ) {
        ActividadAdicional dto = new ActividadAdicional();
        dto.setServicio(
                actividadCatalogo != null && actividadCatalogo.getServicios() != null
                        ? actividadCatalogo.getServicios().getNombreServicio()
                        : "Obra blanca"
        );
        dto.setActividad(item.getActividad());
        dto.setLugar(item.getLugar());
        dto.setUnidad(item.getUnidad());
        dto.setCantidad(item.getCantidad());
        dto.setMedida(valor(item.getMedida()));
        dto.setPrecioUnitario(valor(item.getPrecioUnitario()));
        dto.setSubtotal(valor(item.getSubtotal()));
        return dto;
    }

    private BigDecimal sumarMateriales(List<MaterialCotizacion> items) {
        return items.stream().map(MaterialCotizacion::getSubtotal)
                .map(this::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumarAdicionales(List<ActividadAdicional> items) {
        return items.stream().map(ActividadAdicional::getSubtotal)
                .map(this::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal valor(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String limpiar(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private EstadoCotizacion convertirEstado(String estado) {
        String value = limpiar(estado);
        if (value == null) return null;
        try {
            return EstadoCotizacion.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de cotizacion no valido");
        }
    }
}
