package com.image.server;

import org.imgscalr.Scalr;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class DefaultImageResizing implements ImageResizing {
    private final Path basePath;
    private final Scheduler resizingScheduler;
    private final Random random = new Random();

    DefaultImageResizing(Path basePath, Scheduler resizingScheduler) {
        this.basePath = basePath;
        this.resizingScheduler = resizingScheduler;
    }

    //returns ResizedImageFilePath
    public Mono<Path> scale(Path imagePath, int x, int y) {
        return Mono
                .just(1)
                .subscribeOn(resizingScheduler)
                .flatMap(ignored -> {
                    File resizedFile = resizedFile(imagePath, x, y);
                    if (resizedFile.exists()) {
                        //already exists and finalized; assuming immutable images (are not updated)
                        return Mono.just(resizedFile.toPath());
                    }
                    //writing into a temp file the resized img and then rename it to final name_size.ext
                    String temporaryFilePath = resizedFile.getPath() + "." +random.nextLong() +".temporary";
                    File temporaryFile = new File(temporaryFilePath);
                    try {
                        //CAS like atomic operation; alternatively, FileLock but filesystem/platform dependent
                        boolean wasCreated = temporaryFile.createNewFile();
                        if (wasCreated) {
                            return Mono.just(resize(imagePath, x, y, resizedFile, temporaryFile));
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    //already existed; another thread is doing the resize for this image; poll asynchronously until done
                    return Flux
                            .interval(Duration.of(50, ChronoUnit.MILLIS))
                            .filter(ignore -> !new File(temporaryFilePath).exists())
                            .take(1)
                            .next()
                            .flatMap(a -> scale(imagePath, x, y));

                });
    }

    public File resizedFile(Path imagePath, int x, int y) {
        String fileName = imagePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf(".");
        String filenameWithoutExtension = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex + 1);
        String outputFileName = filenameWithoutExtension + "_" + x + "x" + y + "." + extension;
        return new File(basePath.resolve(outputFileName).toString());
    }

    private Path resize(Path imagePath, int x, int y, File resizedFile, File temporaryFile) {
        try {
            BufferedImage image = getImage(imagePath);
            BufferedImage bufferedImage = Scalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, x, y);
            String fileName = resizedFile.getName();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            ImageIO.write(bufferedImage, extension, temporaryFile);
            temporaryFile.renameTo(resizedFile);
            return resizedFile.toPath();
        } catch (IOException e) {
            delete(resizedFile);
            delete(temporaryFile);
            throw new RuntimeException(e);
        }
    }

    private BufferedImage getImage(Path filename) throws IOException {
        Path filePath = basePath.resolve(filename);
        Resource resource = new FileSystemResource(filePath);
        return ImageIO.read(resource.getInputStream());
    }

    private void delete(File file) {
        try {
            file.delete();
        } catch (Exception ignore) {
        }
    }
}
