package com.interivalle.Servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interivalle.DTO.ActividadAgrupadaResponse;
import com.interivalle.DTO.AprobarCotizacionRequest;
import com.interivalle.DTO.CarpinteriaBaseRequest;
import com.interivalle.DTO.CotizacionVistaCompletaResponse;
import com.interivalle.DTO.CotizacionPersonalizadaDetalleResponse;
import com.interivalle.DTO.CotizacionActividadResponse;
import com.interivalle.DTO.CotizacionBaseResponse;
import com.interivalle.DTO.CotizacionDetalleResponse;
import com.interivalle.DTO.CotizacionHistorialResponse;
import com.interivalle.DTO.CotizacionObservacionResponse;
import com.interivalle.DTO.CotizacionResponse;
import com.interivalle.DTO.CotizacionSemanaResponse;
import com.interivalle.DTO.CrearCotizacionRequest;
import com.interivalle.DTO.GenerarCotizacionBaseRequest;
import com.interivalle.DTO.ManoObraBaseRequest;
import com.interivalle.DTO.MaterialAgrupadoResponse;
import com.interivalle.DTO.MezonBaseRequest;
import com.interivalle.DTO.ObservacionRequest;
import com.interivalle.DTO.VidrioBaseRequest;
import com.interivalle.Modelo.ActividadMaterial;
import com.interivalle.Modelo.CatalogoItem;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.CotizacionCarpinteria;
import com.interivalle.Modelo.CotizacionDetalle;
import com.interivalle.Modelo.CotizacionHistorialEstado;
import com.interivalle.Modelo.CotizacionManoObra;
import com.interivalle.Modelo.CotizacionMezon;
import com.interivalle.Modelo.CotizacionObservacion;
import com.interivalle.Modelo.CotizacionVidrio;
import com.interivalle.Modelo.Servicios;
import com.interivalle.Modelo.Solicitud;
import com.interivalle.Modelo.SolicitudServicios;
import com.interivalle.Modelo.Usuario;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Modelo.enums.TipoCotizacion;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Modelo.enums.TipoObservacion;
import com.interivalle.Repositorio.ActividadMaterialRepositorio;
import com.interivalle.Repositorio.CatalogoItemRepositorio;
import com.interivalle.Repositorio.CotizacionCarpinteriaRepositorio;
import com.interivalle.Repositorio.CotizacionDetalleRepositorio;
import com.interivalle.Repositorio.CotizacionHistorialRepositorio;
import com.interivalle.Repositorio.CotizacionManoObraRepositorio;
import com.interivalle.Repositorio.CotizacionMezonRepositorio;
import com.interivalle.Repositorio.CotizacionObservacionRepositorio;
import com.interivalle.Repositorio.CotizacionRepositorio;
import com.interivalle.Repositorio.CotizacionVidrioRepositorio;
import com.interivalle.Repositorio.ServiciosRepositorio;
import com.interivalle.Repositorio.SolicitudRepositorio;
import com.interivalle.Repositorio.UsuarioRepositorio;
import com.interivalle.Servicio.CronogramaService;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
/**
 *
 * @author mary_
 */


@Service
public class CotizacionService {

    private static final int SERVICIO_MANO_OBRA = 1;
    private static final int SERVICIO_CARPINTERIA = 2;
    private static final int SERVICIO_VIDRIO = 3;
    private static final int SERVICIO_MEZON = 4;
    private static final BigDecimal PRECIO_MT_MARMOL = new BigDecimal("850000");

    @Autowired private CotizacionRepositorio cotizacionRepo;
    @Autowired private CotizacionDetalleRepositorio detalleRepo;
    @Autowired private CotizacionObservacionRepositorio obsRepo;
    @Autowired private CotizacionHistorialRepositorio histRepo;
    @Autowired private CotizacionPersonalizadaService cotizacionPersonalizadaService;

    @Autowired private SolicitudRepositorio solicitudRepo;
    @Autowired private ServiciosRepositorio serviciosRepo;
    @Autowired private UsuarioRepositorio usuarioRepo;
    @Autowired private ActividadMaterialRepositorio actividadMaterialRepo;
    @Autowired private CatalogoItemRepositorio catalogoItemRepo;

    @Autowired private CotizacionManoObraRepositorio cotizacionManoObraRepo;
    @Autowired private CotizacionCarpinteriaRepositorio cotizacionCarpinteriaRepo;
    @Autowired private CotizacionVidrioRepositorio cotizacionVidrioRepo;
    @Autowired private CotizacionMezonRepositorio cotizacionMezonRepo;
    @Autowired private CronogramaService cronogramaServicio;

    // CREA COTIZACION MANUAL
    @Transactional
    public CotizacionResponse crearCotizacion(Integer idUsuario, CrearCotizacionRequest req) {

        Usuario usuario = usuarioRepo.findById(idUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Solicitud solicitud = solicitudRepo.findById(req.getSolicitudId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (!solicitud.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes cotizar una solicitud de otro usuario");
        }

        if (req.getDetalles() == null || req.getDetalles().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes enviar al menos 1 item en detalles");
        }

        Cotizacion cot = new Cotizacion();
        cot.setSolicitud(solicitud);
        cot.setTipo(req.getTipo());
        cot.setEstado(EstadoCotizacion.GENERADA);
        cot.setCreadaPor(usuario);

        cot = cotizacionRepo.save(cot);

        BigDecimal totalGeneral = BigDecimal.ZERO;
        BigDecimal totalManoObra = BigDecimal.ZERO;
        BigDecimal totalMateriales = BigDecimal.ZERO;
        BigDecimal totalProductos = BigDecimal.ZERO;

        for (CrearCotizacionRequest.DetalleItem item : req.getDetalles()) {

            Servicios servicio = serviciosRepo.findById(item.getServicioId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Servicio no encontrado: " + item.getServicioId()
                ));

            validarItem(item);

            BigDecimal subtotalVenta = item.getCantidad().multiply(item.getPrecioUnitario());

            CotizacionDetalle det = new CotizacionDetalle();
            det.setCotizacion(cot);
            det.setServicio(servicio);
            det.setTipoItem(item.getTipoItem());
            det.setCategoria(item.getCategoria());
            det.setSemana(item.getSemana());
            det.setDescripcion(item.getDescripcion());
            det.setActividadMaterial(item.getActividadMaterial());
            det.setCantidad(item.getCantidad());
            det.setPrecioUnitarioVenta(item.getPrecioUnitario());
            det.setSubtotalVenta(subtotalVenta);

            detalleRepo.save(det);

            totalGeneral = totalGeneral.add(subtotalVenta);

            if (item.getTipoItem() == TipoItemCotizacion.ACTIVIDAD) {
                totalManoObra = totalManoObra.add(subtotalVenta);
            } else if (item.getTipoItem() == TipoItemCotizacion.MATERIAL) {
                totalMateriales = totalMateriales.add(subtotalVenta);
            } else if (item.getTipoItem() == TipoItemCotizacion.PRODUCTO) {
                totalProductos = totalProductos.add(subtotalVenta);
            }
        }

        cot.setTotalManoObra(totalManoObra);
        cot.setTotalMateriales(totalMateriales);
        cot.setTotalProductos(totalProductos);
        cot.setTotalEstimado(totalGeneral);

        cot = cotizacionRepo.save(cot);

        guardarHistorial(cot, null, EstadoCotizacion.GENERADA, usuario);

        return toResponseCompleto(cot);
    }

    // LISTA POR CLIENTE

    public List<CotizacionResponse> listarPorCliente(Integer idUsuario) {
        List<Cotizacion> lista = cotizacionRepo.findBySolicitud_Usuario_IdUsuario(idUsuario);
        List<CotizacionResponse> out = new ArrayList<>();
        for (Cotizacion c : lista) {
            out.add(toResponseBasico(c));
        }
        return out;
    }
    
    //LISTAR TODAS LAS COTIZACIONES
    public List<CotizacionResponse> listarTodas() {
        List<Cotizacion> lista = cotizacionRepo.findAll();
        List<CotizacionResponse> out = new ArrayList<>();

        for (Cotizacion c : lista) {
            out.add(toResponseBasico(c));
        }

        return out;
    }

    // DETALLE POR ID CLIENTE
    public CotizacionResponse verDetalle(Integer idUsuario, Integer idCotizacion) {
        Cotizacion cot = getCotizacionDelUsuario(idUsuario, idCotizacion);
        return toResponseCompleto(cot);
    }
    
    //DETALLE ADMIN Y SUPERVISOR
    public CotizacionResponse verDetalleAdminSupervisor(Integer idCotizacion) {
        Cotizacion cot = cotizacionRepo.findById(idCotizacion)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cotización no encontrada"
            ));

