package org.local.websocketapp.Utils;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.local.websocketapp.Models.AuthRequest;
import org.local.websocketapp.Models.UserC;
import org.local.websocketapp.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService1 {

    private final JwtTokenUtils jwtTokenUtils;

    @Autowired
    AuthenticationManager authManager;


    private final UserRepository repo;


    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserC register(UserC user) {
        user.setPassword(encoder.encode(user.getPassword()));
        repo.save(user);
        return user;
    }

    public String verify(AuthRequest user) {

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getName(), user.getPassword());
        Authentication authentication;
        System.out.println(user.getName() +" "+ user.getPassword());
        try {
            authentication = authManager.authenticate(token);
            System.out.println("Authentication result: " + authentication);

            if (authentication != null && authentication.isAuthenticated()) {
                System.out.println("token generated");
                return jwtTokenUtils.generateToken(user.getName());
            } else {
                System.out.println("Authentication failed: " + authentication);
                return "fail";
            }
        } catch (AuthenticationException e) {
            System.out.println("Error during authentication: " + e.getMessage());
            return "fail";
        }
    }
}