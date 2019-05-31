package com.image.server;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static java.lang.System.getenv;

@Component
public class Config {

    private final Path basePath = Paths.get(
            Optional
                    .ofNullable(getenv("BASE_PATH"))
                    .orElseThrow(() -> new NullPointerException("BASE_PATH env var is necessary: the root of images folder")));

    private final int bufferSize = Optional
            .ofNullable(getenv("BUFFER_BYTE_SIZE"))
            .map(Integer::new)
            .orElse(16384);


    Path getBasePath() {
        return basePath;
    }

    int getBufferSize() {
        return bufferSize;
    }
}
