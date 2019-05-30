package com.image.server;

import org.imgscalr.AsyncScalr;
import org.imgscalr.Scalr;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DefaultImageResizing implements ImageResizing {
    private final Path basePath;

    DefaultImageResizing(Path basePath) {
        this.basePath = basePath;
    }

    //returns ResizedImageFilePath
    public Path scale(Path imagePath, int x, int y) {
        try {
            BufferedImage image = getImage(imagePath);
            Future<BufferedImage> resizedImageFuture = AsyncScalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, x, y);
            BufferedImage bufferedImage = resizedImageFuture.get();
            File outputFile = resizedFile(imagePath, x, y);
            if (!outputFile.exists()) {
                outputFile.createNewFile();//should not be necessary
            }
            String fileName = outputFile.getName();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            if(ImageIO.write(bufferedImage, extension, outputFile)){
                new File(outputFile.getPath()+".ok").createNewFile();
            }
            return Paths.get(outputFile.toURI());
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File resizedFile(Path imagePath, int x, int y) {
        String fileName = imagePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf(".");
        String filenameWithoutExtension = fileName.substring(0, dotIndex);
        String extension = fileName.substring(dotIndex + 1);
        String outputFileName = filenameWithoutExtension + "_" + x + "x" + y + "." + extension;
        return new File(basePath.resolve(outputFileName).toString());
    }


    private BufferedImage getImage(Path filename) throws IOException {
        Path filePath = basePath.resolve(filename);
        Resource resource = new FileSystemResource(filePath);
        return ImageIO.read(resource.getInputStream());
    }
}
