package com.atugusto.notify.Repository;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Entity.PlatosDiarios;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PlatoDiariosRepository extends ReactiveCrudRepository<PlatosDiarios, Long> {

    @Query("""
            SELECT p.*
            FROM platos_diarios pd
            JOIN platos p ON p.id = pd.plato_id
            WHERE pd.fec_menu_pedido = :fecha
              AND pd.empresa_id = :empresaId
              AND pd.disponible = true
            """)
    Flux<Platos> findPlatosHoy(@Param("empresaId") Long empresaId, @Param("fecha") LocalDate fecha);

    Mono<Boolean> existsByFecMenuPedido(LocalDate fecMenuPedido);

    Mono<Boolean> existsByEmpresaIdAndFecMenuPedido(Long empresaId, LocalDate fecMenuPedido);
}
