package com.bn.berrynovel.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bn.berrynovel.domain.User;
import com.bn.berrynovel.domain.Role;
import com.bn.berrynovel.domain.dto.RegisterDTO;
import com.bn.berrynovel.repository.UserRepository;
import com.bn.berrynovel.repository.RoleRepository;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageService imageService;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
            ImageService imageService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageService = imageService;
    }

    public void createUserByClient(RegisterDTO registerDTO) {
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setFullName(registerDTO.getFullName());
        user.setEmail(registerDTO.getEmail());
        user.setPhoneNumber(registerDTO.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRole(this.roleRepository.findByName("USER"));
        String imageName = "defaultavatar.png";
        user.setImage(imageName);
        User savedUser = this.userRepository.save(user);
    }

    public void adminCreateUser(User user, MultipartFile file) {
        Role roleInDataBase = this.roleRepository.findByName(user.getRole().getName());
        user.setRole(roleInDataBase);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        String imageName = "defaultavatar.png";
        if (file != null && !file.isEmpty()) {
            // Nếu người dùng có upload ảnh
            imageName = this.imageService.handleImage(file, "avatar");
        }
        user.setImage(imageName);

        User savedUser = this.userRepository.save(user);
    }

    public User updateUser(User user, MultipartFile file) {
        User currentUser = this.userRepository.findById(user.getId()).get();

        currentUser.setRole(this.roleRepository.findByName(user.getRole().getName()));

        currentUser.setFullName(user.getFullName());

        currentUser.setPhoneNumber(user.getPhoneNumber());

        if (file != null && !file.isEmpty()) {
            if (currentUser.getImage() != null
                    && !currentUser.getImage().isEmpty()
                    && !currentUser.getImage().equals("defaultavatar.png")) {
                String imageName = this.imageService.handleImage(file, "avatar");
                currentUser.setImage(imageName);
            }
        }

        return this.userRepository.save(currentUser);
    }

    public List<User> getUserList() {
        return this.userRepository.findAll();
    }

    public User getUserByID(int id) {
        return this.userRepository.findFirstById(id);
    }

    public User getUserByUsername(String username) {
        return this.userRepository.findByUsername(username);
    }

    public void softDeleteUser(int id) {
        User userInDataBase = this.userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userInDataBase.setStatus(!userInDataBase.getStatus());
        this.userRepository.save(userInDataBase);
    }
}
