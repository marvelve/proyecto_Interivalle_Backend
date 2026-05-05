package com.interivalle.Servicio;

import com.interivalle.DTO.ProductoRequest;
import com.interivalle.DTO.ProductoResponse;
import com.interivalle.Modelo.CatalogoItem;
import com.interivalle.Modelo.Producto;
import com.interivalle.Modelo.Servicios;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.CatalogoItemRepositorio;
import com.interivalle.Repositorio.ProductoRepositorio;
import com.interivalle.Repositorio.ServiciosRepositorio;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepositorio productoRepo;

    @Autowired
    private CatalogoItemRepositorio catalogoItemRepo;

    @Autowired
    private ServiciosRepositorio serviciosRepo;

    public List<ProductoResponse> listar(Integer idServicio, String categoria, Boolean activo, String texto) {
        return productoRepo.filtrar(idServicio, limpiar(categoria), activo, limpiar(texto))
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductoResponse obtenerPorId(Integer id) {
        return toResponse(buscarProducto(id));
    }

    public ProductoResponse crear(ProductoRequest req) {
        validarCrear(req);

        Producto producto = new Producto();
        aplicarDatos(producto, req, true);

        return toResponse(productoRepo.save(producto));
    }

    public ProductoResponse actualizar(Integer id, ProductoRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar los datos del producto");
        }

        Producto producto = buscarProducto(id);
        aplicarDatos(producto, req, false);

        return toResponse(productoRepo.save(producto));
    }

    public ProductoResponse cambiarEstado(Integer id, Boolean activo) {
        Producto producto = buscarProducto(id);
        producto.setActivo(Boolean.TRUE.equals(activo));

        return toResponse(productoRepo.save(producto));
    }

    public Map<String, Integer> migrarDesdeCatalogoItem() {
        List<CatalogoItem> productosCatalogo =
                catalogoItemRepo.findByTipoItemAndActivoTrue(TipoItemCotizacion.PRODUCTO);

        int creados = 0;
        int existentes = 0;

        for (CatalogoItem item : productosCatalogo) {
            if (productoRepo.findByIdCatalogoItemOrigen(item.getIdCatalogoItem()).isPresent()) {
                existentes++;
                continue;
            }

            Producto producto = new Producto();
            producto.setIdCatalogoItemOrigen(item.getIdCatalogoItem());
            producto.setServicio(item.getServicio());
            producto.setCodigo(limpiar(item.getCodigo()));
            producto.setNombreProducto(item.getNombreItem());
            producto.setDescripcion(item.getDescripcion());
            producto.setCategoria(item.getCategoria());
            producto.setUnidad(item.getUnidad());
            producto.setModoPrecio(item.getModoPrecio());
            producto.setPrecioUnitarioVenta(item.getPrecioUnitarioVenta());
            producto.setPrecioUnitarioProveedor(item.getPrecioUnitarioProveedor());
            producto.setSemana(item.getSemana());
            producto.setActivo(item.getActivo() == null ? true : item.getActivo());
            producto.setCreadoPor(item.getCreadoPor());

            productoRepo.save(producto);
            creados++;
        }

        return Map.of(
                "origen", productosCatalogo.size(),
                "creados", creados,
                "existentes", existentes
        );
    }

    private void validarCrear(ProductoRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar los datos del producto");
        }

        if (req.getIdServicio() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar el servicio del producto");
        }

        if (limpiar(req.getNombreProducto()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del producto es obligatorio");
        }
    }

    private Producto buscarProducto(Integer id) {
        return productoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado"
                ));
    }

    private void aplicarDatos(Producto producto, ProductoRequest req, boolean crear) {
        if (crear || req.getIdServicio() != null) {
            producto.setServicio(buscarServicio(req.getIdServicio()));
        }

        if (crear || req.getCodigo() != null) producto.setCodigo(limpiar(req.getCodigo()));
        if (crear || req.getNombreProducto() != null) {
            String nombre = limpiar(req.getNombreProducto());
            if (nombre == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del producto es obligatorio");
            }
            producto.setNombreProducto(nombre);
        }
        if (crear || req.getDescripcion() != null) producto.setDescripcion(limpiar(req.getDescripcion()));
        if (crear || req.getCategoria() != null) producto.setCategoria(limpiar(req.getCategoria()));
        if (crear || req.getUnidad() != null) producto.setUnidad(limpiar(req.getUnidad()));
        if (crear || req.getModoPrecio() != null) producto.setModoPrecio(limpiar(req.getModoPrecio()));
        if (crear || req.getPrecioUnitarioVenta() != null) producto.setPrecioUnitarioVenta(req.getPrecioUnitarioVenta());
        if (crear || req.getPrecioUnitarioProveedor() != null) producto.setPrecioUnitarioProveedor(req.getPrecioUnitarioProveedor());
        if (crear || req.getSemana() != null) producto.setSemana(req.getSemana());
        if (crear || req.getActivo() != null) producto.setActivo(req.getActivo() == null ? true : req.getActivo());
        if (crear || req.getCreadoPor() != null) producto.setCreadoPor(req.getCreadoPor());
    }

    private Servicios buscarServicio(Integer idServicio) {
        return serviciosRepo.findById(idServicio)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Servicio no encontrado"
                ));
    }

    private ProductoResponse toResponse(Producto producto) {
        ProductoResponse r = new ProductoResponse();
        r.setIdProducto(producto.getIdProducto());
        r.setIdCatalogoItemOrigen(producto.getIdCatalogoItemOrigen());
        r.setCodigo(producto.getCodigo());
        r.setNombreProducto(producto.getNombreProducto());
        r.setDescripcion(producto.getDescripcion());
        r.setCategoria(producto.getCategoria());
        r.setUnidad(producto.getUnidad());
        r.setModoPrecio(producto.getModoPrecio());
        r.setPrecioUnitarioVenta(producto.getPrecioUnitarioVenta());
        r.setPrecioUnitarioProveedor(producto.getPrecioUnitarioProveedor());
        r.setSemana(producto.getSemana());
        r.setActivo(producto.getActivo());
        r.setCreadoPor(producto.getCreadoPor());
        r.setFechaCreacion(producto.getFechaCreacion());
        r.setFechaActualizacion(producto.getFechaActualizacion());

        if (producto.getServicio() != null) {
            r.setIdServicio(producto.getServicio().getIdServicio());
            r.setNombreServicio(producto.getServicio().getNombreServicio());
        }

        return r;
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
