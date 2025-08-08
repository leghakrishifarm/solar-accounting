package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partners")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Partner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String mobile;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "share_percent", nullable = false)
    private Double sharePercent;

    private String itrStatus;
    private String gstStatus;

    private Double totalIncomeReceived;  // Optional aggregate fields
    private Double totalGovernmentDeductions;

    @OneToOne(fetch = FetchType.LAZY)  // Change here: LAZY fetch
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;
}
