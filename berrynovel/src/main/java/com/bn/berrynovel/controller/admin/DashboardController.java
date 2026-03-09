package com.bn.berrynovel.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.bn.berrynovel.repository.UserRepository;
import com.bn.berrynovel.repository.NovelRepository;
import com.bn.berrynovel.repository.GenreRepository;

@Controller
@RequestMapping("/admin")
public class DashboardController {

    private final UserRepository userRepository;
    private final NovelRepository novelRepository;
    private final GenreRepository genreRepository;

    public DashboardController(UserRepository userRepository, NovelRepository novelRepository,
            GenreRepository genreRepository) {
        this.userRepository = userRepository;
        this.novelRepository = novelRepository;
        this.genreRepository = genreRepository;
    }

    @GetMapping("/dashboard")
    public String getDashboard(Model model) {
        model.addAttribute("userCount", this.userRepository.count());
        model.addAttribute("novelCount", this.novelRepository.count());
        model.addAttribute("genreCount", this.genreRepository.count());
        return "admin/dashboard/show";
    }
}
