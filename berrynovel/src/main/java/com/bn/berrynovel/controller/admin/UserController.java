package com.bn.berrynovel.controller.admin;

import com.bn.berrynovel.service.ImageService;
import java.util.List;
import java.util.Optional;
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
import com.bn.berrynovel.domain.PaginationQuery;
import com.bn.berrynovel.service.PaginationService;

@Controller
@RequestMapping("/admin/user")
public class UserController {

    private final ImageService imageService;
    private final UserService userService;
    private final PaginationService paginationService;

    public UserController(UserService userService, ImageService imageService, PaginationService paginationService) {
        this.userService = userService;
        this.imageService = imageService;
        this.paginationService = paginationService;
    }

    @GetMapping
    public String getUserListPage(Model model,
            @RequestParam(value = "page") Optional<String> pageOptional,
            @RequestParam(value = "keyword", required = false) String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        PaginationQuery paginationQuery = this.paginationService.AdminUserPagination(pageOptional, 8,
                normalizedKeyword);
        List<User> users = paginationQuery.getNvs().getContent();
        model.addAttribute("users", users);
        model.addAttribute("currentPage", paginationQuery.getPage());
        model.addAttribute("totalPage", paginationQuery.getNvs().getTotalPages());
        model.addAttribute("keyword", normalizedKeyword);
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

        if (userService.checkUsernameExists(user.getUsername())) {
            userBindingResult.rejectValue("username", "error.user", "Username already exists");
        }

        if (userService.checkEmailExists(user.getEmail())) {
            userBindingResult.rejectValue("email", "error.user", "Email already exists");
        }

        String phone = user.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            if (!phone.matches("^[0-9]{10}$")) {
                userBindingResult.rejectValue("phoneNumber", "error.user", "Phone number must be exactly 10 digits");
            }
        }

        if (userBindingResult.hasErrors()) {
            return "admin/user/create";
        }

        this.userService.adminCreateUser(user, file);
        return "redirect:/admin/user";
    }

    @GetMapping("/update/{id}")
    public String getUpdatePage(Model model, @PathVariable Long id) {
        User user = this.userService.getUserByID(id);
        model.addAttribute("newUser", user);
        return "admin/user/update";
    }

    @PostMapping("/update/{id}")
    public String updateUserPage(@PathVariable Long id, @ModelAttribute("newUser") User user,
            BindingResult userBindingResult, @RequestParam(value = "images", required = false) MultipartFile file) {

        String phone = user.getPhoneNumber();
        if (phone != null && !phone.isEmpty()) {
            if (!phone.matches("^[0-9]{10}$")) {
                userBindingResult.rejectValue("phoneNumber", "error.user", "Phone number must be exactly 10 digits");
            }
        }

        if (userBindingResult.hasErrors()) {
            return "admin/user/update";
        }

        this.userService.updateUser(user, file);
        return "redirect:/admin/user";
    }

    @PostMapping("/ban/{id}")
    public String banUser(@PathVariable Long id) {
        this.userService.softDeleteUser(id);
        return "redirect:/admin/user";
    }

}
