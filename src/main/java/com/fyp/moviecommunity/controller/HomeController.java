package com.fyp.moviecommunity.controller;

import com.fyp.moviecommunity.model.Post;
import com.fyp.moviecommunity.repository.CommentRepository;
import com.fyp.moviecommunity.repository.PostRepository;
import com.fyp.moviecommunity.service.CommentService;
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

    private static final int FEED_PAGE_SIZE = 20;

    private final PostRepository posts;
    private final CommentRepository comments;
    private final CommentService commentService;

    public HomeController(PostRepository posts,
                          CommentRepository comments,
                          CommentService commentService) {
        this.posts = posts;
        this.comments = comments;
        this.commentService = commentService;
    }

    @GetMapping("/")
    public String root() {
        // root just sends users to the feed
        return "redirect:/feed";
    }

    @GetMapping("/feed")
    public String feed(@RequestParam(defaultValue = "0") int page, Model model) {
        // stop negative page numbers
        int safePage = Math.max(0, page);

        Page<Post> feedPage = posts.findPageWithAuthors(
                PageRequest.of(safePage, FEED_PAGE_SIZE));
        List<Post> rows = feedPage.getContent();

        // ids for the posts shown on this page
        List<Long> ids = rows.stream().map(Post::getId).toList();

        // comment totals for each feed card
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            for (Object[] row : comments.countByPostIdIn(ids)) {
                Long postId = (Long) row[0];
                long total = ((Number) row[1]).longValue();
                counts.put(postId, total);
            }
        }

        // preview comment shown under each post
        Map<Long, CommentService.TopCommentPreview> topComments =
                commentService.topCommentByPost(ids);

        model.addAttribute("posts", rows);
        model.addAttribute("commentCounts", counts);
        model.addAttribute("topComments", topComments);
        model.addAttribute("currentPage", feedPage.getNumber());
        model.addAttribute("totalPages", feedPage.getTotalPages());
        model.addAttribute("hasPrevious", feedPage.hasPrevious());
        model.addAttribute("hasNext", feedPage.hasNext());
        return "feed";
    }
}