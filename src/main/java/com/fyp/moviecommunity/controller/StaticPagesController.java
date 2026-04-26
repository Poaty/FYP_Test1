package com.fyp.moviecommunity.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Three policy pages. Plain HTML, no auth required, accessible from the
 * footer link on every "front-of-house" page.
 *
 *   GET /privacy     -- what data we collect, how it's stored, how to delete
 *   GET /terms       -- terms of use (18+, content rules, project context)
 *   GET /guidelines  -- community norms (honest takes, no harassment, etc.)
 *
 * These pages exist to honour LSEPI commitments from the PPD:
 *   - DPA 2018 / GDPR transparency (legal)
 *   - Informed consent (ethical)
 *   - Clear community moderation policy (social)
 */
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
