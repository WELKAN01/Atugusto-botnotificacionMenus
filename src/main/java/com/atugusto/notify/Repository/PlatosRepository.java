package com.atugusto.notify.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.atugusto.notify.Entity.Platos;

@Repository
public interface PlatosRepository extends JpaRepository<Platos, Long>{

}
