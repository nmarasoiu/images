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

public class DefaultImageResizing implements ImageResizing {
    private final Path basePath;

    DefaultImageResizing(Path basePath) {
        this.basePath = basePath;
    }

    //returns ResizedImageFilePath
    public Path scale(Path imagePath, int x, int y) {
        File resizedFile = resizedFile(imagePath, x, y);
        if (resizedFile.exists()) {
            //already exists and finalized; assuming immutable images (are not updated)
            return resizedFile.toPath();
        }
        //writing into a temp file the resized img and then rename it to final name_size.ext
        File temporaryFile = new File(resizedFile.getPath() + ".temporary");
        try {
            if(temporaryFile.exists()) {
                //another writer is generating resized image; wait for it and retry
                while (temporaryFile.exists()) {
                    Thread.sleep(300);
                }
                return scale(imagePath, x, y);
            }
            BufferedImage image = getImage(imagePath);
            //async.get makes sense because a limited thread pool is used in AsyncScalr
            BufferedImage bufferedImage =
                    AsyncScalr.resize(image, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, x, y).get();
//            temporaryFile.createNewFile();//should not be necessary
            String fileName = resizedFile.getName();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            ImageIO.write(bufferedImage, extension, temporaryFile);
            temporaryFile.renameTo(resizedFile);
            return resizedFile.toPath();
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                temporaryFile.delete();
            } catch (Exception ignore) {
            }
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
