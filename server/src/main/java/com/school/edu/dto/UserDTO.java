package com.school.edu.dto;

import lombok.Data;
import com.school.edu.entity.User;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String name;
    private User.Role role;
    private String department;
    private String email;
    private String token;
}