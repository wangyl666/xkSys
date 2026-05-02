package com.school.edu.service;

import com.school.edu.dto.LoginRequest;
import com.school.edu.dto.UserDTO;
import com.school.edu.entity.User;
import com.school.edu.repository.UserRepository;
import com.school.edu.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public UserDTO login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        
        if (!userOpt.isPresent()) {
            throw new RuntimeException("用户不存在");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        String token = jwtTokenUtil.generateToken(user.getUsername(), user.getRole().name());
        return toUserDTO(user, token);
    }

    public UserDTO register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return toUserDTO(savedUser, null);
    }

    public UserDTO getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        return toUserDTO(user, null);
    }

    private UserDTO toUserDTO(User user, String token) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setName(user.getName());
        dto.setRole(user.getRole());
        dto.setDepartment(user.getDepartment());
        dto.setEmail(user.getEmail());
        if (token != null) {
            dto.setToken(token);
        }
        return dto;
    }
}