        return toResponseCompleto(cot);
    }

    // ENVIAR

    @Transactional
    public CotizacionResponse enviar(Integer idUsuario, Integer idCotizacion) {
        Cotizacion cot = getCotizacionDelUsuario(idUsuario, idCotizacion);

        if (cot.getEstado() != EstadoCotizacion.GENERADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes enviar cotizaciones en GENERADA");
        }

        Usuario usuario = usuarioRepo.findById(idUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        EstadoCotizacion anterior = cot.getEstado();
        cot.setEstado(EstadoCotizacion.ENVIADA);
        cot = cotizacionRepo.save(cot);

        guardarHistorial(cot, anterior, EstadoCotizacion.ENVIADA, usuario);

        return toResponseCompleto(cot);
    }

    // APROBAR
    @Transactional
    public CotizacionResponse aprobar(Integer idUsuario, Integer idCotizacion, AprobarCotizacionRequest req) {
        Cotizacion cot = getCotizacionDelUsuario(idUsuario, idCotizacion);

        if (cot.getEstado() != EstadoCotizacion.GENERADA && cot.getEstado() != EstadoCotizacion.EN_REVISION) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Solo puedes aprobar cotizaciones GENERADA o EN_REVISION"
            );
        }

        if (req == null || req.getFechaInicio() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La fechaInicio es obligatoria para generar el cronograma"
            );
        }

        Usuario usuario = usuarioRepo.findById(idUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        EstadoCotizacion anterior = cot.getEstado();

        cot.setEstado(EstadoCotizacion.APROBADA);
        cot.setFechaAprobacion(LocalDateTime.now());
        cot = cotizacionRepo.save(cot);

        guardarObservacion(cot, usuario, TipoObservacion.APROBACION, req.getMensaje());
        guardarHistorial(cot, anterior, EstadoCotizacion.APROBADA, usuario);

        cronogramaServicio.crearDesdeCotizacionAprobada(cot.getIdCotizacion(), req.getFechaInicio());

        return toResponseCompleto(cot);
    }

    // RECHAZAR
    @Transactional
    public CotizacionResponse rechazar(Integer idUsuario, Integer idCotizacion, ObservacionRequest req) {
        Cotizacion cot = getCotizacionDelUsuario(idUsuario, idCotizacion);

        if (cot.getEstado() != EstadoCotizacion.GENERADA && cot.getEstado() != EstadoCotizacion.EN_REVISION) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo puedes rechazar cotizaciones GENERADA o EN_REVISION");
        }

        Usuario usuario = usuarioRepo.findById(idUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        EstadoCotizacion anterior = cot.getEstado();
        cot.setEstado(EstadoCotizacion.RECHAZADA);
        cot = cotizacionRepo.save(cot);

        guardarObservacion(cot, usuario, TipoObservacion.RECHAZO, req.getMensaje());
        guardarHistorial(cot, anterior, EstadoCotizacion.RECHAZADA, usuario);

        return toResponseCompleto(cot);
    }

    // GENERAR COTIZACION BASE DESDE SOLICITUD + GUARDAR FORMULARIOS
    @Transactional
    public CotizacionBaseResponse generarCotizacionBaseDesdeSolicitud(Integer idUsuario, GenerarCotizacionBaseRequest req) {

        Usuario usuario = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Solicitud solicitud = solicitudRepo.findById(req.getSolicitudId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        if (!solicitud.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes generar cotización para otra solicitud");
        }

        if (!"COTIZACION_BASE".equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no corresponde a cotización base");
        }

        Set<Integer> idsServiciosSolicitud = solicitud.getServiciosSeleccionados()
                .stream()
                .map((SolicitudServicios item) -> item.getServicios().getIdServicio())
                .collect(Collectors.toSet());

        if (req.getManoObra() != null && !idsServiciosSolicitud.contains(SERVICIO_MANO_OBRA)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud no contiene el servicio Mano de Obra");
        }

        if (req.getCarpinteria() != null && !idsServiciosSolicitud.contains(SERVICIO_CARPINTERIA)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud no contiene el servicio Carpintería");
        }

        if (req.getVidrio() != null && !idsServiciosSolicitud.contains(SERVICIO_VIDRIO)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud no contiene el servicio Divisiones en Vidrio");
        }

        if (req.getMezon() != null && !idsServiciosSolicitud.contains(SERVICIO_MEZON)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La solicitud no contiene el servicio Mesón Granito");
        }

        if (req.getManoObra() == null
                && req.getCarpinteria() == null
                && req.getVidrio() == null
                && req.getMezon() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar al menos una sección de cotización");
        }

        Cotizacion cot = new Cotizacion();
        cot.setSolicitud(solicitud);
        cot.setCreadaPor(usuario);
        cot.setTipo(TipoCotizacion.BASE);
        cot.setEstado(EstadoCotizacion.GENERADA);
        cot.setTotalManoObra(BigDecimal.ZERO);
        cot.setTotalMateriales(BigDecimal.ZERO);
        cot.setTotalProductos(BigDecimal.ZERO);
        cot.setTotalEstimado(BigDecimal.ZERO);

        cot = cotizacionRepo.save(cot);

        guardarHistorial(cot, null, EstadoCotizacion.GENERADA, usuario);

        if (req.getManoObra() != null) {
            CotizacionManoObra mano = new CotizacionManoObra();
            mano.setCotizacion(cot);
            mano.setMedidaAreaPrivada(req.getManoObra().getMedidaAreaPrivada());
            mano.setCantidadBanos(req.getManoObra().getCantidadBanos());
            mano.setTipoCielo(req.getManoObra().getTipoCielo());
            mano.setDivisionPared(req.getManoObra().getDivisionPared());
            cotizacionManoObraRepo.save(mano);
        }

        if (req.getCarpinteria() != null) {
            CotizacionCarpinteria carp = new CotizacionCarpinteria();
            carp.setCotizacion(cot);
            carp.setCantidadCloset(valorEntero(req.getCarpinteria().getCantidadCloset()));
            carp.setCantidadPuertas(valorEntero(req.getCarpinteria().getCantidadPuertas()));
            carp.setMuebleAltoCocina(valorDecimal(req.getCarpinteria().getMuebleAltoCocina()));
            carp.setMuebleBajoCocina(valorDecimal(req.getCarpinteria().getMuebleBajoCocina()));
            carp.setMuebleBarra(valorDecimal(req.getCarpinteria().getMuebleBarra()));
            carp.setCantidadBanos(valorEntero(req.getCarpinteria().getCantidadBanos()));
            carp.setCantidadMuebleAltoBano(valorEntero(req.getCarpinteria().getCantidadMuebleAltoBano()));
            carp.setCantidadMuebleBajoBano(valorEntero(req.getCarpinteria().getCantidadMuebleBajoBano()));
            cotizacionCarpinteriaRepo.save(carp);
        }

        if (req.getVidrio() != null) {
            CotizacionVidrio vidrio = new CotizacionVidrio();
            vidrio.setCotizacion(cot);
            vidrio.setCantidadBanos(req.getVidrio().getCantidadBanos());
            vidrio.setTipoApertura(req.getVidrio().getTipoApertura());
            vidrio.setColorAccesorios(req.getVidrio().getColorAccesorios());
            vidrio.setTieneNicho(Boolean.TRUE.equals(req.getVidrio().getTieneNicho()));
            cotizacionVidrioRepo.save(vidrio);
        }

        if (req.getMezon() != null) {
            CotizacionMezon mezon = new CotizacionMezon();
            mezon.setCotizacion(cot);
            mezon.setMezonCocina(req.getMezon().getMezonCocina());
            mezon.setMezonBarra(req.getMezon().getMezonBarra());
            mezon.setMezonLavamanos(req.getMezon().getMezonLavamanos());
            mezon.setMedidaCocina(valorDecimal(req.getMezon().getMedidaCocina()));
            mezon.setMedidaBarra(valorDecimal(req.getMezon().getMedidaBarra()));
            mezon.setMedidaLavamanos(valorDecimal(req.getMezon().getMedidaLavamanos()));
            cotizacionMezonRepo.save(mezon);
        }


        // GENERAR DETALLES DESDE CATALOGO + ACTIVIDAD_MATERIAL
        BigDecimal totalManoObra = BigDecimal.ZERO;
        BigDecimal totalMateriales = BigDecimal.ZERO;
        BigDecimal totalProductos = BigDecimal.ZERO;

        LocalDate hoy = LocalDate.now();

        if (req.getManoObra() != null) {
            System.out.println("=== ENTRANDO A GENERAR DETALLES MANO DE OBRA ===");

            List<CatalogoItem> actividadesManoObra = catalogoItemRepo.buscarVigentesPorServicioYTipo(
                    SERVICIO_MANO_OBRA,
                    TipoItemCotizacion.ACTIVIDAD,
                    hoy
            );

            System.out.println("Actividades encontradas: " + actividadesManoObra.size());

            for (CatalogoItem actividad : actividadesManoObra) {
                if (!actividadAplicaParaManoObra(actividad, req.getManoObra())) {
                    continue;
                }

                System.out.println("Actividad: " + actividad.getNombreItem());

                BigDecimal valorActividad = calcularValorActividad(actividad, req);
                System.out.println("Valor actividad: " + valorActividad);

                if (valorActividad.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                CotizacionDetalle detActividad = new CotizacionDetalle();
                detActividad.setCotizacion(cot);
                detActividad.setServicio(actividad.getServicio());
                detActividad.setTipoItem(TipoItemCotizacion.ACTIVIDAD);
                detActividad.setCategoria(actividad.getCategoria());
                detActividad.setSemana(actividad.getSemana());
               // detActividad.setDescripcion(actividad.getNombreItem());
                detActividad.setDescripcion(obtenerDescripcionCatalogo(actividad));
                detActividad.setActividadMaterial(actividad.getNombreItem());
                detActividad.setCantidad(BigDecimal.ONE);
                detActividad.setPrecioUnitarioVenta(valorActividad);
                detActividad.setSubtotalVenta(valorActividad);
                detActividad.setPrecioUnitarioProveedor(BigDecimal.ZERO);
                detActividad.setSubtotalProveedor(BigDecimal.ZERO);

                detalleRepo.save(detActividad);

                totalManoObra = totalManoObra.add(valorActividad);

                List<ActividadMaterial> materialesRelacionados =
                        actividadMaterialRepo.findByActividad_IdCatalogoItemAndActivoTrue(
                                actividad.getIdCatalogoItem()
                        );

                System.out.println("Materiales relacionados: " + materialesRelacionados.size());

                for (ActividadMaterial rel : materialesRelacionados) {
                    CatalogoItem material = rel.getMaterial();

                    if (material == null || material.getActivo() == null || !material.getActivo()) {
                        continue;
                    }

                    BigDecimal cantidadMaterial = calcularCantidadMaterial(rel, req);

                    if (cantidadMaterial.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    BigDecimal precioVenta = material.getPrecioUnitarioVenta() != null
                            ? material.getPrecioUnitarioVenta()
                            : BigDecimal.ZERO;

                    BigDecimal precioProveedor = material.getPrecioUnitarioProveedor() != null
                            ? material.getPrecioUnitarioProveedor()
                            : BigDecimal.ZERO;

                    BigDecimal subtotalVenta = cantidadMaterial.multiply(precioVenta);
                    BigDecimal subtotalProveedor = cantidadMaterial.multiply(precioProveedor);

                    CotizacionDetalle detMaterial = new CotizacionDetalle();
                    detMaterial.setCotizacion(cot);
                    detMaterial.setServicio(material.getServicio());
                    detMaterial.setTipoItem(TipoItemCotizacion.MATERIAL);
                    detMaterial.setCategoria(material.getCategoria());
                    detMaterial.setSemana(rel.getSemana() != null ? rel.getSemana() : actividad.getSemana());
                    //detMaterial.setDescripcion(material.getNombreItem());
                    detMaterial.setDescripcion(obtenerDescripcionCatalogo(material));
                    detMaterial.setActividadMaterial(actividad.getNombreItem());
                    detMaterial.setCantidad(cantidadMaterial);
                    detMaterial.setPrecioUnitarioVenta(precioVenta);
                    detMaterial.setSubtotalVenta(subtotalVenta);
                    detMaterial.setPrecioUnitarioProveedor(precioProveedor);
                    detMaterial.setSubtotalProveedor(subtotalProveedor);

                    detalleRepo.save(detMaterial);

                    totalMateriales = totalMateriales.add(subtotalVenta);
                }
            }
        }
        
        if (req.getCarpinteria() != null) {
        System.out.println("=== ENTRANDO A GENERAR DETALLES CARPINTERIA ===");

        List<CatalogoItem> productosCarpinteria = catalogoItemRepo.buscarVigentesPorServicioYTipo(
                SERVICIO_CARPINTERIA,
                TipoItemCotizacion.PRODUCTO,
                hoy
        );

        System.out.println("Productos carpintería encontrados: " + productosCarpinteria.size());

        for (CatalogoItem producto : productosCarpinteria) {
            BigDecimal cantidad = obtenerCantidadProductoCarpinteria(producto, req.getCarpinteria());

            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal precioVenta = producto.getPrecioUnitarioVenta() != null
                    ? producto.getPrecioUnitarioVenta()
                    : BigDecimal.ZERO;

            BigDecimal precioProveedor = producto.getPrecioUnitarioProveedor() != null
                    ? producto.getPrecioUnitarioProveedor()
                    : BigDecimal.ZERO;

            BigDecimal subtotalVenta = cantidad.multiply(precioVenta);
            BigDecimal subtotalProveedor = cantidad.multiply(precioProveedor);

            CotizacionDetalle detProducto = new CotizacionDetalle();
            detProducto.setCotizacion(cot);
            detProducto.setServicio(producto.getServicio());
            detProducto.setTipoItem(TipoItemCotizacion.PRODUCTO);
            detProducto.setCategoria(producto.getCategoria());
            detProducto.setSemana(producto.getSemana());
            detProducto.setDescripcion(obtenerDescripcionCatalogo(producto));
            detProducto.setActividadMaterial(producto.getNombreItem());
            detProducto.setCantidad(cantidad);
            detProducto.setUnidad(producto.getUnidad());
            detProducto.setPrecioUnitarioVenta(precioVenta);
            detProducto.setSubtotalVenta(subtotalVenta);
            detProducto.setPrecioUnitarioProveedor(precioProveedor);
            detProducto.setSubtotalProveedor(subtotalProveedor);

            detalleRepo.save(detProducto);

            totalProductos = totalProductos.add(subtotalVenta);
        }
    }

        if (req.getVidrio() != null) {
        System.out.println("=== ENTRANDO A GENERAR DETALLES DIVISIONES EN VIDRIO ===");

        List<CatalogoItem> productosVidrio = catalogoItemRepo.buscarVigentesPorServicioYTipo(
                SERVICIO_VIDRIO,
                TipoItemCotizacion.PRODUCTO,
                hoy
        );

        System.out.println("Productos vidrio encontrados: " + productosVidrio.size());

        CatalogoItem productoVidrio = seleccionarProductoVidrio(productosVidrio, req.getVidrio());

        if (productoVidrio != null) {
            BigDecimal cantidad = BigDecimal.valueOf(req.getVidrio().getCantidadBanos());
            BigDecimal subtotalVenta = guardarDetalleProducto(cot, productoVidrio, cantidad);
            totalProductos = totalProductos.add(subtotalVenta);
        }
    }

        if (req.getMezon() != null) {
        System.out.println("=== ENTRANDO A GENERAR DETALLES MESON MARMOL / GRANITO ===");

        List<CatalogoItem> productosMezon = catalogoItemRepo.buscarVigentesPorServicioYTipo(
                SERVICIO_MEZON,
                TipoItemCotizacion.PRODUCTO,
                hoy
        );

        System.out.println("Productos meson encontrados: " + productosMezon.size());

        if (!productosMezon.isEmpty()) {
            BigDecimal subtotalVenta = guardarDetallesProductosMezon(cot, productosMezon, req.getMezon());
            totalProductos = totalProductos.add(subtotalVenta);
        }
    }

        cot.setTotalManoObra(totalManoObra);
        cot.setTotalMateriales(totalMateriales);
        cot.setTotalProductos(totalProductos);
        cot.setTotalEstimado(totalManoObra.add(totalMateriales).add(totalProductos));

        cotizacionRepo.save(cot);

        CotizacionBaseResponse resp = new CotizacionBaseResponse();
        resp.setSolicitudId(solicitud.getIdSolicitud());
        resp.setIdCotizacion(cot.getIdCotizacion());
        resp.setMensaje("Cotización base guardada correctamente");
        resp.setManoObraProcesada(req.getManoObra() != null);
        resp.setCarpinteriaProcesada(req.getCarpinteria() != null);
        resp.setVidrioProcesado(req.getVidrio() != null);
        resp.setMezonProcesado(req.getMezon() != null);

        return resp;
    }
    
    private String obtenerDescripcionCatalogo(CatalogoItem item) {
    if (item == null) {
        return "";
    }

    if (item.getDescripcion() != null && !item.getDescripcion().trim().isEmpty()) {
        return item.getDescripcion().trim();
    }

    if (item.getNombreItem() != null) {
        return item.getNombreItem().trim();
    }

    return "";
}
    
    private BigDecimal obtenerAreaTotalDesdeParams(String paramsJson) {
    try {
        if (paramsJson == null || paramsJson.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(paramsJson);

        if (root.has("areaTotal") && !root.get("areaTotal").isNull()) {
            return root.get("areaTotal").decimalValue();
        }

        BigDecimal areaPiso = root.has("areaPiso") && !root.get("areaPiso").isNull()
                ? root.get("areaPiso").decimalValue()
                : BigDecimal.ZERO;

        BigDecimal areaPared = root.has("areaPared") && !root.get("areaPared").isNull()
                ? root.get("areaPared").decimalValue()
                : BigDecimal.ZERO;

        return areaPiso.add(areaPared);

    } catch (Exception e) {
        return BigDecimal.ZERO;
    }
}
    
private Integer obtenerCantidadSegunActividad(CatalogoItem actividad, GenerarCotizacionBaseRequest req) {
    if (actividad == null || actividad.getNombreItem() == null || req.getManoObra() == null) {
        return null;
    }

    String texto = textoCatalogo(actividad);

    if (texto.contains("poyo")) {
        return obtenerCantidadPoyos(actividad, req.getManoObra());
    }

    if (texto.contains("centrar luces") ||  texto.contains("punto electrico") || texto.contains("puntos electricos")) {
        return req.getManoObra().getCantidadPuntosElectricos();
    }

    return null;
}

private BigDecimal obtenerMetrosCuadradosSegunActividad(CatalogoItem actividad, GenerarCotizacionBaseRequest req) {
    if (actividad == null || actividad.getNombreItem() == null || req.getManoObra() == null) {
        return null;
    }

    String nombre = actividad.getNombreItem().toLowerCase().trim();

    if (nombre.contains("muro en drywall") || nombre.contains("muro") ){
        return req.getManoObra().getMetrosCuadradosMuro();
    }

    if (nombre.contains("drywall en cielo") || nombre.contains("cielo")) {
        return req.getManoObra().getMetrosCuadradosCielo();
    }

    if (nombre.contains("tapar tuberias") || nombre.contains("tapar tuberías")) {
        return primerValorPositivo(
                req.getManoObra().getMetrosCuadradosTaparTuberias(),
                obtenerM2DesdeParams(actividad.getParamsJson())
        );
    }

    if (nombre.contains("panel yeso")) {
        return req.getManoObra().getMetrosCuadradosPanelYeso();
    }

    return null;
}

private BigDecimal obtenerMetrosCuadradosCondicionado(CatalogoItem actividad, GenerarCotizacionBaseRequest req) {
    if (actividad == null || req.getManoObra() == null) {
        return null;
    }

    ManoObraBaseRequest manoObra = req.getManoObra();
    String texto = textoCatalogo(actividad);

    if (texto.contains("muro en drywall") || texto.contains("pared division") || texto.contains("division en drywall")) {
        return primerValorPositivo(
                manoObra.getMetrosCuadradosMuro(),
                obtenerM2DesdeParams(actividad.getParamsJson())
        );
    }

    if (texto.contains("drywall en cielo") || (texto.contains("drywall") && texto.contains("cielo"))) {
        return primerValorPositivo(
                manoObra.getMetrosCuadradosCielo(),
                obtenerM2DesdeParams(actividad.getParamsJson())
        );
    }

    if (texto.contains("tapar tuberias")) {
        return primerValorPositivo(
                manoObra.getMetrosCuadradosTaparTuberias(),
                obtenerM2DesdeParams(actividad.getParamsJson())
        );
    }

    if (texto.contains("panel yeso")) {
        return manoObra.getMetrosCuadradosPanelYeso();
    }

    return null;
}

private boolean actividadAplicaParaManoObra(CatalogoItem actividad, ManoObraBaseRequest manoObra) {
    if (actividad == null || manoObra == null) {
        return false;
    }

    String texto = textoCatalogo(actividad);

    if (texto.contains("drywall en cielo") || (texto.contains("drywall") && texto.contains("cielo"))) {
        return esTipoCielo(manoObra, "DRYWALL");
    }

    if (texto.contains("muro en drywall") || texto.contains("pared division") || texto.contains("division en drywall")) {
        return Boolean.TRUE.equals(manoObra.getDivisionPared());
    }

    if (texto.contains("tapar tuberias")) {
        return esTipoCielo(manoObra, "ESTUCO");
    }

    if (texto.contains("centrar luces") || texto.contains("punto electrico") || texto.contains("puntos electricos")) {
        return manoObra.getCantidadPuntosElectricos() != null && manoObra.getCantidadPuntosElectricos() > 0;
    }

    if (texto.contains("poyo")) {
        return obtenerCantidadPoyos(actividad, manoObra) > 0;
    }

    if (texto.contains("bano principal")) {
        return cantidadBanos(manoObra) >= 1;
    }

    if (texto.contains("bano social")) {
        return cantidadBanos(manoObra) >= 2;
    }

    return true;
}

private BigDecimal obtenerFactorActividad(CatalogoItem actividad, ManoObraBaseRequest manoObra) {
    BigDecimal factorCatalogo = actividad.getFactor() != null
            ? actividad.getFactor()
            : BigDecimal.ONE;

    String texto = textoCatalogo(actividad);

    if (texto.contains("estuco")) {
        return esTipoCielo(manoObra, "ESTUCO")
                ? new BigDecimal("3")
                : new BigDecimal("2");
    }

    return factorCatalogo;
}

private boolean esTipoCielo(ManoObraBaseRequest manoObra, String esperado) {
    return normalizarComparacion(manoObra.getTipoCielo())
            .contains(normalizarComparacion(esperado));
}

private int cantidadBanos(ManoObraBaseRequest manoObra) {
    return manoObra.getCantidadBanos() == null ? 0 : manoObra.getCantidadBanos();
}

private int obtenerCantidadPoyos(CatalogoItem actividad, ManoObraBaseRequest manoObra) {
    if (manoObra != null && manoObra.getCantidadPoyos() != null && manoObra.getCantidadPoyos() > 0) {
        return manoObra.getCantidadPoyos();
    }

    if (actividad != null && actividad.getFactor() != null && actividad.getFactor().compareTo(BigDecimal.ZERO) > 0) {
        return actividad.getFactor().intValue();
    }

    return 0;
}

private BigDecimal primerValorPositivo(BigDecimal... valores) {
    for (BigDecimal valor : valores) {
        if (esPositivo(valor)) {
            return valor;
        }
    }
    return BigDecimal.ZERO;
}

private boolean esPositivo(BigDecimal valor) {
    return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
}

private BigDecimal obtenerM2DesdeParams(String paramsJson) {
    try {
        if (paramsJson == null || paramsJson.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(paramsJson);

        if (root.has("M2") && !root.get("M2").isNull()) {
            return root.get("M2").decimalValue();
        }

        if (root.has("m2") && !root.get("m2").isNull()) {
            return root.get("m2").decimalValue();
        }

        return obtenerAreaTotalDesdeParams(paramsJson);
    } catch (Exception e) {
        return BigDecimal.ZERO;
    }
}

private BigDecimal calcularValorActividad(CatalogoItem actividad, GenerarCotizacionBaseRequest req) {
    String formula = actividad.getFormulaCode() == null ? "" : actividad.getFormulaCode().trim();

    BigDecimal precio = actividad.getPrecioUnitarioVenta() != null
            ? actividad.getPrecioUnitarioVenta()
            : BigDecimal.ZERO;

    BigDecimal factor = obtenerFactorActividad(actividad, req.getManoObra());

    switch (formula) {
        case "FIJO":
            return precio;

        case "AREA_PRIVADA_X_FACTOR":
            if (req.getManoObra() == null || req.getManoObra().getMedidaAreaPrivada() == null) {
                return BigDecimal.ZERO;
            }
            return precio.multiply(
                    factor.multiply(BigDecimal.valueOf(req.getManoObra().getMedidaAreaPrivada()))
            );

        case "AREA_PRIVADA_X_PRECIO":
            if (req.getManoObra() == null || req.getManoObra().getMedidaAreaPrivada() == null) {
                return BigDecimal.ZERO;
            }
            return precio.multiply(
                    BigDecimal.valueOf(req.getManoObra().getMedidaAreaPrivada())
            );

        case "METRO_CUADRADO_X_PRECIO":
            BigDecimal metros2 = obtenerMetrosCuadradosCondicionado(actividad, req);
            if (metros2 == null || metros2.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            System.out.println("Metros cuadrados: " + metros2);
            return precio.multiply(metros2);

        case "CANTIDAD_X_PRECIO":
            Integer cantidad = obtenerCantidadSegunActividad(actividad, req);
            if (cantidad == null || cantidad <= 0) {
                return BigDecimal.ZERO;
            }
            return precio.multiply(BigDecimal.valueOf(cantidad));

        case "AREA_FIJA_X_PRECIO":
            BigDecimal areaFija = obtenerAreaTotalDesdeParams(actividad.getParamsJson());
            if (areaFija == null || areaFija.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }
            return precio.multiply(areaFija);

        default:
            return BigDecimal.ZERO;
    }
}
        
        
        private BigDecimal calcularCantidadMaterial(ActividadMaterial rel, GenerarCotizacionBaseRequest req) {
            if (rel.getCantidad() == null) {
                return BigDecimal.ZERO;
            }
            return rel.getCantidad();
        }

        private BigDecimal guardarDetalleProducto(Cotizacion cot, CatalogoItem producto, BigDecimal cantidad) {
            return guardarDetalleProducto(cot, producto, cantidad, null);
        }

        private BigDecimal guardarDetalleProducto(Cotizacion cot, CatalogoItem producto, BigDecimal cantidad, BigDecimal precioVentaOverride) {
            return guardarDetalleProducto(cot, producto, cantidad, precioVentaOverride, null);
        }

        private BigDecimal guardarDetalleProducto(
                Cotizacion cot,
                CatalogoItem producto,
                BigDecimal cantidad,
                BigDecimal precioVentaOverride,
                String descripcionOverride
        ) {
            BigDecimal precioVenta = precioVentaOverride != null
                    ? precioVentaOverride
                    : producto.getPrecioUnitarioVenta() != null
                    ? producto.getPrecioUnitarioVenta()
                    : BigDecimal.ZERO;

            BigDecimal precioProveedor = producto.getPrecioUnitarioProveedor() != null
                    ? producto.getPrecioUnitarioProveedor()
                    : BigDecimal.ZERO;

            BigDecimal subtotalVenta = cantidad.multiply(precioVenta);
            BigDecimal subtotalProveedor = cantidad.multiply(precioProveedor);

            CotizacionDetalle detProducto = new CotizacionDetalle();
            detProducto.setCotizacion(cot);
            detProducto.setServicio(producto.getServicio());
            detProducto.setTipoItem(TipoItemCotizacion.PRODUCTO);
            detProducto.setCategoria(producto.getCategoria());
            detProducto.setSemana(producto.getSemana());
            String descripcionProducto = descripcionOverride != null && !descripcionOverride.trim().isEmpty()
                    ? descripcionOverride.trim()
                    : obtenerDescripcionCatalogo(producto);
            String actividadProducto = descripcionOverride != null && !descripcionOverride.trim().isEmpty()
                    ? descripcionProducto
                    : producto.getNombreItem();
            detProducto.setDescripcion(descripcionProducto);
            detProducto.setActividadMaterial(actividadProducto);
            detProducto.setCantidad(cantidad);
            detProducto.setUnidad(producto.getUnidad());
            detProducto.setPrecioUnitarioVenta(precioVenta);
            detProducto.setSubtotalVenta(subtotalVenta);
            detProducto.setPrecioUnitarioProveedor(precioProveedor);
            detProducto.setSubtotalProveedor(subtotalProveedor);

            detalleRepo.save(detProducto);

            return subtotalVenta;
        }

        private BigDecimal guardarDetallesProductosMezon(Cotizacion cot, List<CatalogoItem> productos, MezonBaseRequest req) {
            BigDecimal total = BigDecimal.ZERO;

            if (req == null || productos == null || productos.isEmpty()) {
                return total;
            }

            if (Boolean.TRUE.equals(req.getMezonCocina())) {
                total = total.add(guardarDetalleProductoMezon(
                        cot,
                        seleccionarProductoMezon(productos, "cocina"),
                        medidaMezon(req.getMedidaCocina()),
                        "Mezon cocina"
                ));
            }

            if (Boolean.TRUE.equals(req.getMezonBarra())) {
                total = total.add(guardarDetalleProductoMezon(
                        cot,
                        seleccionarProductoMezon(productos, "barra"),
                        medidaMezon(req.getMedidaBarra()),
                        "Mezon barra"
                ));
            }

            if (Boolean.TRUE.equals(req.getMezonLavamanos())) {
                total = total.add(guardarDetalleProductoMezon(
                        cot,
                        seleccionarProductoMezon(productos, "lavamanos"),
                        medidaMezon(req.getMedidaLavamanos()),
                        "Mezon lavamanos"
                ));
            }

            return total;
        }

        private BigDecimal guardarDetalleProductoMezon(
                Cotizacion cot,
                CatalogoItem producto,
                BigDecimal cantidad,
                String descripcion
        ) {
            if (producto == null || cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }

            return guardarDetalleProducto(cot, producto, cantidad, PRECIO_MT_MARMOL, descripcion);
        }

        private CatalogoItem seleccionarProductoMezon(List<CatalogoItem> productos, String tipo) {
            if (productos == null || productos.isEmpty()) {
                return null;
            }

            for (CatalogoItem producto : productos) {
                if (productoMezonCoincide(producto, tipo)) {
                    return producto;
                }
            }

            return productos.get(0);
        }

        private boolean productoMezonCoincide(CatalogoItem producto, String tipo) {
            String texto = textoCatalogo(producto);

            if ("lavamanos".equals(tipo)) {
                return texto.contains("lavamanos")
                        || texto.contains("lavabo")
                        || texto.contains("bano");
            }

            return texto.contains(tipo);
        }

        private BigDecimal obtenerCantidadProductoVidrio(CatalogoItem producto, VidrioBaseRequest req) {
            if (producto == null || req == null || req.getCantidadBanos() == null || req.getCantidadBanos() <= 0) {
                return BigDecimal.ZERO;
            }

            String textoCatalogo = textoCatalogo(producto);

            if (!coincideNichoVidrio(textoCatalogo, req.getTieneNicho())) {
                return BigDecimal.ZERO;
            }

            if (!coincideTipoApertura(textoCatalogo, req.getTipoApertura())) {
                return BigDecimal.ZERO;
            }

            if (!coincideColorAccesorios(textoCatalogo, req.getColorAccesorios())) {
                return BigDecimal.ZERO;
            }

            return BigDecimal.valueOf(req.getCantidadBanos());
        }

        private CatalogoItem seleccionarProductoVidrio(List<CatalogoItem> productos, VidrioBaseRequest req) {
            CatalogoItem mejorProducto = null;
            int mejorPuntaje = -1;

            for (CatalogoItem producto : productos) {
                BigDecimal cantidad = obtenerCantidadProductoVidrio(producto, req);

                if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                int puntaje = puntajeProductoVidrio(producto, req);
                if (puntaje > mejorPuntaje) {
                    mejorProducto = producto;
                    mejorPuntaje = puntaje;
                }
            }

            return mejorProducto;
        }

        private int puntajeProductoVidrio(CatalogoItem producto, VidrioBaseRequest req) {
            String texto = textoCatalogo(producto);
            boolean coincideCaja = Boolean.TRUE.equals(req.getTieneNicho())
                    ? productoVidrioConCaja(texto)
                    : productoVidrioSinCaja(texto);
            boolean coincideTipo = textoTieneTipoApertura(texto, req.getTipoApertura());
            boolean coincideColor = textoTieneColorAccesorios(texto, req.getColorAccesorios());

            if (coincideCaja && coincideTipo && coincideColor) {
                return 1000;
            }

            if (coincideTipo && coincideColor) {
                return 900;
            }

            if (coincideCaja && coincideTipo) {
                return 800;
            }

            if (coincideCaja && coincideColor) {
                return 700;
            }

            if (coincideTipo) {
                return 600;
            }

            if (coincideCaja) {
                return 500;
            }

            if (coincideColor) {
                return 400;
            }

            return 0;
        }

        private BigDecimal obtenerCantidadProductoMezon(CatalogoItem producto, MezonBaseRequest req) {
            if (producto == null || req == null) {
                return BigDecimal.ZERO;
            }

            String textoCatalogo = textoCatalogo(producto);
            boolean esCocina = textoCatalogo.contains("cocina");
            boolean esBarra = textoCatalogo.contains("barra");
            boolean esLavamanos = textoCatalogo.contains("lavamanos")
                    || textoCatalogo.contains("lavabo")
                    || textoCatalogo.contains("bano");

            BigDecimal cantidad = BigDecimal.ZERO;

            if (esCocina && Boolean.TRUE.equals(req.getMezonCocina())) {
                cantidad = cantidad.add(medidaMezon(req.getMedidaCocina()));
            }
            if (esBarra && Boolean.TRUE.equals(req.getMezonBarra())) {
                cantidad = cantidad.add(medidaMezon(req.getMedidaBarra()));
            }
            if (esLavamanos && Boolean.TRUE.equals(req.getMezonLavamanos())) {
                cantidad = cantidad.add(medidaMezon(req.getMedidaLavamanos()));
            }

            if (esCocina || esBarra || esLavamanos) {
                return cantidad;
            }

            return totalMedidasMezonSeleccionadas(req);
        }

        private BigDecimal totalMedidasMezonSeleccionadas(MezonBaseRequest req) {
            BigDecimal cantidad = BigDecimal.ZERO;
            if (Boolean.TRUE.equals(req.getMezonCocina())) {
                cantidad = cantidad.add(medidaMezon(req.getMedidaCocina()));
            }
            if (Boolean.TRUE.equals(req.getMezonBarra())) {
                cantidad = cantidad.add(medidaMezon(req.getMedidaBarra()));
            }
            if (Boolean.TRUE.equals(req.getMezonLavamanos())) {
                cantidad = cantidad.add(medidaMezon(req.getMedidaLavamanos()));
            }
            return cantidad;
        }

        private BigDecimal medidaMezon(BigDecimal medida) {
            if (medida == null || medida.compareTo(BigDecimal.ZERO) <= 0) {
                return BigDecimal.ZERO;
            }

            return medida;
        }

        private boolean coincideTipoApertura(String textoCatalogo, String tipoApertura) {
            String tipo = normalizarComparacion(tipoApertura);
            boolean seleccionCorrediza = tipo.contains("corrediz");
            boolean seleccionBatiente = tipo.contains("batiente");
            boolean productoCorrediza = textoCatalogo.contains("corrediz");
            boolean productoBatiente = textoCatalogo.contains("batiente");

            if (seleccionCorrediza && productoBatiente && !productoCorrediza) {
                return false;
            }
            if (seleccionBatiente && productoCorrediza && !productoBatiente) {
                return false;
            }
            return true;
        }

        private boolean coincideNichoVidrio(String textoCatalogo, Boolean tieneNicho) {
            boolean seleccionConNicho = Boolean.TRUE.equals(tieneNicho);
            boolean productoConCaja = productoVidrioConCaja(textoCatalogo);
            boolean productoSinCaja = productoVidrioSinCaja(textoCatalogo);

            if (seleccionConNicho && productoSinCaja) {
                return false;
            }

            if (!seleccionConNicho && productoConCaja && !productoSinCaja) {
                return false;
            }

            return true;
        }

        private boolean productoVidrioConCaja(String textoCatalogo) {
            return textoCatalogo.contains("con caja") || textoCatalogo.contains("con nicho");
        }

        private boolean productoVidrioSinCaja(String textoCatalogo) {
            return textoCatalogo.contains("sin caja") || textoCatalogo.contains("sin nicho");
        }

        private boolean textoTieneTipoApertura(String textoCatalogo, String tipoApertura) {
            String tipo = normalizarComparacion(tipoApertura);

            if (tipo.contains("corrediz")) {
                return textoCatalogo.contains("corrediz");
            }

            if (tipo.contains("batiente")) {
                return textoCatalogo.contains("batiente");
            }

            return false;
        }

        private boolean textoTieneColorAccesorios(String textoCatalogo, String colorAccesorios) {
            String color = normalizarComparacion(colorAccesorios);

            if (color.contains("negro")) {
                return textoCatalogo.contains("negro");
            }

            if (color.contains("plateado") || color.contains("plata") || color.contains("cromado")) {
                return textoCatalogo.contains("plateado")
                        || textoCatalogo.contains("plata")
                        || textoCatalogo.contains("cromado");
            }

            return false;
        }

        private boolean coincideColorAccesorios(String textoCatalogo, String colorAccesorios) {
            String color = normalizarComparacion(colorAccesorios);
            boolean seleccionNegro = color.contains("negro");
            boolean seleccionPlateado = color.contains("plateado") || color.contains("plata") || color.contains("cromado");
            boolean productoNegro = textoCatalogo.contains("negro");
            boolean productoPlateado = textoCatalogo.contains("plateado")
                    || textoCatalogo.contains("plata")
                    || textoCatalogo.contains("cromado");

            if (seleccionNegro && productoPlateado && !productoNegro) {
                return false;
            }
            if (seleccionPlateado && productoNegro && !productoPlateado) {
                return false;
            }
            return true;
        }

        private String textoCatalogo(CatalogoItem item) {
            return normalizarComparacion(
                    valorTexto(item.getCodigo()) + " "
                    + valorTexto(item.getCategoria()) + " "
                    + valorTexto(item.getNombreItem()) + " "
                    + valorTexto(item.getDescripcion())
            );
        }

        private String valorTexto(String texto) {
            return texto == null ? "" : texto;
        }

        private String normalizarComparacion(String texto) {
            if (texto == null) {
                return "";
            }

            String textoLimpio = texto
                    .replace('¤', 'n')
                    .replace('¥', 'n')
                    .replace("Ã±", "n")
                    .replace("Ã‘", "n");

            String sinAcentos = Normalizer.normalize(textoLimpio, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "");

            return sinAcentos.toLowerCase().trim();
        }

    // HELPERS DE NEGOCIO + VALIDACION
    private void validarItem(CrearCotizacionRequest.DetalleItem item) {
        if (item.getTipoItem() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipoItem es obligatorio");
        }
        if (item.getCantidad() == null || item.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cantidad debe ser > 0");
        }
        if (item.getPrecioUnitario() == null || item.getPrecioUnitario().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "precioUnitario no puede ser negativo");
        }
    }

    private Cotizacion getCotizacionDelUsuario(Integer idUsuario, Integer idCotizacion) {
        return cotizacionRepo.findByIdCotizacionAndSolicitud_Usuario_IdUsuario(idCotizacion, idUsuario)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada o sin acceso"));
    }

    private void guardarObservacion(Cotizacion cot, Usuario usuario, TipoObservacion tipo, String mensaje) {
        if (mensaje == null || mensaje.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mensaje es obligatorio");
        }

        CotizacionObservacion obs = new CotizacionObservacion();
        obs.setCotizacion(cot);
        obs.setUsuario(usuario);
        obs.setTipo(tipo);
        obs.setMensaje(mensaje.trim());
        obsRepo.save(obs);
    }

    private void guardarHistorial(Cotizacion cot, EstadoCotizacion anterior, EstadoCotizacion nuevo, Usuario usuario) {
        CotizacionHistorialEstado h = new CotizacionHistorialEstado();
        h.setCotizacion(cot);
        h.setEstadoAnterior(anterior == null ? EstadoCotizacion.GENERADA : anterior);
        h.setEstadoNuevo(nuevo);
        h.setCambiadoPor(usuario);
        histRepo.save(h);
    }


    // MAPPERS -> RESPONSE DTOs
    private CotizacionResponse toResponseBasico(Cotizacion cot) {
        CotizacionResponse r = new CotizacionResponse();
        r.setIdCotizacion(cot.getIdCotizacion());

        r.setSolicitudId(cot.getSolicitud().getIdSolicitud());
        r.setNombreProyecto(cot.getSolicitud().getNombreProyectoUsuario());
        r.setNombreUsuario(cot.getSolicitud().getUsuario().getNombreUsuario());

        r.setTipo(cot.getTipo());
        r.setEstado(cot.getEstado());

        r.setTotalManoObra(cot.getTotalManoObra());
        r.setTotalMateriales(cot.getTotalMateriales());
        r.setTotalProductos(cot.getTotalProductos());
        r.setTotalEstimado(cot.getTotalEstimado());

        r.setFechaCreacion(cot.getFechaCreacion());
        r.setFechaActualizacion(cot.getFechaActualizacion());

        r.setDetalles(null);
        r.setSemanas(null);
        r.setActividades(null);
        r.setObservaciones(null);
        r.setHistorial(null);

        return r;
    }

    private CotizacionResponse toResponseCompleto(Cotizacion cot) {
    CotizacionResponse r = toResponseBasico(cot);

    List<CotizacionDetalle> detalles = detalleRepo.findByCotizacion_IdCotizacion(cot.getIdCotizacion());
    detalles.sort(Comparator
        .comparing((CotizacionDetalle d) -> d.getServicio().getIdServicio())
        .thenComparing(d -> d.getTipoItem().name())
        .thenComparing(d -> d.getCategoria() == null ? "" : d.getCategoria())
        .thenComparing(d -> d.getSemana() == null ? 0 : d.getSemana())
        .thenComparing(d -> d.getActividadMaterial() == null ? "" : d.getActividadMaterial())
        .thenComparing(d -> d.getDescripcion() == null ? "" : d.getDescripcion())
    );

    List<CotizacionDetalleResponse> detResp = detalles.stream()
        .map(this::toDetalleResponse)
        .collect(Collectors.toList());

    r.setDetalles(detResp);
    r.setSemanas(agruparPorSemanas(detResp));

    List<CotizacionObservacion> obs = obsRepo.findByCotizacion_IdCotizacionOrderByFechaAsc(cot.getIdCotizacion());
    List<CotizacionObservacionResponse> obsResp = new ArrayList<>();
    for (CotizacionObservacion o : obs) {
        CotizacionObservacionResponse or = new CotizacionObservacionResponse();
        or.setIdObservacion(o.getIdObservacion());
        or.setTipo(o.getTipo());
        or.setMensaje(o.getMensaje());
        or.setUsuarioNombre(o.getUsuario().getNombreUsuario());
        or.setFecha(o.getFecha());
        obsResp.add(or);
    }
    r.setObservaciones(obsResp);

    List<CotizacionHistorialEstado> hist = histRepo.findByCotizacion_IdCotizacionOrderByFechaAsc(cot.getIdCotizacion());
    List<CotizacionHistorialResponse> histResp = new ArrayList<>();
    for (CotizacionHistorialEstado h : hist) {
        CotizacionHistorialResponse hr = new CotizacionHistorialResponse();
        hr.setIdHistorial(h.getIdHistorial());
        hr.setEstadoAnterior(h.getEstadoAnterior());
        hr.setEstadoNuevo(h.getEstadoNuevo());
        hr.setUsuarioNombre(h.getCambiadoPor().getNombreUsuario());
        hr.setFecha(h.getFecha());
        histResp.add(hr);
    }
    r.setHistorial(histResp);

    return r;
    }

    private CotizacionDetalleResponse toDetalleResponse(CotizacionDetalle d) {
        CotizacionDetalleResponse dr = new CotizacionDetalleResponse();

        dr.setIdDetalle(d.getIdDetalle());
        dr.setServicioId(d.getServicio().getIdServicio());
        dr.setNombreServicio(d.getServicio().getNombreServicio());

        dr.setTipoItem(d.getTipoItem());
        dr.setCategoria(d.getCategoria());
        dr.setSemana(d.getSemana());
        dr.setDescripcion(d.getDescripcion());
        dr.setActividadMaterial(d.getActividadMaterial());

        dr.setCantidad(d.getCantidad());
        dr.setUnidad(d.getUnidad());
        dr.setPrecioUnitarioVenta(d.getPrecioUnitarioVenta());
        dr.setSubtotalVenta(d.getSubtotalVenta());
        dr.setPrecioUnitarioProveedor(d.getPrecioUnitarioProveedor());
        dr.setSubtotalProveedor(d.getSubtotalProveedor());

        return dr;
    }

private List<CotizacionSemanaResponse> agruparPorSemanas(List<CotizacionDetalleResponse> detalles) {
    Map<Integer, List<CotizacionDetalleResponse>> agrupado = detalles.stream()
        .collect(Collectors.groupingBy(d -> d.getSemana() == null ? 0 : d.getSemana()));

    return agrupado.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .map(entry -> {
            Integer semana = entry.getKey();
            List<CotizacionDetalleResponse> items = entry.getValue();

            BigDecimal totalManoObra = sumarPorTipo(items, TipoItemCotizacion.ACTIVIDAD);
            BigDecimal totalMateriales = sumarPorTipo(items, TipoItemCotizacion.MATERIAL);
            BigDecimal totalProductos = sumarPorTipo(items, TipoItemCotizacion.PRODUCTO);

            CotizacionSemanaResponse s = new CotizacionSemanaResponse();
            s.setSemana(semana);
            s.setActividades(agruparActividadesConMateriales(items));
            s.setTotalManoObra(totalManoObra);
            s.setTotalMateriales(totalMateriales);
            s.setTotalProductos(totalProductos);
            s.setTotalSemana(totalManoObra);

            return s;
        })
        .collect(Collectors.toList());
}
    
    private String normalizarTexto(String texto) {
    return texto == null ? "" : texto.trim().toLowerCase();
    }
    
    private List<ActividadAgrupadaResponse> agruparActividadesConMateriales(List<CotizacionDetalleResponse> items) {

    List<CotizacionDetalleResponse> actividades = items.stream()
        .filter(i -> i.getTipoItem() == TipoItemCotizacion.ACTIVIDAD)
        .collect(Collectors.toList());

    List<CotizacionDetalleResponse> materiales = items.stream()
        .filter(i -> i.getTipoItem() == TipoItemCotizacion.MATERIAL)
        .collect(Collectors.toList());

    List<ActividadAgrupadaResponse> resultado = new ArrayList<>();

    for (CotizacionDetalleResponse act : actividades) {
        ActividadAgrupadaResponse actividad = new ActividadAgrupadaResponse();
        actividad.setActividad( act.getActividadMaterial() != null && !act.getActividadMaterial().trim().isEmpty()
        ? act.getActividadMaterial()
        : act.getDescripcion());
        
        actividad.setPrecioActividad(
            act.getSubtotalVenta() != null ? act.getSubtotalVenta() : BigDecimal.ZERO
        );

        String nombreActividad = normalizarTexto(
            act.getActividadMaterial() != null ? act.getActividadMaterial() : act.getDescripcion()
        );

        List<MaterialAgrupadoResponse> mats = materiales.stream()
            .filter(mat -> {
                String relacionMaterial = normalizarTexto(mat.getActividadMaterial());
                return relacionMaterial.equals(nombreActividad);
            })
            .map(mat -> {
                MaterialAgrupadoResponse m = new MaterialAgrupadoResponse();
                m.setIdDetalle(mat.getIdDetalle());
                m.setCantidad(mat.getCantidad());
                m.setMaterial(mat.getDescripcion());
                m.setPrecioMaterial(
                    mat.getSubtotalVenta() != null ? mat.getSubtotalVenta() : BigDecimal.ZERO
                );
                return m;
            })
            .collect(Collectors.toList());

        actividad.setMateriales(mats);
        resultado.add(actividad);
    }

    return resultado;
}

    private BigDecimal sumarPorTipo(List<CotizacionDetalleResponse> items, TipoItemCotizacion tipo) {
        return items.stream()
            .filter(i -> i.getTipoItem() == tipo)
            .map(i -> i.getSubtotalVenta() == null ? BigDecimal.ZERO : i.getSubtotalVenta())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public CotizacionVistaCompletaResponse obtenerVistaCompleta(Integer idUsuario, Integer idCotizacion) {
    Cotizacion cot = getCotizacionDelUsuario(idUsuario, idCotizacion);

    List<CotizacionDetalle> detalles = detalleRepo.findByCotizacion_IdCotizacion(cot.getIdCotizacion());
    detalles.sort(Comparator
        .comparing((CotizacionDetalle d) -> d.getServicio().getIdServicio())
        .thenComparing(d -> d.getTipoItem().name())
        .thenComparing(d -> d.getCategoria() == null ? "" : d.getCategoria())
        .thenComparing(d -> d.getSemana() == null ? 0 : d.getSemana())
        .thenComparing(d -> d.getActividadMaterial() == null ? "" : d.getActividadMaterial())
        .thenComparing(d -> d.getDescripcion() == null ? "" : d.getDescripcion())
    );

    List<CotizacionDetalleResponse> detalleBase = detalles.stream()
        .map(this::toDetalleResponse)
        .collect(Collectors.toList());

    CotizacionPersonalizadaDetalleResponse personalizada = null;
    BigDecimal totalAdicionales = BigDecimal.ZERO;

    try {
       // personalizada = cotizacionPersonalizadaService.obtenerDetalle(idCotizacion);
        personalizada = cotizacionPersonalizadaService.obtenerDetallePorCotizacion(idCotizacion);

        if (personalizada != null && personalizada.getTotal() != null) {
            totalAdicionales = personalizada.getTotal();
        }
    } catch (Exception e) {
        personalizada = null;
        totalAdicionales = BigDecimal.ZERO;
    }

    BigDecimal totalBase = cot.getTotalEstimado() != null
        ? cot.getTotalEstimado()
        : BigDecimal.ZERO;

    BigDecimal totalGeneral = totalBase.add(totalAdicionales);

    CotizacionVistaCompletaResponse resp = new CotizacionVistaCompletaResponse();
    resp.setIdCotizacion(cot.getIdCotizacion());
    resp.setNombreProyecto(cot.getSolicitud().getNombreProyectoUsuario());
    resp.setEstado(cot.getEstado().name());

    resp.setTotalManoObra(cot.getTotalManoObra() != null ? cot.getTotalManoObra() : BigDecimal.ZERO);
    resp.setTotalMateriales(cot.getTotalMateriales() != null ? cot.getTotalMateriales() : BigDecimal.ZERO);
    resp.setTotalProductos(cot.getTotalProductos() != null ? cot.getTotalProductos() : BigDecimal.ZERO);
    resp.setTotalEstimadoBase(totalBase);

    resp.setTotalAdicionales(totalAdicionales);
    resp.setTotalGeneral(totalGeneral);

    resp.setDetalleBase(detalleBase);
    resp.setSemanas(agruparPorSemanas(detalleBase));
    resp.setPersonalizada(personalizada);

    return resp;
    }
    
    // VISTA COMPLETA PARA DMIN Y SUPERVISOR
    public CotizacionVistaCompletaResponse obtenerVistaCompletaAdminSupervisor(Integer idCotizacion) {
        Cotizacion cot = cotizacionRepo.findById(idCotizacion)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Cotización no encontrada"
            ));

        List<CotizacionDetalle> detalles = detalleRepo.findByCotizacion_IdCotizacion(cot.getIdCotizacion());
        detalles.sort(Comparator
            .comparing((CotizacionDetalle d) -> d.getServicio().getIdServicio())
            .thenComparing(d -> d.getTipoItem().name())
            .thenComparing(d -> d.getCategoria() == null ? "" : d.getCategoria())
            .thenComparing(d -> d.getSemana() == null ? 0 : d.getSemana())
            .thenComparing(d -> d.getActividadMaterial() == null ? "" : d.getActividadMaterial())
            .thenComparing(d -> d.getDescripcion() == null ? "" : d.getDescripcion())
        );

        List<CotizacionDetalleResponse> detalleBase = detalles.stream()
            .map(this::toDetalleResponse)
            .collect(Collectors.toList());

        CotizacionPersonalizadaDetalleResponse personalizada = null;
        BigDecimal totalAdicionales = BigDecimal.ZERO;

        try {
            personalizada = cotizacionPersonalizadaService.obtenerDetallePorCotizacion(idCotizacion);

            if (personalizada != null && personalizada.getTotal() != null) {
                totalAdicionales = personalizada.getTotal();
            }
        } catch (Exception e) {
            personalizada = null;
            totalAdicionales = BigDecimal.ZERO;
        }

        BigDecimal totalBase = cot.getTotalEstimado() != null
            ? cot.getTotalEstimado()
            : BigDecimal.ZERO;

        BigDecimal totalGeneral = totalBase.add(totalAdicionales);

        CotizacionVistaCompletaResponse resp = new CotizacionVistaCompletaResponse();
        resp.setIdCotizacion(cot.getIdCotizacion());
        resp.setNombreProyecto(cot.getSolicitud().getNombreProyectoUsuario());
        resp.setEstado(cot.getEstado().name());

        resp.setTotalManoObra(cot.getTotalManoObra() != null ? cot.getTotalManoObra() : BigDecimal.ZERO);
        resp.setTotalMateriales(cot.getTotalMateriales() != null ? cot.getTotalMateriales() : BigDecimal.ZERO);
        resp.setTotalProductos(cot.getTotalProductos() != null ? cot.getTotalProductos() : BigDecimal.ZERO);
        resp.setTotalEstimadoBase(totalBase);

        resp.setTotalAdicionales(totalAdicionales);
        resp.setTotalGeneral(totalGeneral);

        resp.setDetalleBase(detalleBase);
        resp.setSemanas(agruparPorSemanas(detalleBase));
        resp.setPersonalizada(personalizada);

        return resp;
    }
    
    ///VALIDAR COTIZACION ANTES DE EDITAR    
    private void validarCotizacionEditable(Cotizacion cotizacion) {
        if (cotizacion == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada");
        }

        if (cotizacion.getEstado() == EstadoCotizacion.APROBADA ||
            cotizacion.getEstado() == EstadoCotizacion.RECHAZADA) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La cotización no se puede modificar porque está en estado " + cotizacion.getEstado().name()
            );
        }
    }
    
    private Integer valorEntero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private BigDecimal valorDecimal(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private BigDecimal obtenerCantidadProductoCarpinteria(CatalogoItem producto, CarpinteriaBaseRequest req) {
        if (producto == null || producto.getNombreItem() == null || req == null) {
            return BigDecimal.ZERO;
        }

        String texto = textoCatalogo(producto);
        String nombre = texto;

        if (texto.contains("closet")) {
            return cantidadDesdeEntero(req.getCantidadCloset());
        }

        if (texto.contains("puerta")) {
            return cantidadDesdeEntero(req.getCantidadPuertas());
        }

        if (texto.contains("mueble alto") && texto.contains("cocina")) {
            return cantidadDesdeDecimal(req.getMuebleAltoCocina());
        }

        if (texto.contains("mueble bajo") && texto.contains("cocina")) {
            return cantidadDesdeDecimal(req.getMuebleBajoCocina());
        }

        boolean esProductoBano = texto.contains("bano") || texto.contains("lavamanos");

        if (texto.contains("mueble alto") && esProductoBano) {
            return cantidadMuebleBano(
                    req.getCantidadMuebleAltoBano(),
                    req.getCantidadBanos()
            );
        }

        if (texto.contains("mueble bajo") && esProductoBano) {
            return cantidadMuebleBano(
                    req.getCantidadMuebleBajoBano(),
                    req.getCantidadBanos()
            );
        }

        // solución rápida: un solo campo del formulario alimenta ambos muebles de baño
       if (nombre.contains("mueble bajo baño") || nombre.contains("mueble bajo bano")) {
        return req.getCantidadBanos() == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(req.getCantidadBanos());
        }

        if (nombre.contains("mueble alto para baño") || nombre.contains("mueble alto para bano")) {
            return req.getCantidadBanos() == null
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(req.getCantidadBanos());
        }

        // todavía no existe en tu formulario actual
        if (nombre.contains("mueble barra")) {
            return cantidadDesdeDecimal(req.getMuebleBarra());
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal cantidadMuebleBano(Integer cantidadEspecifica, Integer cantidadBanos) {
        Integer cantidad = cantidadEspecifica != null ? cantidadEspecifica : cantidadBanos;
        return cantidadDesdeEntero(cantidad);
    }

    private BigDecimal cantidadDesdeEntero(Integer valor) {
        if (valor == null || valor <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(valor);
    }

    private BigDecimal cantidadDesdeDecimal(BigDecimal valor) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return valor;
    }
    
}
