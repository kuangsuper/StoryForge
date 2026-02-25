package com.toonflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64)
    private String name;
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64)
    private String password;
    private String email;
}
