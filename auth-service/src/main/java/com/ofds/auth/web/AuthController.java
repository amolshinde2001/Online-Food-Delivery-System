package com.ofds.auth.web;

import com.ofds.auth.model.User;
import com.ofds.auth.repo.UserRepository;
import com.ofds.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository repo, PasswordEncoder encoder, JwtService jwt) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String email = body.get("email");

        if (repo.existsByUsername(username)) {
            return Map.of("status", "error", "message", "Username already exists");
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setEmail(email);
        u.setRoles("ROLE_USER");
        repo.save(u);
        return Map.of("status", "ok", "message", "User registered");
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        var userOpt = repo.findByUsername(username);
        if (userOpt.isEmpty() || !encoder.matches(password, userOpt.get().getPassword())) {
            return Map.of("status", "error", "message", "Invalid credentials");
        }
        var user = userOpt.get();
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRoles());
        String token = jwt.generate(user.getUsername(), claims);
        return Map.of("status", "ok", "token", token);
    }
}
