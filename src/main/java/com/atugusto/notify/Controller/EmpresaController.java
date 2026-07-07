package com.atugusto.notify.Controller;

import com.atugusto.notify.Entity.Empresa;
import com.atugusto.notify.Service.EmpresaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/empresas")
public class EmpresaController {
    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public Flux<Empresa> getEmpresasActivas() {
        return empresaService.findEmpresasActivas();
    }

    @PostMapping
    public Mono<ResponseEntity<Empresa>> createEmpresa(@RequestBody Empresa empresa) {
        return empresaService.saveEmpresa(empresa)
                .map(createdEmpresa -> ResponseEntity.status(HttpStatus.CREATED).body(createdEmpresa));
    }
}
