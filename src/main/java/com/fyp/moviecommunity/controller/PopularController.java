package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.service.CommentService;
import com.fyp.moviecommunity.service.ForYouService;
import com.fyp.moviecommunity.service.ForYouService.Diagnostics;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PopularController {

    private static final int FEED_SIZE = 20;

    private final ForYouService forYou;
    private final CommentService commentService;

    public PopularController(ForYouService forYou, CommentService commentService) {
        this.forYou = forYou;
        this.commentService = commentService;
    }

    @GetMapping("/popular")
    public String popular(Model model) {
        // baseline feed without the quiet-pick interleave
        Diagnostics diagnostics = forYou.diagnostics(FEED_SIZE);
        List<Post> posts = diagnostics.baselineFeed();

        // ids used for the comment previews
        List<Long> postIds = posts.stream().map(Post::getId).toList();

        model.addAttribute("posts", posts);
        model.addAttribute("commentCounts", diagnostics.commentCounts());
        model.addAttribute("topComments", commentService.topCommentByPost(postIds));
        return "popular";
    }
}