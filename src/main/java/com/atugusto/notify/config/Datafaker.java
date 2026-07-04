package com.atugusto.notify.config;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Component;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Entity.PlatosDiarios;
import com.atugusto.notify.Repository.PlatoDiariosRepository;
import com.atugusto.notify.Repository.PlatosRepository;

import net.datafaker.Faker;

@Component
public class Datafaker implements ApplicationRunner{

    private final PlatosRepository platosRepository;
    private final PlatoDiariosRepository platoDiariosRepository;

    public Datafaker(PlatosRepository platosRepository, PlatoDiariosRepository platoDiariosRepository) {
        this.platosRepository = platosRepository;
        this.platoDiariosRepository = platoDiariosRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Faker faker = new Faker(new Locale("es"));

        if(platosRepository.count() == 0){
            for (int i = 0; i < 10; i++) {
                String nombre = faker.food().dish();
                String descripcion = faker.lorem().sentence();
                double precio = faker.number().randomDouble(2, 5, 50);
                String categoria = faker.options().option("ENTRANTE", "PRINCIPAL");
                boolean disponible = faker.bool().bool();
                Platos platos = new Platos();
                platos.setNombre(nombre);
                platos.setDescripcion(descripcion);
                platos.setPrecio(precio);
                platos.setCategoria(Platos.Categoria.valueOf(categoria));
                platos.setDisponible(disponible);
                platosRepository.save(platos);
            }            
        }

        List<PlatosDiarios> platosDiarios = platoDiariosRepository.findAll();
        List<PlatosDiarios> platosDiariosHoy = platosDiarios.stream()
                .filter(platoDiario -> platoDiario.getFec_menu_pedido().equals(LocalDate.now()))
                .collect(Collectors.toList());
        System.out.println("Platos diarios para hoy: " + platosDiariosHoy.stream().count());
        System.out.println("Platos diarios disponibles hoy: " + platosDiarios.isEmpty());
        Optional<List<PlatosDiarios>> platoDiarioHoy = Optional.ofNullable(platosDiariosHoy);

        
        if(platoDiarioHoy.stream().count() == 0){
            LocalDate date = LocalDate.now();
            List<Platos> platos = platosRepository.findAll();

            for (Platos plato : platos) {
                PlatosDiarios platosd = new PlatosDiarios();
                platosd.setDisponible(true);
                platosd.setFec_menu_pedido(date);
                platosd.setPlatos(plato);
                platoDiariosRepository.save(platosd);
            }  
        }
    }

}
