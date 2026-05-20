package com.interivalle.Servicio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interivalle.DTO.CarpinteriaBaseRequest;
import com.interivalle.DTO.GenerarCotizacionBaseRequest;
import com.interivalle.DTO.ManoObraBaseRequest;
import com.interivalle.DTO.MezonBaseRequest;
import com.interivalle.DTO.VidrioBaseRequest;
import com.interivalle.Modelo.Actividad;
import com.interivalle.Modelo.ActividadMaterialV2;
import com.interivalle.Modelo.Cotizacion;
import com.interivalle.Modelo.CotizacionDetalle;
import com.interivalle.Modelo.Material;
import com.interivalle.Modelo.Producto;
import com.interivalle.Modelo.Solicitud;
import com.interivalle.Modelo.SolicitudServicios;
import com.interivalle.Modelo.enums.EstadoCotizacion;
import com.interivalle.Modelo.enums.TipoCotizacion;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.ActividadMaterialV2Repositorio;
import com.interivalle.Repositorio.ActividadRepositorio;
import com.interivalle.Repositorio.CotizacionDetalleRepositorio;
import com.interivalle.Repositorio.CotizacionRepositorio;
import com.interivalle.Repositorio.ProductoRepositorio;
import com.interivalle.Repositorio.SolicitudRepositorio;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CotizacionBaseV2Service {

    private static final int SERVICIO_OBRA_BLANCA = 1;
    private static final int SERVICIO_CARPINTERIA = 2;
    private static final int SERVICIO_VIDRIO = 3;
    private static final int SERVICIO_MEZON = 4;

    @Autowired
    private SolicitudRepositorio solicitudRepositorio;

    @Autowired
    private CotizacionRepositorio cotizacionRepositorio;

    @Autowired
    private CotizacionDetalleRepositorio cotizacionDetalleRepositorio;

    @Autowired
    private ActividadRepositorio actividadRepositorio;

    @Autowired
    private ActividadMaterialV2Repositorio actividadMaterialV2Repositorio;

    @Autowired
    private ProductoRepositorio productoRepositorio;

    @Transactional
    public Cotizacion generarCotizacionBaseV2(GenerarCotizacionBaseRequest request) {
        if (request == null || request.getSolicitudId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar el id de la solicitud");
        }

        Solicitud solicitud = solicitudRepositorio.findById(request.getSolicitudId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe la solicitud con ID: " + request.getSolicitudId()
                ));

        validarSolicitudBase(solicitud);

        Set<Integer> idsServicios = obtenerServiciosAProcesar(request);
        validarServiciosDeSolicitud(solicitud, idsServicios);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setSolicitud(solicitud);
        cotizacion.setCreadaPor(solicitud.getUsuario());
        cotizacion.setTipo(TipoCotizacion.BASE);
        cotizacion.setEstado(EstadoCotizacion.GENERADA);
        cotizacion.setFechaCreacion(LocalDateTime.now());
        cotizacion.setFechaActualizacion(LocalDateTime.now());
        cotizacion.setTotalManoObra(BigDecimal.ZERO);
        cotizacion.setTotalMateriales(BigDecimal.ZERO);
        cotizacion.setTotalProductos(BigDecimal.ZERO);
        cotizacion.setTotalEstimado(BigDecimal.ZERO);

        cotizacion = cotizacionRepositorio.save(cotizacion);

        BigDecimal totalManoObra = BigDecimal.ZERO;
        BigDecimal totalMateriales = BigDecimal.ZERO;
        BigDecimal totalProductos = BigDecimal.ZERO;

        for (Integer idServicio : idsServicios) {
            List<Actividad> actividades = actividadRepositorio
                    .findByServicio_IdServiciosAndActivoTrueOrderBySemanaAscOrdenAsc(idServicio);

            for (Actividad actividad : actividades) {
                if (idServicio == SERVICIO_OBRA_BLANCA
                        && !actividadAplicaParaManoObra(actividad, request.getManoObra())) {
                    continue;
                }

                BigDecimal cantidadActividad = calcularCantidadActividad(actividad, request, idServicio);

                if (cantidadActividad.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal precioActividad = valorSeguro(actividad.getPrecioUnitarioVenta());
                BigDecimal subtotalActividad = cantidadActividad.multiply(precioActividad);

                if (subtotalActividad.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                CotizacionDetalle detalleActividad = new CotizacionDetalle();
                detalleActividad.setCotizacion(cotizacion);
                detalleActividad.setServicio(actividad.getServicio());
                detalleActividad.setTipoItem(TipoItemCotizacion.ACTIVIDAD);
                detalleActividad.setCategoria(actividad.getCategoria());
                detalleActividad.setSemana(actividad.getSemana());
                detalleActividad.setDescripcion(textoPrincipal(actividad.getDescripcion(), actividad.getNombreActividad()));
                detalleActividad.setActividadMaterial(actividad.getNombreActividad());
                detalleActividad.setCantidad(redondear(cantidadActividad));
                detalleActividad.setUnidad(actividad.getUnidad());
                detalleActividad.setPrecioUnitarioVenta(redondear(precioActividad));
                detalleActividad.setSubtotalVenta(redondear(subtotalActividad));
                detalleActividad.setPrecioUnitarioProveedor(BigDecimal.ZERO);
                detalleActividad.setSubtotalProveedor(BigDecimal.ZERO);

                cotizacionDetalleRepositorio.save(detalleActividad);
                totalManoObra = totalManoObra.add(subtotalActividad);

                List<ActividadMaterialV2> materialesRelacionados = actividadMaterialV2Repositorio
                        .findByActividad_IdActividadAndActivoTrueOrderBySemanaAscIdActividadMaterialV2Asc(
                                actividad.getIdActividad()
                        );

                for (ActividadMaterialV2 relacion : materialesRelacionados) {
                    Material material = relacion.getMaterial();

                    if (material == null || !Boolean.TRUE.equals(material.getActivo())) {
                        continue;
                    }

                    BigDecimal cantidadMaterial = calcularCantidadMaterial(relacion, cantidadActividad);

                    if (cantidadMaterial.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    BigDecimal precioVentaMaterial = valorSeguro(material.getPrecioUnitarioVenta());
                    BigDecimal precioProveedorMaterial = valorSeguro(material.getPrecioUnitarioProveedor());
                    BigDecimal subtotalVentaMaterial = cantidadMaterial.multiply(precioVentaMaterial);
                    BigDecimal subtotalProveedorMaterial = cantidadMaterial.multiply(precioProveedorMaterial);

                    CotizacionDetalle detalleMaterial = new CotizacionDetalle();
                    detalleMaterial.setCotizacion(cotizacion);
                    detalleMaterial.setServicio(material.getServicio());
                    detalleMaterial.setTipoItem(TipoItemCotizacion.MATERIAL);
                    detalleMaterial.setCategoria(material.getCategoria());
                    detalleMaterial.setSemana(relacion.getSemana() != null ? relacion.getSemana() : actividad.getSemana());
                    detalleMaterial.setDescripcion(textoPrincipal(material.getDescripcion(), material.getNombreMaterial()));
                    detalleMaterial.setActividadMaterial(actividad.getNombreActividad());
                    detalleMaterial.setCantidad(redondear(cantidadMaterial));
                    detalleMaterial.setUnidad(material.getUnidad());
                    detalleMaterial.setPrecioUnitarioVenta(redondear(precioVentaMaterial));
                    detalleMaterial.setSubtotalVenta(redondear(subtotalVentaMaterial));
                    detalleMaterial.setPrecioUnitarioProveedor(redondear(precioProveedorMaterial));
                    detalleMaterial.setSubtotalProveedor(redondear(subtotalProveedorMaterial));

                    cotizacionDetalleRepositorio.save(detalleMaterial);
                    totalMateriales = totalMateriales.add(subtotalVentaMaterial);
                }
            }
        }

        for (Integer idServicio : idsServicios) {
            List<Producto> productos = productoRepositorio
                    .findByServicio_IdServiciosAndActivoTrueOrderBySemanaAscIdProductoAsc(idServicio);

            for (Producto producto : productos) {
                BigDecimal cantidadProducto = calcularCantidadProducto(producto, request, idServicio);

                if (cantidadProducto.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                BigDecimal precioVentaProducto = valorSeguro(producto.getPrecioUnitarioVenta());
                BigDecimal precioProveedorProducto = valorSeguro(producto.getPrecioUnitarioProveedor());
                BigDecimal subtotalVentaProducto = cantidadProducto.multiply(precioVentaProducto);
                BigDecimal subtotalProveedorProducto = cantidadProducto.multiply(precioProveedorProducto);

                if (subtotalVentaProducto.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                CotizacionDetalle detalleProducto = new CotizacionDetalle();
                detalleProducto.setCotizacion(cotizacion);
                detalleProducto.setServicio(producto.getServicio());
                detalleProducto.setTipoItem(TipoItemCotizacion.PRODUCTO);
                detalleProducto.setCategoria(producto.getCategoria());
                detalleProducto.setSemana(producto.getSemana());
                detalleProducto.setDescripcion(textoPrincipal(producto.getDescripcion(), producto.getNombreProducto()));
                detalleProducto.setActividadMaterial(producto.getNombreProducto());
                detalleProducto.setCantidad(redondear(cantidadProducto));
                detalleProducto.setUnidad(producto.getUnidad());
                detalleProducto.setPrecioUnitarioVenta(redondear(precioVentaProducto));
                detalleProducto.setSubtotalVenta(redondear(subtotalVentaProducto));
                detalleProducto.setPrecioUnitarioProveedor(redondear(precioProveedorProducto));
                detalleProducto.setSubtotalProveedor(redondear(subtotalProveedorProducto));

                cotizacionDetalleRepositorio.save(detalleProducto);
                totalProductos = totalProductos.add(subtotalVentaProducto);
            }
        }

        BigDecimal totalEstimado = totalManoObra
                .add(totalMateriales)
                .add(totalProductos);

        cotizacion.setTotalManoObra(redondear(totalManoObra));
        cotizacion.setTotalMateriales(redondear(totalMateriales));
        cotizacion.setTotalProductos(redondear(totalProductos));
        cotizacion.setTotalEstimado(redondear(totalEstimado));
        cotizacion.setFechaActualizacion(LocalDateTime.now());

        return cotizacionRepositorio.save(cotizacion);
    }

    private void validarSolicitudBase(Solicitud solicitud) {
        if (solicitud.getUsuario() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no tiene usuario creador");
        }

        if (!"COTIZACION_BASE".equalsIgnoreCase(textoOriginal(solicitud.getTipoSolicitud()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no corresponde a cotizacion base");
        }
    }

    private Set<Integer> obtenerServiciosAProcesar(GenerarCotizacionBaseRequest request) {
        Set<Integer> idsServicios = new LinkedHashSet<>();

        if (request.getManoObra() != null) {
            idsServicios.add(SERVICIO_OBRA_BLANCA);
        }
        if (request.getCarpinteria() != null) {
            idsServicios.add(SERVICIO_CARPINTERIA);
        }
        if (request.getVidrio() != null) {
            idsServicios.add(SERVICIO_VIDRIO);
        }
        if (request.getMezon() != null) {
            idsServicios.add(SERVICIO_MEZON);
        }

        if (idsServicios.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe enviar al menos una seccion de cotizacion base"
            );
        }

        return idsServicios;
    }

    private void validarServiciosDeSolicitud(Solicitud solicitud, Set<Integer> idsServicios) {
        Set<Integer> idsSolicitud = solicitud.getServiciosSeleccionados()
                .stream()
                .map(SolicitudServicios::getServicios)
                .filter(servicio -> servicio != null && servicio.getIdServicio() != null)
                .map(servicio -> servicio.getIdServicio())
                .collect(Collectors.toSet());

        if (idsSolicitud.isEmpty()) {
            return;
        }

        for (Integer idServicio : idsServicios) {
            if (!idsSolicitud.contains(idServicio)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La solicitud no contiene el servicio ID: " + idServicio
                );
            }
        }
    }

    private BigDecimal calcularCantidadActividad(
            Actividad actividad,
            GenerarCotizacionBaseRequest request,
            Integer idServicio
    ) {
        String formula = textoSeguro(actividad.getFormulaCode());
        BigDecimal base = obtenerValorVariable(actividad.getVariableBase(), request, idServicio);
        BigDecimal factor = valorSeguroUno(actividad.getFactor());

        switch (formula) {
            case "FIJO":
                return BigDecimal.ONE;

            case "METRO_CUADRADO_X_PRECIO":
                return primerValorPositivo(base, obtenerM2DesdeParams(actividad.getParamsJson()));

            case "METRO_LINEAL_X_PRECIO":
                return base;

            case "CANTIDAD_X_PRECIO":
                return primerValorPositivo(
                        base,
                        cantidadDesdeParams(actividad.getParamsJson()),
                        valorSeguro(actividad.getFactor())
                );

            case "AREA_PRIVADA_X_PRECIO":
                return primerValorPositivo(base, obtenerAreaPrivada(request));

            case "AREA_PRIVADA_X_FACTOR":
            case "AREA_PRIVADA_X_FACTOR_X_PRECIO":
                return primerValorPositivo(obtenerAreaPrivada(request), base).multiply(factor);

            case "METRO_CUADRADO_X_FACTOR_X_PRECIO":
            case "METRO_LINEAL_X_FACTOR_X_PRECIO":
            case "CANTIDAD_X_FACTOR_X_PRECIO":
                return base.multiply(factor);

            case "AREA_FIJA_X_PRECIO":
                return primerValorPositivo(
                        obtenerAreaTotalDesdeParams(actividad.getParamsJson()),
                        base,
                        valorSeguro(actividad.getFactor())
                );

            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calcularCantidadProducto(
            Producto producto,
            GenerarCotizacionBaseRequest request,
            Integer idServicio
    ) {
        String formula = textoSeguro(producto.getFormulaCode());
        BigDecimal base = obtenerValorVariable(producto.getVariableBase(), request, idServicio);
        BigDecimal factor = valorSeguroUno(producto.getFactor());

        if (!formula.isEmpty()) {
            switch (formula) {
                case "FIJO":
                    return BigDecimal.ONE;

                case "METRO_CUADRADO_X_PRECIO":
                case "METRO_LINEAL_X_PRECIO":
                case "CANTIDAD_X_PRECIO":
                case "AREA_PRIVADA_X_PRECIO":
                    return base;

                case "AREA_PRIVADA_X_FACTOR":
                case "AREA_PRIVADA_X_FACTOR_X_PRECIO":
                case "METRO_CUADRADO_X_FACTOR_X_PRECIO":
                case "METRO_LINEAL_X_FACTOR_X_PRECIO":
                case "CANTIDAD_X_FACTOR_X_PRECIO":
                    return base.multiply(factor);

                default:
                    return BigDecimal.ZERO;
            }
        }

        if (idServicio == SERVICIO_CARPINTERIA) {
            return obtenerCantidadProductoCarpinteria(producto, request.getCarpinteria());
        }
        if (idServicio == SERVICIO_VIDRIO) {
            return obtenerCantidadProductoVidrio(producto, request.getVidrio());
        }
        if (idServicio == SERVICIO_MEZON) {
            return obtenerCantidadProductoMezon(producto, request.getMezon());
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal calcularCantidadMaterial(ActividadMaterialV2 relacion, BigDecimal cantidadActividad) {
        String modoCantidad = textoSeguro(relacion.getModoCantidad());
        BigDecimal cantidad = valorSeguro(relacion.getCantidad());
        BigDecimal factor = valorSeguroUno(relacion.getFactor());

        switch (modoCantidad) {
            case "FIJA":
                return cantidad;

            case "POR_ACTIVIDAD":
            case "POR_UNIDAD":
                return cantidadActividad.multiply(cantidad);

            case "POR_FACTOR":
            case "POR_AREA":
                return cantidadActividad.multiply(factor);

            default:
                if (relacion.getFactor() != null) {
                    return cantidadActividad.multiply(factor);
                }
                return cantidad;
        }
    }

    private BigDecimal obtenerValorVariable(
            String variableBase,
            GenerarCotizacionBaseRequest request,
            Integer idServicio
    ) {
        String variable = textoSeguro(variableBase);

        if (variable.isEmpty()) {
            return BigDecimal.ZERO;
        }

        if ("NINGUNA".equals(variable)) {
            return BigDecimal.ONE;
        }

        switch (idServicio) {
            case SERVICIO_OBRA_BLANCA:
                return obtenerVariableManoObra(variable, request);
            case SERVICIO_CARPINTERIA:
                return obtenerVariableCarpinteria(variable, request);
            case SERVICIO_VIDRIO:
                return obtenerVariableVidrio(variable, request);
            case SERVICIO_MEZON:
                return obtenerVariableMezon(variable, request);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal obtenerVariableManoObra(String variable, GenerarCotizacionBaseRequest request) {
        switch (variable) {
            case "MEDIDA_AREA_PRIVADA":
            case "AREA_PRIVADA":
                return obtenerValorGetter(request.getManoObra(), "getMedidaAreaPrivada");
            case "METROS_CUADRADOS_MURO":
                return obtenerValorGetter(request.getManoObra(), "getMetrosCuadradosMuro");
            case "METROS_CUADRADOS_CIELO":
                return obtenerValorGetter(request.getManoObra(), "getMetrosCuadradosCielo");
            case "METROS_CUADRADOS_PANEL_YESO":
                return obtenerValorGetter(request.getManoObra(), "getMetrosCuadradosPanelYeso");
            case "METROS_CUADRADOS_TAPAR_TUBERIAS":
                return obtenerValorGetter(request.getManoObra(), "getMetrosCuadradosTaparTuberias");
            case "CANTIDAD_POYOS":
                return obtenerValorGetter(request.getManoObra(), "getCantidadPoyos");
            case "CANTIDAD_PUNTOS_ELECTRICOS":
                return obtenerValorGetter(request.getManoObra(), "getCantidadPuntosElectricos");
            case "CANTIDAD_BANOS":
                return obtenerValorGetter(request.getManoObra(), "getCantidadBanos");
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal obtenerVariableCarpinteria(String variable, GenerarCotizacionBaseRequest request) {
        switch (variable) {
            case "CANTIDAD_CLOSET":
            case "CLOSET":
                return obtenerValorGetter(request.getCarpinteria(), "getCantidadCloset");
            case "CANTIDAD_PUERTAS":
            case "PUERTAS":
                return obtenerValorGetter(request.getCarpinteria(), "getCantidadPuertas");
            case "MUEBLE_ALTO_COCINA":
            case "MEDIDA_MUEBLE_ALTO_COCINA":
                return obtenerValorGetter(request.getCarpinteria(), "getMuebleAltoCocina");
            case "MUEBLE_BAJO_COCINA":
            case "MEDIDA_MUEBLE_BAJO_COCINA":
                return obtenerValorGetter(request.getCarpinteria(), "getMuebleBajoCocina");
            case "MUEBLE_BARRA":
            case "MEDIDA_MUEBLE_BARRA":
                return obtenerValorGetter(request.getCarpinteria(), "getMuebleBarra");
            case "CANTIDAD_BANOS":
                return obtenerValorGetter(request.getCarpinteria(), "getCantidadBanos");
            case "CANTIDAD_MUEBLE_BAJO_BANO":
                return obtenerValorGetter(request.getCarpinteria(), "getCantidadMuebleBajoBano");
            case "CANTIDAD_MUEBLE_ALTO_BANO":
                return obtenerValorGetter(request.getCarpinteria(), "getCantidadMuebleAltoBano");
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal obtenerVariableVidrio(String variable, GenerarCotizacionBaseRequest request) {
        if ("CANTIDAD_BANOS".equals(variable)) {
            return obtenerValorGetter(request.getVidrio(), "getCantidadBanos");
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal obtenerVariableMezon(String variable, GenerarCotizacionBaseRequest request) {
        switch (variable) {
            case "MEDIDA_COCINA":
                return obtenerValorGetter(request.getMezon(), "getMedidaCocina");
            case "MEDIDA_BARRA":
                return obtenerValorGetter(request.getMezon(), "getMedidaBarra");
            case "MEDIDA_LAVAMANOS":
                return obtenerValorGetter(request.getMezon(), "getMedidaLavamanos");
            case "METROS_LINEALES_MESON":
                return totalMedidasMezonSeleccionadas(request.getMezon());
            default:
                return BigDecimal.ZERO;
        }
    }

    private boolean actividadAplicaParaManoObra(Actividad actividad, ManoObraBaseRequest manoObra) {
        if (actividad == null || manoObra == null) {
            return false;
        }

        String texto = textoActividad(actividad);

        if (texto.contains("drywall en cielo") || (texto.contains("drywall") && texto.contains("cielo"))) {
            return esTipoCielo(manoObra, "DRYWALL");
        }

        if (texto.contains("muro en drywall")
                || texto.contains("pared division")
                || texto.contains("division en drywall")) {
            return Boolean.TRUE.equals(manoObra.getDivisionPared());
        }

        if (texto.contains("tapar tuberias")) {
            return esTipoCielo(manoObra, "ESTUCO");
        }

        if (texto.contains("centrar luces")
                || texto.contains("punto electrico")
                || texto.contains("puntos electricos")) {
            return obtenerCantidadPuntosElectricos(actividad, manoObra).compareTo(BigDecimal.ZERO) > 0;
        }

        if (texto.contains("poyo")) {
            return obtenerCantidadPoyos(actividad, manoObra).compareTo(BigDecimal.ZERO) > 0;
        }

        if (texto.contains("bano principal")) {
            return cantidadBanos(manoObra) >= 1;
        }

        if (texto.contains("bano social")) {
            return cantidadBanos(manoObra) >= 2;
        }

        return true;
    }

    private boolean esTipoCielo(ManoObraBaseRequest manoObra, String esperado) {
        return normalizarComparacion(manoObra.getTipoCielo())
                .contains(normalizarComparacion(esperado));
    }

    private int cantidadBanos(ManoObraBaseRequest manoObra) {
        return manoObra.getCantidadBanos() == null ? 0 : manoObra.getCantidadBanos();
    }

    private BigDecimal obtenerCantidadPoyos(Actividad actividad, ManoObraBaseRequest manoObra) {
        if (manoObra.getCantidadPoyos() != null && manoObra.getCantidadPoyos() > 0) {
            return BigDecimal.valueOf(manoObra.getCantidadPoyos());
        }

        return valorSeguro(actividad.getFactor());
    }

    private BigDecimal obtenerCantidadPuntosElectricos(Actividad actividad, ManoObraBaseRequest manoObra) {
        if (manoObra.getCantidadPuntosElectricos() != null && manoObra.getCantidadPuntosElectricos() > 0) {
            return BigDecimal.valueOf(manoObra.getCantidadPuntosElectricos());
        }

        return primerValorPositivo(
                cantidadDesdeParams(actividad.getParamsJson()),
                valorSeguro(actividad.getFactor())
        );
    }

    private BigDecimal obtenerAreaPrivada(GenerarCotizacionBaseRequest request) {
        if (request == null || request.getManoObra() == null) {
            return BigDecimal.ZERO;
        }

        return convertirBigDecimal(request.getManoObra().getMedidaAreaPrivada());
    }

    private BigDecimal cantidadDesdeParams(String paramsJson) {
        return obtenerDecimalDesdeParams(
                paramsJson,
                "CANT",
                "cant",
                "cantidad",
                "Cantidad",
                "cantidadBase"
        );
    }

    private BigDecimal obtenerM2DesdeParams(String paramsJson) {
        BigDecimal m2 = obtenerDecimalDesdeParams(paramsJson, "M2", "m2");
        if (m2.compareTo(BigDecimal.ZERO) > 0) {
            return m2;
        }

        return obtenerAreaTotalDesdeParams(paramsJson);
    }

    private BigDecimal obtenerAreaTotalDesdeParams(String paramsJson) {
        BigDecimal areaTotal = obtenerDecimalDesdeParams(paramsJson, "areaTotal", "AREA_TOTAL");
        if (areaTotal.compareTo(BigDecimal.ZERO) > 0) {
            return areaTotal;
        }

        BigDecimal areaPiso = obtenerDecimalDesdeParams(paramsJson, "areaPiso", "AREA_PISO");
        BigDecimal areaPared = obtenerDecimalDesdeParams(paramsJson, "areaPared", "AREA_PARED");
        return areaPiso.add(areaPared);
    }

    private BigDecimal obtenerDecimalDesdeParams(String paramsJson, String... claves) {
        try {
            if (paramsJson == null || paramsJson.trim().isEmpty()) {
                return BigDecimal.ZERO;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(paramsJson);

            for (String clave : claves) {
                if (root.has(clave) && !root.get(clave).isNull()) {
                    return root.get(clave).decimalValue();
                }
            }

            return BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal primerValorPositivo(BigDecimal... valores) {
        for (BigDecimal valor : valores) {
            if (valor != null && valor.compareTo(BigDecimal.ZERO) > 0) {
                return valor;
            }
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal obtenerCantidadProductoCarpinteria(Producto producto, CarpinteriaBaseRequest request) {
        if (producto == null || request == null) {
            return BigDecimal.ZERO;
        }

        String texto = textoProducto(producto);

        if (texto.contains("closet")) {
            return cantidadDesdeEntero(request.getCantidadCloset());
        }
        if (texto.contains("puerta")) {
            return cantidadDesdeEntero(request.getCantidadPuertas());
        }
        if (texto.contains("mueble alto") && texto.contains("cocina")) {
            return valorSeguro(request.getMuebleAltoCocina());
        }
        if (texto.contains("mueble bajo") && texto.contains("cocina")) {
            return valorSeguro(request.getMuebleBajoCocina());
        }
        if (texto.contains("barra")) {
            return valorSeguro(request.getMuebleBarra());
        }

        boolean esBano = texto.contains("bano") || texto.contains("lavamanos");

        if (texto.contains("mueble alto") && esBano) {
            return cantidadDesdeEntero(
                    request.getCantidadMuebleAltoBano() != null
                    ? request.getCantidadMuebleAltoBano()
                    : request.getCantidadBanos()
            );
        }
        if (texto.contains("mueble bajo") && esBano) {
            return cantidadDesdeEntero(
                    request.getCantidadMuebleBajoBano() != null
                    ? request.getCantidadMuebleBajoBano()
                    : request.getCantidadBanos()
            );
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal obtenerCantidadProductoVidrio(Producto producto, VidrioBaseRequest request) {
        if (producto == null || request == null || request.getCantidadBanos() == null) {
            return BigDecimal.ZERO;
        }

        String texto = textoProducto(producto);

        if (!coincideTipoApertura(texto, request.getTipoApertura())) {
            return BigDecimal.ZERO;
        }
        if (!coincideColorAccesorios(texto, request.getColorAccesorios())) {
            return BigDecimal.ZERO;
        }
        if (!coincideNichoVidrio(texto, request.getTieneNicho())) {
            return BigDecimal.ZERO;
        }

        return cantidadDesdeEntero(request.getCantidadBanos());
    }

    private BigDecimal obtenerCantidadProductoMezon(Producto producto, MezonBaseRequest request) {
        if (producto == null || request == null) {
            return BigDecimal.ZERO;
        }

        String texto = textoProducto(producto);
        boolean esCocina = texto.contains("cocina");
        boolean esBarra = texto.contains("barra");
        boolean esLavamanos = texto.contains("lavamanos")
                || texto.contains("lavabo")
                || texto.contains("bano");

        BigDecimal cantidad = BigDecimal.ZERO;

        if (esCocina && Boolean.TRUE.equals(request.getMezonCocina())) {
            cantidad = cantidad.add(valorSeguro(request.getMedidaCocina()));
        }
        if (esBarra && Boolean.TRUE.equals(request.getMezonBarra())) {
            cantidad = cantidad.add(valorSeguro(request.getMedidaBarra()));
        }
        if (esLavamanos && Boolean.TRUE.equals(request.getMezonLavamanos())) {
            cantidad = cantidad.add(valorSeguro(request.getMedidaLavamanos()));
        }

        if (esCocina || esBarra || esLavamanos) {
            return cantidad;
        }

        return totalMedidasMezonSeleccionadas(request);
    }

    private BigDecimal totalMedidasMezonSeleccionadas(MezonBaseRequest request) {
        if (request == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal cantidad = BigDecimal.ZERO;
        if (Boolean.TRUE.equals(request.getMezonCocina())) {
            cantidad = cantidad.add(valorSeguro(request.getMedidaCocina()));
        }
        if (Boolean.TRUE.equals(request.getMezonBarra())) {
            cantidad = cantidad.add(valorSeguro(request.getMedidaBarra()));
        }
        if (Boolean.TRUE.equals(request.getMezonLavamanos())) {
            cantidad = cantidad.add(valorSeguro(request.getMedidaLavamanos()));
        }
        return cantidad;
    }

    private boolean coincideTipoApertura(String textoProducto, String tipoApertura) {
        String tipo = normalizarComparacion(tipoApertura);
        boolean seleccionCorrediza = tipo.contains("corrediz");
        boolean seleccionBatiente = tipo.contains("batiente");
        boolean productoCorrediza = textoProducto.contains("corrediz");
        boolean productoBatiente = textoProducto.contains("batiente");

        if (seleccionCorrediza) {
            return productoCorrediza || !productoBatiente;
        }
        if (seleccionBatiente) {
            return productoBatiente || !productoCorrediza;
        }
        return true;
    }

    private boolean coincideColorAccesorios(String textoProducto, String colorAccesorios) {
        String color = normalizarComparacion(colorAccesorios);
        boolean seleccionNegro = color.contains("negro");
        boolean seleccionPlateado = color.contains("plateado")
                || color.contains("plata")
                || color.contains("cromado");
        boolean productoNegro = textoProducto.contains("negro");
        boolean productoPlateado = textoProducto.contains("plateado")
                || textoProducto.contains("plata")
                || textoProducto.contains("cromado");

        if (seleccionNegro) {
            return productoNegro || !productoPlateado;
        }
        if (seleccionPlateado) {
            return productoPlateado || !productoNegro;
        }
        return true;
    }

    private boolean coincideNichoVidrio(String textoProducto, Boolean tieneNicho) {
        boolean seleccionConNicho = Boolean.TRUE.equals(tieneNicho);
        boolean productoConNicho = textoProducto.contains("con caja") || textoProducto.contains("con nicho");
        boolean productoSinNicho = textoProducto.contains("sin caja") || textoProducto.contains("sin nicho");

        if (seleccionConNicho) {
            return productoConNicho || !productoSinNicho;
        }
        return productoSinNicho || !productoConNicho;
    }

    private BigDecimal obtenerValorGetter(Object fuente, String metodoGetter) {
        try {
            if (fuente == null) {
                return BigDecimal.ZERO;
            }

            Method metodo = fuente.getClass().getMethod(metodoGetter);
            Object valor = metodo.invoke(fuente);
            return convertirBigDecimal(valor);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal convertirBigDecimal(Object valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        if (valor instanceof BigDecimal) {
            return (BigDecimal) valor;
        }
        if (valor instanceof Integer) {
            return BigDecimal.valueOf((Integer) valor);
        }
        if (valor instanceof Long) {
            return BigDecimal.valueOf((Long) valor);
        }
        if (valor instanceof Double) {
            return BigDecimal.valueOf((Double) valor);
        }
        if (valor instanceof Float) {
            return BigDecimal.valueOf((Float) valor);
        }
        return new BigDecimal(valor.toString());
    }

    private BigDecimal cantidadDesdeEntero(Integer valor) {
        if (valor == null || valor <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(valor);
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private BigDecimal valorSeguroUno(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ONE;
    }

    private String textoSeguro(String texto) {
        return texto != null ? texto.trim().toUpperCase() : "";
    }

    private String textoOriginal(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private String textoPrincipal(String preferido, String respaldo) {
        if (preferido != null && !preferido.trim().isEmpty()) {
            return preferido.trim();
        }
        return respaldo == null ? "" : respaldo.trim();
    }

    private String textoActividad(Actividad actividad) {
        return normalizarComparacion(
                textoOriginal(actividad.getCodigo()) + " "
                + textoOriginal(actividad.getCategoria()) + " "
                + textoOriginal(actividad.getNombreActividad()) + " "
                + textoOriginal(actividad.getDescripcion())
        );
    }

    private String textoProducto(Producto producto) {
        return normalizarComparacion(
                textoOriginal(producto.getCodigo()) + " "
                + textoOriginal(producto.getCategoria()) + " "
                + textoOriginal(producto.getNombreProducto()) + " "
                + textoOriginal(producto.getDescripcion())
        );
    }

    private String normalizarComparacion(String texto) {
        if (texto == null) {
            return "";
        }

        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return sinAcentos.toLowerCase().trim();
    }

    private BigDecimal redondear(BigDecimal valor) {
        if (valor == null) {
            return BigDecimal.ZERO;
        }
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
