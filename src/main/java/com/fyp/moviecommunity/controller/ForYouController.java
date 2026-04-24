package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.service.ForYouService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The diversity-weighted feed. See ForYouService for what's actually happening.
 */
@Controller
public class ForYouController {

    private final ForYouService forYou;

    public ForYouController(ForYouService forYou) {
        this.forYou = forYou;
    }

    @GetMapping("/for-you")
    public String forYou(Model model) {
        model.addAttribute("slots", forYou.buildFeed(20));
        return "foryou";
    }
}
