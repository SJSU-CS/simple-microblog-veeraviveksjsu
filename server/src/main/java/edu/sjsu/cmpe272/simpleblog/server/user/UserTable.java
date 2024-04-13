package edu.sjsu.cmpe272.simpleblog.server.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class UserTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @JsonProperty("user")
    @Column(unique = true, name = "user_name")
    private String userName;

    @Column(length = 2000)
    @JsonProperty("public-key")
    private String publicKey;
}
