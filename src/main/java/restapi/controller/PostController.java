package restapi.controller;

import restapi.mapper.PostMapper;
import spring.annotation.*;
import restapi.annotation.Role;
import restapi.exception.IllegalDataException;
import restapi.exception.NotFoundException;
import restapi.pojo.Post;

import java.util.List;

@RequestMapping("/posts")
@RestController
public class PostController {

    @Autowired
    private PostMapper postMapper;

    @Role("USER")
    @GetMapping
    public List<Post> getAllPosts() throws NotFoundException {
        List<Post> allPosts = postMapper.getAllPosts();
        if (allPosts == null || allPosts.size() == 0)
            throw new NotFoundException("Can't load all posts! No posts found!");

        return allPosts;
    }

    @Role("USER")
    @GetMapping("/{id}")
    public Post getPostById(@PathVariable int id) throws NotFoundException {
        Post post = postMapper.getPostById(id);
        if (post == null)
            throw new NotFoundException(String.format("Post with id: '%d' does not exist!", id));

        return post;
    }

    @Role("USER")
    @PostMapping
    public Post createPost(@RequestBody Post post) throws IllegalDataException {
        if (post == null)
            throw new IllegalDataException("Invalid post provided!");

        return postMapper.insertPost(post);
    }

    @Role("USER")
    @PutMapping
    public Post updatePost(@RequestBody Post post) throws NotFoundException, IllegalDataException {
        Post oldPost = getPostById(post.id);
        if (oldPost.equals(post))
            throw new IllegalDataException("Unable to update post which is the same!");

        return postMapper.updatePost(post);
    }

    @Role("USER")
    @DeleteMapping(value = "/{id}")
    public Post deletePost(@PathVariable int id) throws NotFoundException {
        getPostById(id);
        return postMapper.deletePost(id);
    }
}