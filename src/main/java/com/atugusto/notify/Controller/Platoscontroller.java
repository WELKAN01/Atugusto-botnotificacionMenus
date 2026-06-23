package com.atugusto.notify.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Service.PlatosService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/platos")
public class Platoscontroller {
    private final PlatosService platosService;

    public Platoscontroller(PlatosService platosService) {
        this.platosService = platosService;
    }
    
    @GetMapping()
    public ResponseEntity<List<Platos>> getPlatosDisponibles() {
        return new ResponseEntity<>(platosService.findPlatosDisponibles(), HttpStatus.ACCEPTED);
    }

    @PostMapping()
    public ResponseEntity<Platos> createPlato(@RequestBody Platos plato) {
        Platos createdPlato = platosService.savePlato(plato);
        return new ResponseEntity<>(createdPlato, HttpStatus.CREATED);
    }


    // @GetMapping("ai/cantidad")
    // public String getPlatosCantidad(@RequestBody Map<String, String> request) {
    //     return iaService.ask(request.get("message"));
    // }
    
}
