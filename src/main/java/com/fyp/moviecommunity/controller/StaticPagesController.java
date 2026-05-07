package com.fyp.moviecommunity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StaticPagesController {

    @GetMapping("/privacy")
    public String privacy() {
        return "static/privacy";
    }

    @GetMapping("/terms")
    public String terms() {
        return "static/terms";
    }

    @GetMapping("/guidelines")
    public String guidelines() {
        return "static/guidelines";
    }
}