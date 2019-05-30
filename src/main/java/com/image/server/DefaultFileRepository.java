package com.image.server;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Flux;

import java.nio.file.Path;

public class DefaultFileRepository implements FileRepository {
    private final Path basePath;
    private final int bufferSize;
    private final DataBufferFactory bufferFactory;

    DefaultFileRepository(Path basePath, int bufferSize, DataBufferFactory bufferFactory) {
        this.basePath = basePath;
        this.bufferSize = bufferSize;
        this.bufferFactory = bufferFactory;
    }

    @Override
    public Flux<DataBuffer> readImageFromDisk(Path relPath) {
        return DataBufferUtils.read(getResource(relPath), bufferFactory, bufferSize);
    }

    public Resource getResource(Path relPath) {
        return new FileSystemResource(basePath.resolve(relPath));
    }
}
