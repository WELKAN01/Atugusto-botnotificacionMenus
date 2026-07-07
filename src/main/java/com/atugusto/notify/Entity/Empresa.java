package com.atugusto.notify.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("empresa")
public class Empresa {

    @Id
    private Long id;
    private String nombre;
    private String razonSocial;
    private String ruc;
    private String moneda;
    private String timezone;
    private boolean activo;

    public Empresa() {
    }

    public Empresa(Long id, String nombre, String razonSocial, String ruc, String moneda, String timezone,
            boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.razonSocial = razonSocial;
        this.ruc = ruc;
        this.moneda = moneda;
        this.timezone = timezone;
        this.activo = activo;
    }

    public static Empresa demo() {
        return new Empresa(1L, "Empresa Demo", "Empresa Demo", null, "PEN", "America/Lima", true);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
        this.razonSocial = razonSocial;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getMoneda() {
        return moneda;
    }

    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
