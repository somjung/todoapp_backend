package com.todoapp.config;

import com.todoapp.entity.User;
import com.todoapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Create default user if it doesn't exist
        if (!userRepository.existsByUsername("admin")) { 
            User defaultUser = new User();
            defaultUser.setUsername("admin"); 
            defaultUser.setName("Admin User");
            defaultUser.setEmail("admin@example.com");
            defaultUser.setPassword(passwordEncoder.encode("password"));
            
            userRepository.save(defaultUser);
            
            System.out.println("=========================================");
            System.out.println(">>>> Default admin user created <<<<");
            System.out.println("Username: admin");
            System.out.println("Password: password");
            System.out.println("=========================================");
        }
    }
}