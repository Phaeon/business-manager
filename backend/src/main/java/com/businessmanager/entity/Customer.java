package com.businessmanager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@Table(name = "customers")
@NoArgsConstructor
@SuperBuilder
public class Customer extends BaseEntity {

    @Column(nullable = false)
    private String name;

    private String company;

    @Column(nullable = false)
    private String email;

    private String phone;

    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private boolean archived = false;
}
