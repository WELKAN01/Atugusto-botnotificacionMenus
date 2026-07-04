package com.atugusto.notify.Service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Entity.PlatosDiarios;
import com.atugusto.notify.Repository.PlatoDiariosRepository;

@Service
public class PlatosDiariosService {
    
    private final PlatoDiariosRepository platosdiariosrepository;

    public PlatosDiariosService(PlatoDiariosRepository platoDiariosRepository){
        this.platosdiariosrepository = platoDiariosRepository;
    }

    public List<Platos> PlatosDiariosListToday(){
        return platosdiariosrepository.findPlatosHoy();
    }


    public  List<PlatosDiarios> PlatosDiariosListAll(){
        return platosdiariosrepository.findAll();
    }

    public void platosDiariosSave(PlatosDiarios pd){
        platosdiariosrepository.save(pd);
    }
}
