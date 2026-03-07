package com.bn.berrynovel.controller.admin;

import java.util.List;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

import com.bn.berrynovel.service.UserService;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.bn.berrynovel.domain.Role;
import com.bn.berrynovel.domain.User;

@Controller
@RequestMapping("/admin/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String getUserListPage(Model model) {
        List<User> users = this.userService.getUserList();
        System.out.println(">>>>>>> user list: " + users);
        model.addAttribute("users", users);
        return "admin/user/show";
    }

    @GetMapping("/create")
    public String getUserCreatePage(Model model) {
        model.addAttribute("role", new Role());
        model.addAttribute("newUser", new User());
        return "/admin/user/create";
    }

    @PostMapping("/create")
    public String postCreateUser(Model model, @ModelAttribute("newUser") @Valid User user,
            BindingResult userBindingResult, @RequestParam(value = "images", required = false) MultipartFile file) {
        List<FieldError> errors = userBindingResult.getFieldErrors();
        for (FieldError error : errors) {
            System.out.println(">>>>" + error.getObjectName() + " - " + error.getDefaultMessage());
        }

        if (userBindingResult.hasErrors()) {
            return "admin/user/create";
        }
        this.userService.adminCreateUser(user, file);
        return "redirect:/admin/user";
    }

    @GetMapping("/update/{id}")
    public String getUpdatePage(Model model, @PathVariable int id) {
        User user = this.userService.getUserByID(id);
        model.addAttribute("newUser", user);
        return "admin/user/update";
    }

    @PostMapping("/update/{id}")
    public String updateUserPage(@PathVariable int id, @ModelAttribute("newUser") User user,
            @RequestParam(value = "imageFile", required = false) MultipartFile file) {
        this.userService.updateUser(user, file);
        return "redirect:/admin/user";
    }

}
