package com.atugusto.notify.Service;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Repository.PlatosRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PlatosService {
    private final PlatosRepository platosRepository;
    
    public PlatosService(PlatosRepository platosRepository) {
        this.platosRepository = platosRepository;
    }

    public Flux<Platos> findPlatosDisponibles() {
        return platosRepository.findByDisponibleTrue();
    }
        
    public Mono<Platos> savePlato(Platos plato) {
        return platosRepository.save(plato);
    }

    public Mono<Long> getPlatosCantidad() {
        return platosRepository.count();
    }

    public Mono<Platos> findIDPlatos(Long id) {
        return platosRepository.findById(id);
    }
}



