package com.atugusto.notify.Service;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Entity.PlatosDiarios;
import com.atugusto.notify.Repository.PlatoDiariosRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class PlatosDiariosService {
    public static final long DEFAULT_EMPRESA_ID = 1L;
    
    private final PlatoDiariosRepository platosdiariosrepository;

    public PlatosDiariosService(PlatoDiariosRepository platoDiariosRepository){
        this.platosdiariosrepository = platoDiariosRepository;
    }

    public Flux<Platos> platosDiariosListToday() {
        return platosDiariosListToday(DEFAULT_EMPRESA_ID);
    }

    public Flux<Platos> platosDiariosListToday(Long empresaId) {
        return platosdiariosrepository.findPlatosHoy(empresaId, LocalDate.now());
    }

    public Flux<PlatosDiarios> platosDiariosListAll() {
        return platosdiariosrepository.findAll();
    }

    public Mono<PlatosDiarios> platosDiariosSave(PlatosDiarios pd) {
        if (pd.getEmpresaId() == null) {
            pd.setEmpresaId(DEFAULT_EMPRESA_ID);
        }
        return platosdiariosrepository.save(pd);
    }

    public Mono<Boolean> existsMenuForDate(LocalDate date) {
        return existsMenuForDate(DEFAULT_EMPRESA_ID, date);
    }

    public Mono<Boolean> existsMenuForDate(Long empresaId, LocalDate date) {
        return platosdiariosrepository.existsByEmpresaIdAndFecMenuPedido(empresaId, date);
    }
}
