package restapi.mapper;

import restapi.pojo.Post;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PostMapper {

    @Select("SELECT * FROM posts")
    List<Post> getAllPosts();

    @Select("SELECT * FROM posts WHERE id = #{id}")
    Post getPostById(int id);

    @Insert("INSERT INTO posts (userId, title, body) VALUES (#{userId}, #{title}, #{body})")
    int insertPost(Post post);

    @Update("UPDATE posts SET userId = #{userId}, title = #{title}, body = #{body} WHERE id = #{id}")
    int updatePost(Post post);

    @Delete("DELETE FROM posts WHERE id = #{id}")
    int deletePost(int id);
}
