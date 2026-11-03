package com.analisis.backend.dto.request;

import com.analisis.backend.dto.response.CoordinateDto;
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
public class ConvexHullComputeRequestDto {

    @NotEmpty(message = "La lista de coordenadas no puede estar vacía")
    private List<CoordinateDto> coordinates;
}
