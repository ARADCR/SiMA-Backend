package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardUserDto {
    private Integer id;
    private String initials;
    private String avatarBg;
    private String name;
    private String email;
    private String role;
    private String roleBg;
    private String roleColor;
    private String lastAccess;
}
