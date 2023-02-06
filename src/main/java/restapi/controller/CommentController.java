package restapi.controller;

import restapi.annotation.Role;
import spring.annotation.*;
import restapi.exception.NotFoundException;
import restapi.mapper.CommentMapper;
import restapi.pojo.Comment;

import java.util.List;

@RequestMapping("")
@RestController
public class CommentController {

    @Autowired
    CommentMapper commentMapper;

    @Role("USER")
    @GetMapping({"/comments?postId={id}", "/posts/{id}/comments"})
    public List<Comment> getCommentsByPostId(@PathVariable int id) throws NotFoundException {
        List<Comment> commentsByPostId = commentMapper.getCommentsByPostId(id);
        if (commentsByPostId == null || commentsByPostId.size() == 0)
            throw new NotFoundException(String.format("Post with id: '%d' does not have comments or does not exist", id));

        return commentsByPostId;
    }
}