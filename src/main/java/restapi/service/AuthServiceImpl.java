//package springrestapi.service;
//
//import spring.annotation.Autowired;
//import spring.annotation.Service;
//import springrestapi.mapper.UserMapper;
//import springrestapi.pojo.AuthCode;
//import springrestapi.pojo.User;
//import org.springframework.util.StringUtils;
//
//import java.nio.charset.StandardCharsets;
//import java.util.Base64;
//
//import static springrestapi.pojo.AuthCode.*;
//
//@Service
//public class AuthServiceImpl implements AuthService {
//
//    @Autowired
//    UserMapper userMapper;
//
//    @Override
//    public AuthCode validateBasicAuthentication(String basicAuthHeaderValue, String role) {
//        if (StringUtils.hasText(basicAuthHeaderValue) && basicAuthHeaderValue.toLowerCase().startsWith("basic")) {
//            String base64Credentials = basicAuthHeaderValue.substring("Basic".length()).trim();
//            byte[] credDecoded = Base64.getDecoder().decode(base64Credentials);
//            String credentials = new String(credDecoded, StandardCharsets.UTF_8);
//            String[] values = credentials.split(":", 2);
//
//            if (values.length == 2) {
//                String username = values[0];
//                String password = values[1];
//
//                User user = userMapper.findUserByUsername(username);
//                String userRole = user.role;
//                if (userRole == null || !userRole.equals("ADMIN") && role.equals("ADMIN"))
//                    return FORBIDDEN;
//
//                return user.username.equals(username) && user.password.equals(password) ? OK : UNAUTH;
//            }
//        }
//
//        return UNAUTH;
//    }
//}