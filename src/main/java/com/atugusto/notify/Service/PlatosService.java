package com.atugusto.notify.Service;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Repository.PlatosRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PlatosService {
    public static final long DEFAULT_EMPRESA_ID = 1L;

    private final PlatosRepository platosRepository;
    
    public PlatosService(PlatosRepository platosRepository) {
        this.platosRepository = platosRepository;
    }

    public Flux<Platos> findPlatosDisponibles() {
        return findPlatosDisponibles(DEFAULT_EMPRESA_ID);
    }

    public Flux<Platos> findPlatosDisponibles(Long empresaId) {
        return platosRepository.findByEmpresaIdAndDisponibleTrue(empresaId);
    }
        
    public Mono<Platos> savePlato(Platos plato) {
        if (plato.getEmpresaId() == null) {
            plato.setEmpresaId(DEFAULT_EMPRESA_ID);
        }
        return platosRepository.save(plato);
    }

    public Mono<Long> getPlatosCantidad() {
        return getPlatosCantidad(DEFAULT_EMPRESA_ID);
    }

    public Mono<Long> getPlatosCantidad(Long empresaId) {
        return platosRepository.countByEmpresaId(empresaId);
    }

    public Mono<Platos> findIDPlatos(Long id) {
        return findIDPlatos(DEFAULT_EMPRESA_ID, id);
    }

    public Mono<Platos> findIDPlatos(Long empresaId, Long id) {
        return platosRepository.findByIdAndEmpresaId(id, empresaId);
    }
}



