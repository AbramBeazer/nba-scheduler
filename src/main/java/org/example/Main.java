package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        try (InputStream stream = Main.class.getClassLoader().getResourceAsStream("nba.json")) {
            Objects.requireNonNull(stream, "Input stream cannot be null");
            League nba = MAPPER.readValue(stream.readAllBytes(), League.class);

            System.out.println(MAPPER.writeValueAsString(nba));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}