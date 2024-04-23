package edu.sjsu.cmpe272.simpleblog.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.*;

@SpringBootApplication
@Command
public class ClientApplication implements CommandLineRunner, ExitCodeGenerator {

    private final RestTemplate restTemplate = new RestTemplate();
    @Autowired
    CommandLine.IFactory iFactory;

    int exitCode;

    @Autowired
    private ConfigurableApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    @Command
    public int post(@Parameters String message, @Parameters(defaultValue = "null") String attachment) {
        String username;
        String encodedPrivateKey = "";
        try (BufferedReader reader = new BufferedReader(new FileReader("mb.ini"))) {
            username = reader.readLine();
            encodedPrivateKey = reader.readLine();

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(encodedPrivateKey));
            PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            Map<String, Object> messageBody = new HashMap<>();
            messageBody.put("author", username);
            messageBody.put("message", message);

            messageBody.put("date", LocalDateTime.now().toString());

            if (!Objects.equals(attachment, "null")) {
                byte[] fileBytes = Files.readAllBytes(Paths.get(attachment));
                String base64EncodedAttachment = Base64.getEncoder().encodeToString(fileBytes);
                messageBody.put("attachment", base64EncodedAttachment);
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String messageStr = messageBody.get("date").toString() + messageBody.get("author").toString() + messageBody.get("message").toString() + messageBody.getOrDefault("attachment", "").toString();
            byte[] messageHash = digest.digest((messageStr).getBytes());
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(messageHash);
            messageBody.put("signature", Base64.getEncoder().encodeToString(signer.sign()));
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    "https://veeravivek.my.to/messages/create",
                    HttpMethod.POST,
                    new HttpEntity<>(messageBody),
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            System.out.println(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 2;
    }

    @Command
    int create(@Parameters String id) {
        try {
            // Check if the username is valid (consisting of only lowercase letters and numbers)
            if (!id.matches("[a-z0-9]+")) {
                System.out.println("Invalid username format. Username must consist of only lowercase letters and numbers.");
                return 1;
            }

            // Generate public-private key pair
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            // Get the private key
            PrivateKey privateKey = keyPair.getPrivate();
            String privateKeyString = Base64.getEncoder().encodeToString(privateKey.getEncoded());

            // Save user ID and private key to mb.ini file
            String configFilePath = "mb.ini";
            try (FileWriter writer = new FileWriter(configFilePath, false)) {
                // Append user ID and private key to the configuration file
                writer.write(id + "\n" + privateKeyString);
            }

            ClientUser clientUser = new ClientUser();
            clientUser.setUser(id);
            clientUser.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

            // Send POST request and get response
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    "https://veeravivek.my.to/user/create",
                    HttpMethod.POST,
                    new HttpEntity<>(clientUser),
                    new ParameterizedTypeReference<Map<String, String>>() {
                    });
            System.out.println(response.getBody().toString());
        } catch (Exception e) {
            System.out.println("Error occurred while creating user. " + e);
        }
        return 2;
    }

    @Command
    int list(@CommandLine.Option(names = {"--starting"}, defaultValue = "-1") int startingIndex,
             @CommandLine.Option(names = {"--count"}, defaultValue = "10") int count,
             @CommandLine.Option(names = {"--save-attachment"}, defaultValue = "false") boolean saveAttachment) {
        try {
            ResponseEntity<List<Map<String, Object>>> messages = restTemplate.exchange(
                    "https://veeravivek.my.to/messages/list",
                    HttpMethod.POST,
                    new HttpEntity<>(Map.of("limit", count, "next", startingIndex)),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    });
            for (Map<String, Object> message : messages.getBody()) {
                String messageString = message.get("message-id") + ": " +
                        message.get("date") + " " + message.get("author") + " says \"" +
                        message.get("message");

                if (message.get("attachment") != null && message.get("attachment") != "null") {
                    messageString += " 📎";
                    if (saveAttachment) {
                        byte[] attachmentBytes = Base64.getDecoder().decode(message.get("attachment").toString());
                        Path filePath = Paths.get(message.get("message-id").toString() + ".out");
                        Files.write(filePath, attachmentBytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    }
                }
                System.out.println(messageString);
            }
        } catch (Exception e) {
            System.out.println("Error occurred while listing messages. " + e);
        }
        return 2;
    }

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(this, iFactory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    @Data
    public static class ClientUser {
        private String user;

        @JsonProperty("public-key")
        private String publicKey;
    }
}
