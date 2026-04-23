package com.fyp.moviecommunity.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /** Send anyone hitting root straight to the feed (Spring Security
     *  will bounce them to /login if not authenticated). */
    @GetMapping("/")
    public String root() {
        return "redirect:/feed";
    }

    /** Placeholder feed -- empty list for now. We'll flesh this out
     *  once users can actually create posts. */
    @GetMapping("/feed")
    public String feed(Model model) {
        model.addAttribute("posts", List.of());
        return "feed";
    }
}
