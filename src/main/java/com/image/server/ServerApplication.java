package com.image.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.scheduler.Schedulers;

@SpringBootApplication
public class ServerApplication {
    private final Config config;

    public ServerApplication(Config config) {
        this.config = config;
    }

    @Bean
    public ImageResizing imageResizing() {
        return new DefaultImageResizing(config.getBasePath(), blockingExecution());
    }

    @Bean
    BlockingExecution blockingExecution() {
        return new BlockingExecution(Schedulers.elastic());
    }

    @Bean
    public FileRepository imageRepository(DataBufferFactory dataBufferFactory) {
        return new DefaultFileRepository(config.getBasePath(), config.getBufferSize(), dataBufferFactory);
    }

    @Bean
    public DefaultDataBufferFactory getBufferFactory() {
        return new DefaultDataBufferFactory();
    }

    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);
    }

}
