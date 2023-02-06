package restapi.mapper;

import restapi.pojo.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper {

    @Select("SELECT * FROM comments WHERE postId = #{id}")
    List<Comment> getCommentsByPostId(int id);
}
