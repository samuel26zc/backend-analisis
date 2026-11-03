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
public class CompareTspRequestDto {

    @NotBlank(message = "El lugar de origen es obligatorio")
    private String originPlaceId;

    @NotEmpty(message = "La lista de lugares a visitar no puede estar vacía")
    private List<String> targetPlaceIds;
}
