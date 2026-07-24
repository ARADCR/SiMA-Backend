package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private DashboardStatsDto stats;
    private List<DashboardUserDto> usuarios;
    private List<PendingCredentialDto> pendingCredentials;
    private List<UnassignedDeviceDto> unassignedDevices;
}
