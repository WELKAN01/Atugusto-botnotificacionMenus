package com.atugusto.notify.Repository;

import com.atugusto.notify.Entity.Empresa;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface EmpresaRepository extends ReactiveCrudRepository<Empresa, Long> {

    Flux<Empresa> findByActivoTrue();
}
