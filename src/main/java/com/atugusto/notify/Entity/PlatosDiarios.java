package com.atugusto.notify.Entity;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("platos_diarios")
@Setter
@Getter
@NoArgsConstructor
public class PlatosDiarios {
    @Id
    private Long id;

    @Column("empresa_id")
    private Long empresaId;
    
    @Column("fec_menu_pedido")
    private LocalDate fecMenuPedido;
    private boolean disponible;

    @Column("plato_id")
    private Long platoId;
}
