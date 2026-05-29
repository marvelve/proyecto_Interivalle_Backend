package com.interivalle.DTO;

import java.time.LocalDate;
import java.time.LocalTime;

public class ReprogramarVisitaRequest {

    // Datos permitidos para cambiar la agenda de una visita tecnica.
    private LocalDate fechaVisita;
    private LocalTime horaVisita;

    public LocalDate getFechaVisita() {
        return fechaVisita;
    }

    public void setFechaVisita(LocalDate fechaVisita) {
        this.fechaVisita = fechaVisita;
    }

    public LocalTime getHoraVisita() {
        return horaVisita;
    }

    public void setHoraVisita(LocalTime horaVisita) {
        this.horaVisita = horaVisita;
    }
}
