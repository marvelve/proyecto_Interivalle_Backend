package com.interivalle.DTO;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class MezonBaseRequest {

    @NotNull(message = "Debe indicar si desea mesón de cocina")
    private Boolean mezonCocina;

    @NotNull(message = "Debe indicar si desea mesón barra")
    private Boolean mezonBarra;

    @NotNull(message = "Debe indicar si desea mesón lavamanos")
    private Boolean mezonLavamanos;

    private BigDecimal medidaCocina;
    private BigDecimal medidaBarra;
    private BigDecimal medidaLavamanos;

    public Boolean getMezonCocina() {
        return mezonCocina;
    }

    public void setMezonCocina(Boolean mezonCocina) {
        this.mezonCocina = mezonCocina;
    }

    public Boolean getMezonBarra() {
        return mezonBarra;
    }

    public void setMezonBarra(Boolean mezonBarra) {
        this.mezonBarra = mezonBarra;
    }

    public Boolean getMezonLavamanos() {
        return mezonLavamanos;
    }

    public void setMezonLavamanos(Boolean mezonLavamanos) {
        this.mezonLavamanos = mezonLavamanos;
    }

    public BigDecimal getMedidaCocina() {
        return medidaCocina;
    }

    public void setMedidaCocina(BigDecimal medidaCocina) {
        this.medidaCocina = medidaCocina;
    }

    public BigDecimal getMedidaBarra() {
        return medidaBarra;
    }

    public void setMedidaBarra(BigDecimal medidaBarra) {
        this.medidaBarra = medidaBarra;
    }

    public BigDecimal getMedidaLavamanos() {
        return medidaLavamanos;
    }

    public void setMedidaLavamanos(BigDecimal medidaLavamanos) {
        this.medidaLavamanos = medidaLavamanos;
    }
}
