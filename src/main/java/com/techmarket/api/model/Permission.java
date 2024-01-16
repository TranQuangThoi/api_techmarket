package com.techmarket.api.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Entity
@Table(name = "db_permission")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class Permission extends Auditable<String> {
    @Column(name = "name", unique =  true)
    private String name;
    @Column(name = "action")
    private String action;
    @Column(name = "show_menu")
    private Boolean showMenu;
    private String description;
    @Column(name = "name_group")
    private String nameGroup;
    @Column(name = "p_code")
    private String pCode;
}
