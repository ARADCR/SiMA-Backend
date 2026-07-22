package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnassignedDeviceDto {
    private Integer id;
    private String type;
    private String mac;
}
