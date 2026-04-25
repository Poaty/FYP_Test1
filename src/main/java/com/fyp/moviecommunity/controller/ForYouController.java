package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.service.CommentService;
import com.fyp.moviecommunity.service.ForYouService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The diversity-weighted feed. See ForYouService for the algorithm itself.
 * This controller adds the top-comment previews on top of the slot list.
 */
@Controller
public class ForYouController {

    private final ForYouService forYou;
    private final CommentService commentService;

    public ForYouController(ForYouService forYou, CommentService commentService) {
        this.forYou = forYou;
        this.commentService = commentService;
    }

    @GetMapping("/for-you")
    public String forYou(Model model) {
        List<ForYouService.FeedSlot> slots = forYou.buildFeed(20);

        // Same top-comment preview map shape as the plain feed -- template
        // looks up by post id.
        List<Long> postIds = slots.stream().map(s -> s.post().getId()).toList();
        model.addAttribute("slots", slots);
        model.addAttribute("topComments", commentService.topCommentByPost(postIds));
        return "foryou";
    }
}
