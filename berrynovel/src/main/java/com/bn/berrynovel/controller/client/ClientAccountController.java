package com.bn.berrynovel.controller.client;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.bn.berrynovel.service.UserService;

import jakarta.servlet.http.HttpSession;

import com.bn.berrynovel.domain.User;

import org.springframework.ui.Model;

@Controller
public class ClientAccountController {
    private final UserService userService;

    public ClientAccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String getProfilePage(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = this.userService.getUserByUsername(username);
        model.addAttribute("user", user);
        return "client/profile/show";
    }

    @GetMapping("/profile/edit")
    public String getEditProfilePage(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = this.userService.getUserByUsername(username);
        model.addAttribute("user", user);
        return "client/profile/edit";
    }

    @PostMapping("/profile/edit")
    public String postEditProfile(Model model, @ModelAttribute("user") User user, Authentication authentication,
            @RequestParam(value = "images") MultipartFile file, HttpSession session,
            RedirectAttributes redirectAttributes) {

        String username = authentication.getName();
        User updateUser = this.userService.updateUser(user, file);

        session.setAttribute("phoneNumber", updateUser.getPhoneNumber());
        session.setAttribute("avatar", updateUser.getImage());
        session.setAttribute("fullName", updateUser.getFullName());
        return "redirect:/profile";
    }

}
