package com.image.server;

import java.io.File;
import java.nio.file.Path;

public interface ImageResizing {
    //returns ResizedImageFilePath
    Path scale(Path imagePath, int x, int y);

    File resizedFile(Path imagePath, int x, int y);
}