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
    
    private final PlatoDiariosRepository platosdiariosrepository;

    public PlatosDiariosService(PlatoDiariosRepository platoDiariosRepository){
        this.platosdiariosrepository = platoDiariosRepository;
    }

    public Flux<Platos> platosDiariosListToday() {
        return platosdiariosrepository.findPlatosHoy(LocalDate.now());
    }

    public Flux<PlatosDiarios> platosDiariosListAll() {
        return platosdiariosrepository.findAll();
    }

    public Mono<PlatosDiarios> platosDiariosSave(PlatosDiarios pd) {
        return platosdiariosrepository.save(pd);
    }

    public Mono<Boolean> existsMenuForDate(LocalDate date) {
        return platosdiariosrepository.existsByFecMenuPedido(date);
    }
}
