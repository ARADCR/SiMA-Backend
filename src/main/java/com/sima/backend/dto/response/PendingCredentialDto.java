package com.sima.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingCredentialDto {
    private Integer id;
    private String initials;
    private String name;
    private String docType;
    private String date;
}
