package com.toonflow.dto.response;

import lombok.Data;

@Data
public class UserInfoResponse {
    private Long id;
    private String name;
    private String email;
    private String avatar;
    private Integer status;
    private String role;
}
