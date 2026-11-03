package com.analisis.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayItineraryRequestDto {

    @NotBlank(message = "El lugar de origen es obligatorio")
    private String originPlaceId;

    @NotEmpty(message = "La lista de atractivos turísticos no puede estar vacía")
    private List<String> attractionIds;
}
