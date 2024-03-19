package edu.sjsu.cmpe272.simpleblog.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

@SpringBootApplication
@Command
public class ClientApplication implements CommandLineRunner, ExitCodeGenerator {

    @Autowired
    CommandLine.IFactory iFactory;

    @Autowired
    private ConfigurableApplicationContext context;

    @Command
    public int post(@Parameters String message, @Parameters(defaultValue = "null") String attachment) {
        System.out.println("I wish i knew how to send " + message);
        if (attachment !=null) {
            System.out.println("And upload " + attachment);
        }
        return 2;
    }

    @Command
    int create(@Parameters String id) {
        System.out.println("I wish i knew how to create " + id);
        return 2;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

    int exitCode;

    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(this, iFactory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
