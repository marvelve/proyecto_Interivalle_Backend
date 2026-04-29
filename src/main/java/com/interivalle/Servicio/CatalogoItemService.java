/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.interivalle.Servicio;

import com.interivalle.DTO.ActualizarPrecioCatalogoRequest;
import com.interivalle.DTO.CatalogoItemResponse;
import com.interivalle.DTO.CrearCatalogoItemRequest;
import com.interivalle.Modelo.CatalogoItem;
import com.interivalle.Modelo.Servicios;
import com.interivalle.Modelo.enums.TipoItemCotizacion;
import com.interivalle.Repositorio.CatalogoItemRepositorio;
import com.interivalle.Repositorio.ServiciosRepositorio;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
/**
 *
 * @author mary_
 */

@Service
public class CatalogoItemService {

    @Autowired
    private CatalogoItemRepositorio repo;
    @Autowired
    private ServiciosRepositorio serviciosrepo;

    public List<CatalogoItemResponse> listar() {
        return repo.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CatalogoItemResponse obtenerPorId(Integer id) {
        CatalogoItem item = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ítem de catálogo no encontrado"
                ));

        return toResponse(item);
    }

    public CatalogoItemResponse actualizarPrecio(Integer id, ActualizarPrecioCatalogoRequest dto) {
        CatalogoItem item = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ítem de catálogo no encontrado"
                ));

        if (dto.getPrecioUnitarioVenta() != null) {
            item.setPrecioUnitarioVenta(dto.getPrecioUnitarioVenta());
        }

        if (dto.getPrecioUnitarioProveedor() != null) {
            item.setPrecioUnitarioProveedor(dto.getPrecioUnitarioProveedor());
        }

        if (dto.getActivo() != null) {
            item.setActivo(dto.getActivo());
        }

        CatalogoItem actualizado = repo.save(item);

        return toResponse(actualizado);
    }

    private CatalogoItemResponse toResponse(CatalogoItem item) {
        CatalogoItemResponse r = new CatalogoItemResponse();

        r.setIdCatalogoItem(item.getIdCatalogoItem());
        r.setNombreItem(item.getNombreItem());
        r.setCategoria(item.getCategoria());

        if (item.getTipoItem() != null) {
            r.setTipoItem(item.getTipoItem().toString());
        }

        if (item.getServicio() != null) {
            r.setNombreServicio(item.getServicio().getNombreServicio());
        }

        r.setPrecioUnitarioVenta(item.getPrecioUnitarioVenta());
        r.setPrecioUnitarioProveedor(item.getPrecioUnitarioProveedor());
        r.setActivo(item.getActivo());

        return r;
    }
    
    public CatalogoItemResponse crear(CrearCatalogoItemRequest dto) {

        CatalogoItem item = new CatalogoItem();

        item.setNombreItem(dto.getNombreItem());
        item.setCategoria(dto.getCategoria());
        item.setTipoItem(TipoItemCotizacion.valueOf(dto.getTipoItem()));

        Servicios servicios = serviciosrepo.findById(dto.getIdServicio())
            .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        item.setServicio(servicios);

        item.setPrecioUnitarioVenta(dto.getPrecioUnitarioVenta());
        item.setPrecioUnitarioProveedor(dto.getPrecioUnitarioProveedor());
        item.setActivo(dto.getActivo() != null ? dto.getActivo() : true);

        return toResponse(repo.save(item));
    }
}
