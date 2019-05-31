package com.image.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.scheduler.Schedulers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.Executors;

import static java.lang.System.getenv;

@SpringBootApplication
public class ServerApplication {
    private final static Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    private Path basePath = Paths.get(
            Optional
                    .ofNullable(getenv("BASE_PATH"))
                    .orElseThrow(() -> new NullPointerException("BASE_PATH env var is necessary: the root of images folder")));

    private int bufferSize = Optional
            .ofNullable(getenv("BUFFER_BYTE_SIZE"))
            .map(Integer::new)
            .orElse(16384);

    @Bean
    public ImageResizing imageResizing() {
        int resizingThreadCount = Runtime.getRuntime().availableProcessors();
        return new DefaultImageResizing(basePath, Schedulers.fromExecutorService(Executors.newFixedThreadPool(resizingThreadCount)));
    }

    @Bean
    public FileRepository imageRepository(DataBufferFactory dataBufferFactory) {
        return new DefaultFileRepository(basePath, bufferSize, dataBufferFactory);
    }

    @Bean
    public DefaultDataBufferFactory getBufferFactory() {
        return new DefaultDataBufferFactory();
    }

    public static void main(String[] args) {
        logger.debug("BASE_PATH={}", getenv("BASE_PATH"));
        SpringApplication.run(ServerApplication.class, args);
    }

}
