package com.atugusto.notify.Controller.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.atugusto.notify.Service.PlatosService;

@Component
public class PlatosToolsMCP {
    private final PlatosService platosService;

    PlatosToolsMCP(PlatosService platosService) {
        this.platosService = platosService;
    }


    @Tool(description = "cuanto es la cantidad de platos disponibles")
    public String getPlatosCantidad(){
        return platosService.getPlatosCantidad();
    }
}
