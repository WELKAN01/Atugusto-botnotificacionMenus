package com.atugusto.notify.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Repository.PlatosRepository;

import net.datafaker.Faker;

@Component
public class Datafaker implements ApplicationRunner{

    @Autowired
    private PlatosRepository platosRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if(platosRepository.count() == 0){
            Faker faker = new Faker(new Locale("es"));
            for (int i = 0; i < 10; i++) {
                String nombre = faker.food().dish();
                String descripcion = faker.lorem().sentence();
                double precio = faker.number().randomDouble(2, 5, 50);
                String categoria = faker.options().option("ENTRANTE", "PRINCIPAL", "POSTRE");
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
    }

}
