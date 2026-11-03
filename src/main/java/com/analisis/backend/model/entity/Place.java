package com.analisis.backend.model.entity;

import com.analisis.backend.model.enums.PlaceCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidad JPA que representa un lugar representativo del catálogo del Quindío.
 */
@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place extends BaseEntity {

    @Id
    @Column(nullable = false, length = 50)
    private String id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String municipality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlaceCategory category;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "utm_x", nullable = false)
    private long utmX;

    @Column(name = "utm_y", nullable = false)
    private long utmY;

    @Column(name = "visit_duration_minutes", nullable = false)
    private int visitDurationMinutes;

    @Column(name = "nearest_node_id")
    private Long nearestNodeId;

    @Column(name = "distance_to_node_meters")
    private Double distanceToNodeMeters;

    @Column(name = "is_exceeding_500m")
    private Boolean isExceeding500m;

    @Column(name = "treatment_note", length = 500)
    private String treatmentNote;
}
