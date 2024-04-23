package edu.sjsu.cmpe272.simpleblog.server.message;

import edu.sjsu.cmpe272.simpleblog.server.user.UserRepository;
import edu.sjsu.cmpe272.simpleblog.server.user.UserTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Controller
public class MessageController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @PostMapping("/messages/create")
    public ResponseEntity<Map<String, Object>> createMessage(@RequestBody Message message)
            throws Exception {
        System.out.println("Received message: " + message);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String attachment = message.getAttachment() != null ? message.getAttachment() : "";
        byte[] messageHash = digest.digest((message.getDate() + message.getAuthor() + message.getMessage() + attachment).getBytes());

        // Get the public key of the author
        UserTable userTable = userRepository.findByUserName(message.getAuthor());
        if (userTable != null) {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(userTable.getPublicKey()));
            PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(messageHash);
            if (verifier.verify(Base64.getDecoder().decode(message.getSignature()))) {
                message = messageRepository.save(message);
                return ResponseEntity.ok(Map.of("message-id", message.getId()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "signature didn't match"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "user not found"));
    }

    @PostMapping("/messages/list")
    public ResponseEntity<List<Message>> listMessages(@RequestBody Map<String, Integer> params) {
        int next = params.get("next");
        int limit = params.get("limit");
        List<Message> messages = new ArrayList<>();
        List<Message> allMessages = messageRepository.findAll();
        if (next == -1) {
            for (int i = allMessages.size() - 1; i > Math.max(allMessages.size() - limit, 0) - 1; i--) {
                if (i < allMessages.size() && allMessages.get(i) != null) {
                    messages.add(allMessages.get(i));
                } else {
                    break;
                }
            }
        } else {
            for (int i = next; i < next + limit; i++) {
                if (i < allMessages.size() && allMessages.get(i) != null) {
                    messages.add(allMessages.get(i));
                } else {
                    break;
                }
            }
        }
        return ResponseEntity.ok(messages);
    }
}
