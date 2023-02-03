package restapi.mapper;

import restapi.pojo.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users")
    List<User> getAllUsers();

    @Select("SELECT * FROM users WHERE username = #{username}")
    User findUserByUsername(String username);

    @Insert("INSERT INTO users (id, username, password, role) VALUES (#{id}, #{username}, #{password}, #{role})")
    User insertUser(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    User deleteUser(int id);
}
