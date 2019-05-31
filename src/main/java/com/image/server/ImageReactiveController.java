package com.image.server;

import com.sun.el.lang.ELArithmetic;
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
import java.util.function.Supplier;
import java.util.regex.Pattern;

@RestController
public class ImageReactiveController {
    private final Pattern digitsPattern=Pattern.compile("[0-9]+");
    private final ImageResizing imageResizing;
    private final FileRepository fileRepository;
    private final DataBufferFactory dataBufferFactory;
    private final BlockingExecution blockingExecution;
    private final int maxSizeForResize;
    private final int minFreeBytesForResize;

    public ImageReactiveController(FileRepository fileRepository, ImageResizing imageResizing,
                                   DataBufferFactory dataBufferFactory, BlockingExecution blockingExecution,
                                   @Value("${maxSizeForResize:9999}") int maxSizeForResize,
                                   @Value("${minFreeBytesForResize:734217728}") int minFreeBytesForResize) {
        this.fileRepository = fileRepository;
        this.imageResizing = imageResizing;
        this.dataBufferFactory = dataBufferFactory;
        this.blockingExecution = blockingExecution;
        this.maxSizeForResize = maxSizeForResize;
        this.minFreeBytesForResize = minFreeBytesForResize;
    }

    @GetMapping(path = "/image/{path}")
    public Mono<ResponseEntity<Flux<DataBuffer>>> streamImage(@PathVariable(name = "path") Path imageRelPath,
                                                              @RequestParam(required = false) String size) {
        return blockingExecution.scheduleBlockingMono(()-> {
            if (!fileRepository.getResource(imageRelPath).exists()) {
                return getFluxResponseEntity(HttpStatus.NOT_FOUND, "The image was not found");
            }
            if (size == null) {
                return streamFileAsFluxResponseEntity(Mono.just(imageRelPath));
            }
            Optional<int[]> sizeXYOpt = parseSize(size);
            if (!sizeXYOpt.isPresent()) {
                return (getFluxResponseEntity(HttpStatus.BAD_REQUEST,
                        "The size query param should be like 300x400 and max(w,h)<" + maxSizeForResize));
            }
            int[] xy = sizeXYOpt.get();
            int x = xy[0];
            int y = xy[1];
            if (Math.max(x, y) >= maxSizeForResize) {
                return (getFluxResponseEntity(HttpStatus.BAD_REQUEST,
                        "The size query param should be like 300x400 and max(w,h)<" + maxSizeForResize));
            }
            File resizedFile = imageResizing.resizedFile(imageRelPath, x, y);
            if (resizedFile.exists()) {
                return streamFileAsFluxResponseEntity(Mono.fromSupplier(resizedFile::toPath));
            }
            if (isEnoughMemory()) {
                Mono<Path> scaledPathFlux = imageResizing.scale(imageRelPath, x, y);
                return streamFileAsFluxResponseEntity(scaledPathFlux);
            } else {
                return (getFluxResponseEntity(HttpStatus.SERVICE_UNAVAILABLE,
                        "The server does not have enough resources to do image resizing at the moment, please retry in a few minutes"));
            }
        });
    }

    private boolean isEnoughMemory() {
        Supplier<Boolean> isItEnough = () -> Runtime.getRuntime().freeMemory() > minFreeBytesForResize;
        if (isItEnough.get()) {
            return true;
        } else {
            System.gc();
            return isItEnough.get();
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
        return digitsPattern.matcher(str).matches();
    }

    private MediaType getContentType(Path path) {
        return MediaTypeFactory
                .getMediaType(path.toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}