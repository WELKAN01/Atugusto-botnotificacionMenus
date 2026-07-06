package com.atugusto.notify.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("platos")
@Builder
@AllArgsConstructor
public class Platos {

    @Id
    private Long id;
    private String nombre;

    private Categoria categoria;

    private String descripcion;
    private double precio;
    private boolean disponible;
    
    public enum Categoria {
        ENTRANTE,
        PRINCIPAL
    }

    public Platos() {
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

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    @Override
    public String toString() {
        return "Platos [id=" + id + ", nombre=" + nombre + ", categoria=" + categoria + ", descripcion=" + descripcion
                + ", precio=" + precio + ", disponible=" + disponible + "]";
    }


}
