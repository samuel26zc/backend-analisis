package com.analisis.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoResponseDto {
    private String processor;
    private int availableProcessors;
    private String totalMemoryRam;
    private String maxMemoryJvm;
    private String operatingSystem;
    private String osVersion;
    private String osArch;
    private String javaVersion;
    private String javaVendor;
    private String experimentProtocol;
}
