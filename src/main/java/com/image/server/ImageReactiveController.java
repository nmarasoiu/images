package com.image.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@RestController
public class ImageReactiveController {
    private final FileRepository fileRepository;
    private final ImageResizing imageResizing;
    private final DataBufferFactory dataBufferFactory;
    private final int maxSizeForResize;
    private final int minFreeBytesForResize;

    public ImageReactiveController(FileRepository fileRepository, ImageResizing imageResizing,
                                   DataBufferFactory dataBufferFactory,
                                   @Value("${maxSizeForResize:4500}") int maxSizeForResize,
                                   @Value("${minFreeBytesForResize:134217728}") int minFreeBytesForResize) {
        this.fileRepository = fileRepository;
        this.imageResizing = imageResizing;
        this.dataBufferFactory = dataBufferFactory;
        this.maxSizeForResize = maxSizeForResize;
        this.minFreeBytesForResize = minFreeBytesForResize;
    }

    @GetMapping(path = "/image/{path}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> streamImage(@PathVariable(name = "path") Path imageRelPath,
                                                              @RequestParam(required = false) String size) {
        if(!fileRepository.getResource(imageRelPath).exists()){
            return getFluxResponseEntity(HttpStatus.NOT_FOUND, "The image was not found");
        }
        if (size == null) {
            return streamFileAsFluxResponseEntity(Mono.just(imageRelPath));
        }
        Optional<int[]> sizeXYOpt = parseSize(size);
        if (!sizeXYOpt.isPresent()) {
            return (getFluxResponseEntity(HttpStatus.BAD_REQUEST,
                    "The size query param should be like 300x400 and max(w,h)<"+maxSizeForResize));
        }
        int[] xy = sizeXYOpt.get();
        int x = xy[0];
        int y = xy[1];
        if (Math.max(x, y) >= maxSizeForResize) {
            return (getFluxResponseEntity(HttpStatus.BAD_REQUEST,
                    "The size query param should be like 300x400 and max(w,h)<"+maxSizeForResize));
        }
        File resizedFile = imageResizing.resizedFile(imageRelPath, x, y);
        if (resizedFile.exists()) {
            return streamFileAsFluxResponseEntity(Mono.fromSupplier(resizedFile::toPath));
        }
        if (Runtime.getRuntime().freeMemory() > minFreeBytesForResize) {
            Mono<Path> scaledPathFlux = imageResizing.scale(imageRelPath, x, y);
            return streamFileAsFluxResponseEntity(scaledPathFlux);
        } else {
            return (getFluxResponseEntity(HttpStatus.SERVICE_UNAVAILABLE,
                    "The server does not have enough memory to process image resizing at the moment"));
        }
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> streamFileAsFluxResponseEntity(Mono<Path> pathFlux) {
        return pathFlux
                .map(path -> {
                    Flux<DataBuffer> imageStream = fileRepository.readImageFromDisk(path);
                    return ResponseEntity
                            .status(HttpStatus.OK)
                            .contentType(getContentType(path))
                            .body(imageStream);
                });
    }

    private Optional<int[]> parseSize(@RequestParam(required = false) String size) {
        String[] xy = size.trim().toLowerCase().split("x");
        if (xy.length == 2 && isOnlyDigits(xy[0]) && isOnlyDigits(xy[1])) {
            return Optional.of(Arrays.stream(xy).mapToInt(Integer::parseInt).toArray());
        } else {
            return Optional.empty();
        }
    }

    private Mono<ResponseEntity<Flux<DataBuffer>>> getFluxResponseEntity(HttpStatus httpStatus, String body) {
        DataBuffer dataBuffer =
                dataBufferFactory
                        .allocateBuffer()
                        .write(body//json? service to service?
                                .getBytes(StandardCharsets.UTF_8));
        return Mono.just(ResponseEntity.status(httpStatus).body(
                Flux.fromIterable(Collections.singletonList(dataBuffer))));
    }

    private boolean isOnlyDigits(String str) {
        return str.matches("[0-9]+");
    }

    private MediaType getContentType(Path path) {
        return MediaTypeFactory
                .getMediaType(path.toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}