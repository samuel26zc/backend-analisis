package com.analisis.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareDijkstraRequestDto {

    @NotBlank(message = "El identificador del lugar de origen es obligatorio")
    private String originPlaceId;

    @NotBlank(message = "El identificador del lugar de destino es obligatorio")
    private String destinationPlaceId;
}
