package com.bn.berrynovel.controller.client;

import org.springframework.stereotype.Controller;
import com.bn.berrynovel.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.bn.berrynovel.service.NovelService;
import com.bn.berrynovel.domain.dto.RegisterDTO;
import com.bn.berrynovel.domain.User;

@Controller
public class HomepageController {
    private final UserService userService;
    private final NovelService novelService;

    public HomepageController(UserService userService, NovelService novelService) {
        this.userService = userService;
        this.novelService = novelService;
    }

    @GetMapping("/home")
    public String getHomePage(Model model, HttpServletRequest request) {
        model.addAttribute("completedNovels", this.novelService.getHomepageCompletedNovels());
        model.addAttribute("novels", this.novelService.getHomepageOriginalNovels());
        model.addAttribute("translatedNovels", this.novelService.getHomepageTranslatedNovels());
        return "client/homepage/show";
    }

    @GetMapping("/")
    public String autoDirectHomePage() {
        return "redirect:/home";
    }

    @GetMapping("/register")
    public String getRegistrationPage(Model model) {
        model.addAttribute("newUser", new RegisterDTO());
        return "client/auth/register";
    }

    @PostMapping("/register")
    public String handelRegister(@ModelAttribute("newUser") @Valid RegisterDTO registerDTO,
            BindingResult bindingResult, Model model) {

        String phone = registerDTO.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            if (!phone.matches("^[0-9]{10}$")) {
                bindingResult.rejectValue("phoneNumber", "error.user", "Phone number must be exactly 10 digits");
            }
        }

        if (bindingResult.hasErrors()) {
            return "client/auth/register";
        }

        this.userService.createUserByClient(registerDTO);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String getLoginPage(Model model) {
        model.addAttribute("loginUser", new User());
        return "client/auth/login";
    }

}
