package com.atugusto.notify.Repository;

import com.atugusto.notify.Entity.Platos;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PlatosRepository extends ReactiveCrudRepository<Platos, Long> {

    Flux<Platos> findByDisponibleTrue();

    Flux<Platos> findByEmpresaIdAndDisponibleTrue(Long empresaId);

    Flux<Platos> findByEmpresaId(Long empresaId);

    reactor.core.publisher.Mono<Long> countByEmpresaId(Long empresaId);

    reactor.core.publisher.Mono<Platos> findByIdAndEmpresaId(Long id, Long empresaId);
}
