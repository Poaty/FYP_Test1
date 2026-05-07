package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.service.CommentService;
import com.fyp.moviecommunity.service.ForYouService;
import com.fyp.moviecommunity.service.ForYouService.FeedSlot;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ForYouController {

    private static final int DEFAULT_FEED_SIZE = 20;

    private final ForYouService forYou;
    private final CommentService commentService;

    public ForYouController(ForYouService forYou, CommentService commentService) {
        this.forYou = forYou;
        this.commentService = commentService;
    }

    @GetMapping("/for-you")
    public String forYou(Model model) {
        // build the diversity weighted feed
        List<FeedSlot> slots = forYou.buildFeed(DEFAULT_FEED_SIZE);

        // ids needed for the comment previews
        List<Long> postIds = slots.stream().map(s -> s.post().getId()).toList();

        model.addAttribute("slots", slots);
        model.addAttribute("topComments", commentService.topCommentByPost(postIds));
        return "foryou";
    }
}