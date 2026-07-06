package com.atugusto.notify.config;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Entity.PlatosDiarios;
import com.atugusto.notify.Repository.PlatoDiariosRepository;
import com.atugusto.notify.Repository.PlatosRepository;
import java.time.LocalDate;
import java.util.Locale;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class Datafaker implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(Datafaker.class);

    private final PlatosRepository platosRepository;
    private final PlatoDiariosRepository platoDiariosRepository;

    public Datafaker(PlatosRepository platosRepository, PlatoDiariosRepository platoDiariosRepository) {
        this.platosRepository = platosRepository;
        this.platoDiariosRepository = platoDiariosRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Faker faker = new Faker(new Locale("es"));
        LocalDate today = LocalDate.now();

        seedPlatosIfNeeded(faker)
                .then(seedMenuTodayIfNeeded(today))
                .doOnSuccess(unused -> logger.info("Carga inicial reactiva completada"))
                .doOnError(error -> logger.error("Error durante la carga inicial reactiva", error))
                .block();
    }

    private Mono<Void> seedPlatosIfNeeded(Faker faker) {
        return platosRepository.count()
                .flatMap(count -> {
                    if (count > 0) {
                        logger.info("Ya existen {} platos registrados", count);
                        return Mono.empty();
                    }

                    logger.info("No existen platos, generando registros iniciales");
                    return Flux.range(0, 10)
                            .map(index -> buildRandomPlato(faker))
                            .flatMap(platosRepository::save)
                            .then();
                });
    }

    private Mono<Void> seedMenuTodayIfNeeded(LocalDate today) {
        return platoDiariosRepository.existsByFecMenuPedido(today)
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        logger.info("Ya existen platos diarios para {}", today);
                        return Mono.empty();
                    }

                    logger.info("No hay platos diarios para {}, generando menu", today);
                    return platosRepository.findAll()
                            .map(plato -> buildPlatoDiario(today, plato.getId()))
                            .flatMap(platoDiariosRepository::save)
                            .then();
                });
    }

    private Platos buildRandomPlato(Faker faker) {
        String nombre = faker.food().dish();
        String descripcion = faker.lorem().sentence();
        double precio = faker.number().randomDouble(2, 5, 50);
        String categoria = faker.options().option("ENTRANTE", "PRINCIPAL");
        boolean disponible = faker.bool().bool();

        Platos plato = new Platos();
        plato.setNombre(nombre);
        plato.setDescripcion(descripcion);
        plato.setPrecio(precio);
        plato.setCategoria(Platos.Categoria.valueOf(categoria));
        plato.setDisponible(disponible);
        return plato;
    }

    private PlatosDiarios buildPlatoDiario(LocalDate today, Long platoId) {
        PlatosDiarios platoDiario = new PlatosDiarios();
        platoDiario.setDisponible(true);
        platoDiario.setFecMenuPedido(today);
        platoDiario.setPlatoId(platoId);
        return platoDiario;
    }
}
