package com.atugusto.notify.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Repository.PlatosRepository;

@Service
public class PlatosService{
    private final PlatosRepository platosRepository;
    
    public PlatosService(PlatosRepository platosRepository) {
        this.platosRepository = platosRepository;
    }

    public List<Platos> findPlatosDisponibles() {
        // Obtiene los platos disponibles desde la base de datos
        return platosRepository.findAll().stream()
                .filter(Platos::isDisponible)
                .toList();
    }
        
    public Platos savePlato(Platos plato) {
        return platosRepository.save(plato);
    }

    public String getPlatosCantidad(){
        return String.valueOf(platosRepository.count());
    }

    public Platos findIDPlatos(Long ID){
        return platosRepository.findById(ID).orElse(null);
    }

}



