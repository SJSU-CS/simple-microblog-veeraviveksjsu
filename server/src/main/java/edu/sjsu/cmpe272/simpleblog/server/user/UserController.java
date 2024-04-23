package edu.sjsu.cmpe272.simpleblog.server.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Optional;

@Controller
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/user/create")
    public ResponseEntity<Map<String, String>> createUser(@RequestBody UserTable userTable) {
        System.out.println(userRepository.findByUserName(userTable.getUserName()));
        if (userRepository.findByUserName(userTable.getUserName()) != null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User Exists"));
        }
        String publicKey = userTable.getPublicKey().replace("-----BEGIN PUBLIC KEY-----\\n", "");
        publicKey = publicKey.replace("\\n-----END PUBLIC KEY-----\\n", "");
        userTable.setPublicKey(publicKey);
        userRepository.save(userTable);
        return ResponseEntity.ok(Map.of("message", "welcome"));

    }

    @GetMapping("/user/:id/public-key")
    public ResponseEntity<String> getPublicKey(@PathVariable String id) throws NoSuchAlgorithmException, InvalidKeySpecException {
        UserTable user = userRepository.findByUserName(id);
        if (user!= null) {
            String publicKey = user.getPublicKey();

            String key = "-----BEGIN PUBLIC KEY-----\n" + user.getPublicKey();
            int numChunks = (int) Math.ceil((double) publicKey.length() / 64);
            for (int i = 0; i < numChunks; i++) {
                int startIndex = i * 64;
                int endIndex = Math.min(startIndex + 64, publicKey.length());
                key += publicKey.substring(startIndex, endIndex) + "\n";
            }
            key += "\n-----END PUBLIC KEY-----";
            return ResponseEntity.ok(key);
        }
        return ResponseEntity.badRequest().body("user does not exists");
    }
}
