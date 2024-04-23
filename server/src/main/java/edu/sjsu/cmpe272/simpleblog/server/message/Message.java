package edu.sjsu.cmpe272.simpleblog.server.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("message-id")
    Integer id;

    String author;
    String message;
    @Column(length = 15000)
    String attachment;
    String date;
    @Column(length = 2000)
    String signature;
}
