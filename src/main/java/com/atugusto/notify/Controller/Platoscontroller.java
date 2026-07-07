package com.atugusto.notify.Controller;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Service.PlatosService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/platos")
public class Platoscontroller {
    private final PlatosService platosService;

    public Platoscontroller(PlatosService platosService) {
        this.platosService = platosService;
    }
    
    @GetMapping()
    public Flux<Platos> getPlatosDisponibles(
            @RequestParam(name = "empresaId", defaultValue = "1") Long empresaId) {
        return platosService.findPlatosDisponibles(empresaId);
    }

    @PostMapping()
    public Mono<org.springframework.http.ResponseEntity<Platos>> createPlato(@RequestBody Platos plato) {
        return platosService.savePlato(plato)
                .map(createdPlato -> org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).body(createdPlato));
    }
}
