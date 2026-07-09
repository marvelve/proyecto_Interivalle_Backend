package com.interivalle.DTO;

import java.util.List;

public class EliminarActividadesCotizacionRequest {

    private List<Integer> idsDetalleActividad;

    public List<Integer> getIdsDetalleActividad() {
        return idsDetalleActividad;
    }

    public void setIdsDetalleActividad(List<Integer> idsDetalleActividad) {
        this.idsDetalleActividad = idsDetalleActividad;
    }
}
