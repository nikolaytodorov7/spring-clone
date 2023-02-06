package restapi.controller;

import restapi.annotation.Role;
import spring.annotation.*;
import restapi.exception.IllegalDataException;
import restapi.exception.NotFoundException;
import restapi.mapper.UserMapper;
import restapi.pojo.User;

import java.util.List;

@RequestMapping("/users")
@RestController
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Role("ADMIN")
    @GetMapping
    public List<User> getAllUsers() throws NotFoundException {
        List<User> allUsers = userMapper.getAllUsers();
        if (allUsers == null || allUsers.size() == 0)
            throw new NotFoundException("Can't load all users! No users found!");

        return allUsers;
    }

    @Role("USER")
    @PostMapping("/register")
    public int createUser(@RequestBody User user) throws IllegalDataException {
        if (user == null)
            throw new IllegalDataException("Invalid user provided!");

        return userMapper.insertUser(user);
    }

    @Role("ADMIN")
    @DeleteMapping("/{id}")
    public int deleteUser(@PathVariable int id) {
        return userMapper.deleteUser(id);
    }
}
