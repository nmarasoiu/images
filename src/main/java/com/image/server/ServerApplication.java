package com.image.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executors;

import static java.lang.System.getenv;

@SpringBootApplication
public class ServerApplication {
    private final static Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    private final Config config;

    public ServerApplication(Config config) {
        this.config = config;
    }

    @Bean
    public ImageResizing imageResizing() {
        int resizingThreadCount = Runtime.getRuntime().availableProcessors();
        return new DefaultImageResizing(
                config.getBasePath(),
                Schedulers.fromExecutorService(Executors.newFixedThreadPool(resizingThreadCount)));
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
        logger.debug("BASE_PATH={}", getenv("BASE_PATH"));
        SpringApplication.run(ServerApplication.class, args);
    }

}
