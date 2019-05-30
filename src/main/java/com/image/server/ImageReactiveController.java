package com.image.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@RestController
public class ImageReactiveController {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final FileRepository fileRepository;
    private final ImageResizing imageResizing;
    private final DataBufferFactory dataBufferFactory;

    public ImageReactiveController(FileRepository fileRepository, ImageResizing imageResizing, DataBufferFactory dataBufferFactory) {
        this.fileRepository = fileRepository;
        this.imageResizing = imageResizing;
        this.dataBufferFactory = dataBufferFactory;
    }

    @GetMapping(path = "/image/{path}")
    public ResponseEntity<Flux<DataBuffer>> streamImage(@PathVariable Path path,
                                                        @RequestParam(required = false) String size) {

        if (size != null) {
            Optional<int[]> sizeXYOpt = parseSize(size);
            if (sizeXYOpt.isPresent()) {
                int[] xy = sizeXYOpt.get();
                int x = xy[0];
                int y = xy[1];
                File resizedFile = imageResizing.resizedFile(path, x, y);
                File resizedFileComplete = new File(resizedFile.getPath()+".ok");
                if(resizedFileComplete.exists()){
                    return streamFileAsFluxResponseEntity(resizedFile.toPath());
                }
                Path scaledPath = imageResizing.scale(path, x, y);
                return streamFileAsFluxResponseEntity(scaledPath);
            } else {
                return badReqEntity();
            }
        }
        return streamFileAsFluxResponseEntity(path);
    }

    private ResponseEntity<Flux<DataBuffer>> streamFileAsFluxResponseEntity(Path path) {
        Flux<DataBuffer> imageStream = fileRepository.readImageFromDisk(path);
        return getBody(path, imageStream);
    }

    private ResponseEntity<Flux<DataBuffer>> getBody(Path path, Flux<DataBuffer> imageStream) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(getContentType(path))
                .body(imageStream);
    }

    private Optional<int[]> parseSize(@RequestParam(required = false) String size) {
        String[] xy = size.trim().toLowerCase().split("x");
        if (xy.length == 2 && isOnlyDigits(xy[0]) && isOnlyDigits(xy[1])) {
            return Optional.of(Arrays.stream(xy).mapToInt(Integer::parseInt).toArray());
        } else {
            return Optional.empty();
        }
    }

    private ResponseEntity<Flux<DataBuffer>> badReqEntity() {
        DataBuffer dataBuffer =
                dataBufferFactory
                        .allocateBuffer()
                        .write("The size query param should be like 300x400"
                                .getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                Flux.fromIterable(Collections.singletonList(dataBuffer)));
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