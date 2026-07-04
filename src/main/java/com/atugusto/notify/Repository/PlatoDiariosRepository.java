package com.atugusto.notify.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Entity.PlatosDiarios;

public interface PlatoDiariosRepository extends JpaRepository<PlatosDiarios, Long>{

    @Query("SELECT pd.platos FROM PlatosDiarios pd WHERE pd.fec_menu_pedido = CURRENT_DATE")

    List<Platos> findPlatosHoy();
        
}
