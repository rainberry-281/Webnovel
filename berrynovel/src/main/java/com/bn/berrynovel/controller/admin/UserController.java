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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String LOG_DIVIDER = "============================================================";

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
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(LOG_DIVIDER).append("\n");
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            sb.append(">>>>>>>>>>> User[").append(i + 1).append("]\n")
                    .append("id=").append(u.getId()).append("\n")
                    .append("username=").append(u.getUsername()).append("\n")
                    .append("email=").append(u.getEmail()).append("\n")
                    .append("phone=").append(u.getPhoneNumber()).append("\n")
                    .append("image=").append(u.getImage()).append("\n")
                    .append("role=").append(u.getRole() == null ? null : u.getRole().getName()).append("\n")
                    .append("status=").append(u.getStatus());
            if (i < users.size() - 1)
                sb.append("\n\n");
        }
        sb.append("\n").append(LOG_DIVIDER).append("\n");
        logger.info(sb.toString());
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

        logger.info(
                "\n{}\n>>>>>>>>>>> [CREATE USER - REQUEST]\n"
                        + "username={}\nemail={}\nphone={}\nimage={}\nrole={}\nstatus={}\n{}\n",
                LOG_DIVIDER,
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getImage(),
                user.getRole() == null ? null : user.getRole().getName(),
                user.getStatus(),
                LOG_DIVIDER);

        this.userService.adminCreateUser(user, file);

        logger.info("\n{}\n>>>>>>>>>>> [CREATE USER - SUCCESS] username={}, image={} created successfully\n{}\n",
                LOG_DIVIDER,
                user.getUsername(),
                user.getImage(),
                LOG_DIVIDER);
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

        logger.info(
                "\n{}\n>>>>>>>>>>> [UPDATE USER - REQUEST]\n"
                        + "id={}\nusername={}\nemail={}\nphone={}\nrole={}\nstatus={}\n{}\n",
                LOG_DIVIDER,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole() == null ? null : user.getRole().getName(),
                user.getStatus(),
                LOG_DIVIDER);

        this.userService.updateUser(user, file);

        logger.info("\n{}\n>>>>>>>>>>> [UPDATE USER - SUCCESS] id={}, username={} updated successfully\n{}\n",
                LOG_DIVIDER,
                user.getId(),
                user.getUsername(),
                LOG_DIVIDER);
        return "redirect:/admin/user";
    }

    @PostMapping("/ban/{id}")
    public String banUser(@PathVariable Long id) {
        User beforeUser = this.userService.getUserByID(id);
        logger.info("\n{}\n>>>>>>>>>>> [BAN USER - REQUEST]\nid={}\nusername={}\nimage={}\nstatusBefore={}\n{}\n",
                LOG_DIVIDER,
                id,
                beforeUser == null ? "N/A" : beforeUser.getUsername(),
                beforeUser == null ? null : beforeUser.getImage(),
                beforeUser == null ? null : beforeUser.getStatus(),
                LOG_DIVIDER);

        this.userService.softDeleteUser(id);

        User afterUser = this.userService.getUserByID(id);
        logger.info("\n{}\n>>>>>>>>>>> [BAN USER - SUCCESS]\nid={}\nusername={}\nimage={}\nstatusAfter={}\n{}\n",
                LOG_DIVIDER,
                id,
                afterUser == null ? "N/A" : afterUser.getUsername(),
                afterUser == null ? null : afterUser.getImage(),
                afterUser == null ? null : afterUser.getStatus(),
                LOG_DIVIDER);
        return "redirect:/admin/user";
    }

}
