package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    private final PostRepository posts;
    private final CommentRepository comments;

    public HomeController(PostRepository posts, CommentRepository comments) {
        this.posts = posts;
        this.comments = comments;
    }

    /** Send anyone hitting root straight to the feed. Spring Security bounces
     *  anonymous users to /login. */
    @GetMapping("/")
    public String root() {
        return "redirect:/feed";
    }

    /**
     * Plain chronological feed, paginated. 20 posts per page, newest first.
     * User + movie eagerly loaded in one SQL (JOIN FETCH); comment counts
     * come from a second batched GROUP BY. Two DB round-trips total per
     * page render, regardless of how many posts are on screen.
     */
    @GetMapping("/feed")
    public String feed(@RequestParam(defaultValue = "0") int page, Model model) {
        int pageSize = 20;
        Page<Post> feedPage = posts.findPageWithAuthors(
                PageRequest.of(Math.max(0, page), pageSize));

        // Batch-fetch comment counts for every post on screen.
        List<Long> ids = feedPage.getContent().stream().map(Post::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : comments.countByPostIdIn(ids)) {
                counts.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        model.addAttribute("posts", feedPage.getContent());
        model.addAttribute("commentCounts", counts);
        model.addAttribute("currentPage", feedPage.getNumber());
        model.addAttribute("totalPages", feedPage.getTotalPages());
        model.addAttribute("hasPrevious", feedPage.hasPrevious());
        model.addAttribute("hasNext", feedPage.hasNext());
        return "feed";
    }
}
