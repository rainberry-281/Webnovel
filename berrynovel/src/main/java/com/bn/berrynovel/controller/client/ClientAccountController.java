package com.bn.berrynovel.controller.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.validation.BindingResult;

@Controller
public class ClientAccountController {
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(ClientAccountController.class);
    private static final String LOG_DIVIDER = "============================================================";

    public ClientAccountController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String getProfilePage(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = this.userService.getUserByUsername(username);
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(LOG_DIVIDER).append("\n");
        sb.append(">>>>>>>>>>> User Profile\n");
        sb.append("Username: ").append(user.getUsername()).append("\n");
        sb.append("Full Name: ").append(user.getFullName()).append("\n");
        sb.append("Email: ").append(user.getEmail()).append("\n");
        sb.append("Phone Number: ").append(user.getPhoneNumber()).append("\n");
        sb.append(LOG_DIVIDER).append("\n");
        System.out.println(sb.toString());
        model.addAttribute("user", user);
        return "client/profile/show";
    }

    @GetMapping("/profile/edit")
    public String getEditProfilePage(Model model, Authentication authentication) {
        String username = authentication.getName();
        User user = this.userService.getUserByUsername(username);
        logger.info("\n{}\n>>>>>>>>>>> [EDIT PROFILE - PAGE]\nUsername: {}\n{}\n", LOG_DIVIDER, user.getUsername(),
                LOG_DIVIDER);
        model.addAttribute("user", user);
        return "client/profile/edit";
    }

    @PostMapping("/profile/edit")
    public String postEditProfile(Model model, @ModelAttribute("user") User user, Authentication authentication,
            BindingResult bindingResult, @RequestParam(value = "images") MultipartFile file, HttpSession session,
            RedirectAttributes redirectAttributes) {

        String username = authentication.getName();
        User currentUser = this.userService.getUserByUsername(username);

        String phone = user.getPhoneNumber();

        if (phone != null && !phone.isEmpty()) {
            if (!phone.matches("^[0-9]{10}$")) {
                bindingResult.rejectValue("phoneNumber", "error.user", "Phone number must be exactly 10 digits");
            }
        }

        if (bindingResult.hasErrors()) {
            logger.warn(
                    "\n{}\n>>>>>>>>>>> [EDIT PROFILE - VALIDATION ERROR]\nUsername: {}\nPhone Number: {}\nErrors: {}\n{}",
                    LOG_DIVIDER,
                    currentUser.getUsername(),
                    user.getPhoneNumber(),
                    bindingResult.getAllErrors(),
                    LOG_DIVIDER);
            user.setImage(currentUser.getImage());
            model.addAttribute("user", user);
            return "client/profile/edit";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(LOG_DIVIDER).append("\n");
        sb.append(">>>>>>>>>>> [EDIT PROFILE - REQUEST]\n");
        sb.append("Username: ").append(currentUser.getUsername()).append("\n");
        sb.append("Full Name: ").append(user.getFullName()).append("\n");
        sb.append("Email: ").append(user.getEmail()).append("\n");
        sb.append("Phone Number: ").append(user.getPhoneNumber()).append("\n");
        sb.append(LOG_DIVIDER).append("\n");

        logger.info(sb.toString());

        User updateUser = this.userService.updateUser(user, file);

        logger.info(
                "\n{}\n>>>>>>>>>>> [EDIT PROFILE - SUCCESS]\nUsername: {}\nUpdated Full Name: {}\nUpdated Phone Number: {}\n{}\n",
                LOG_DIVIDER,
                updateUser.getUsername(),
                updateUser.getFullName(),
                updateUser.getPhoneNumber(),
                LOG_DIVIDER);

        session.setAttribute("phoneNumber", updateUser.getPhoneNumber());
        session.setAttribute("avatar", updateUser.getImage());
        session.setAttribute("fullName", updateUser.getFullName());
        return "redirect:/profile";
    }

}
