package com.interivalle.DTO;

public class CotizacionBaseResponse {

    private Integer solicitudId;
    private Integer idCotizacion;
    private String mensaje;

    // Indica que seccion del formulario fue procesada por el backend.
    private boolean manoObraProcesada;
    private boolean carpinteriaProcesada;
    private boolean vidrioProcesado;
    private boolean mezonProcesado;

    public Integer getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(Integer solicitudId) {
        this.solicitudId = solicitudId;
    }

    public Integer getIdCotizacion() {
        return idCotizacion;
    }

    public void setIdCotizacion(Integer idCotizacion) {
        this.idCotizacion = idCotizacion;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public boolean isManoObraProcesada() {
        return manoObraProcesada;
    }

    public void setManoObraProcesada(boolean manoObraProcesada) {
        this.manoObraProcesada = manoObraProcesada;
    }

    public boolean isCarpinteriaProcesada() {
        return carpinteriaProcesada;
    }

    public void setCarpinteriaProcesada(boolean carpinteriaProcesada) {
        this.carpinteriaProcesada = carpinteriaProcesada;
    }

    public boolean isVidrioProcesado() {
        return vidrioProcesado;
    }

    public void setVidrioProcesado(boolean vidrioProcesado) {
        this.vidrioProcesado = vidrioProcesado;
    }

    public boolean isMezonProcesado() {
        return mezonProcesado;
    }

    public void setMezonProcesado(boolean mezonProcesado) {
        this.mezonProcesado = mezonProcesado;
    }
}
