package com.image.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class ServerApplication {

    @Value("${basePath}")
    private Path basePath;

    @Value("${bufferByteSize}")
    private int bufferSize;

    @Bean
    public ImageResizing imageResizing() {
        return new DefaultImageResizing(basePath);
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
        SpringApplication.run(ServerApplication.class, args);
    }

}
