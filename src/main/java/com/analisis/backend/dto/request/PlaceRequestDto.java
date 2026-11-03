package com.analisis.backend.dto.request;

import com.analisis.backend.model.enums.PlaceCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceRequestDto {

    @NotBlank(message = "El id es obligatorio")
    private String id;

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El municipio es obligatorio")
    private String municipality;

    @NotNull(message = "La categoría es obligatoria")
    private PlaceCategory category;

    @NotNull(message = "La latitud es obligatoria")
    private Double latitude;

    @NotNull(message = "La longitud es obligatoria")
    private Double longitude;

    private int visitDurationMinutes;
}
