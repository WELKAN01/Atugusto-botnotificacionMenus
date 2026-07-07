package com.atugusto.notify.Service;

import com.atugusto.notify.Entity.Empresa;
import com.atugusto.notify.Repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class EmpresaService {
    public static final long DEFAULT_EMPRESA_ID = 1L;

    private final EmpresaRepository empresaRepository;

    public EmpresaService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public Flux<Empresa> findEmpresasActivas() {
        return empresaRepository.findByActivoTrue();
    }

    public Mono<Empresa> saveEmpresa(Empresa empresa) {
        return empresaRepository.save(empresa);
    }

    public Mono<Empresa> getOrCreateDefaultEmpresa() {
        return empresaRepository.findById(DEFAULT_EMPRESA_ID)
                .switchIfEmpty(empresaRepository.save(Empresa.demo()));
    }
}
