package com.interivalle.DTO;

import java.util.List;
/**
 *
 * @author mary_
 */


public class CotizacionBaseFormularioResponse {

    private Integer idCotizacion;
    private Integer solicitudId;
    private String nombreProyecto;
    private String estado;
    private List<SolicitudServicioItem> serviciosSeleccionados;
    private ManoObraBaseRequest manoObra;
    private CarpinteriaBaseRequest carpinteria;
    private VidrioBaseRequest vidrio;
    private MezonBaseRequest mezon;

    public Integer getIdCotizacion() {
        return idCotizacion;
    }

    public void setIdCotizacion(Integer idCotizacion) {
        this.idCotizacion = idCotizacion;
    }

    public Integer getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(Integer solicitudId) {
        this.solicitudId = solicitudId;
    }

    public String getNombreProyecto() {
        return nombreProyecto;
    }

    public void setNombreProyecto(String nombreProyecto) {
        this.nombreProyecto = nombreProyecto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<SolicitudServicioItem> getServiciosSeleccionados() {
        return serviciosSeleccionados;
    }

    public void setServiciosSeleccionados(List<SolicitudServicioItem> serviciosSeleccionados) {
        this.serviciosSeleccionados = serviciosSeleccionados;
    }

    public ManoObraBaseRequest getManoObra() {
        return manoObra;
    }

    public void setManoObra(ManoObraBaseRequest manoObra) {
        this.manoObra = manoObra;
    }

    public CarpinteriaBaseRequest getCarpinteria() {
        return carpinteria;
    }

    public void setCarpinteria(CarpinteriaBaseRequest carpinteria) {
        this.carpinteria = carpinteria;
    }

    public VidrioBaseRequest getVidrio() {
        return vidrio;
    }

    public void setVidrio(VidrioBaseRequest vidrio) {
        this.vidrio = vidrio;
    }

    public MezonBaseRequest getMezon() {
        return mezon;
    }

    public void setMezon(MezonBaseRequest mezon) {
        this.mezon = mezon;
    }
}
