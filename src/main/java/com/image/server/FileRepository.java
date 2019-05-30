package com.image.server;

import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

public interface FileRepository {
    Resource getResource(Path relPath);
    Flux<DataBuffer> readImageFromDisk(Path imagePath);
}
