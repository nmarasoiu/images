package com.image.server;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Path;

public interface ImageResizing {
    //returns ResizedImageFilePath
    Mono<Path> scale(Path imagePath, int x, int y);

    File resizedFile(Path imagePath, int x, int y);
}