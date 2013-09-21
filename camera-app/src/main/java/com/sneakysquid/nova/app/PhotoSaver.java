package com.sneakysquid.nova.app;

import android.hardware.Camera;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.sneakysquid.nova.util.Debug.debug;

/**
 * @author Joe Walnes
 */
public class PhotoSaver implements Camera.PictureCallback {

    protected final File dir;
    protected final String suffix;
    protected final String prefix;
    protected final ErrorReporter errorReporter;

    public PhotoSaver(ErrorReporter errorReporter) {
        this(errorReporter,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "IMG_",
                ".jpg");
    }

    public PhotoSaver(ErrorReporter errorReporter, File dir, String prefix, String suffix) {
        this.errorReporter = errorReporter;
        this.dir = dir;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        File file = uniqueFile();
        debug("onPictureTaken() saving to %s", file);

        if (!writeToFile(file, data)) {
            errorReporter.reportError("Could not save photo");
        }
    }

    protected File uniqueFile() {
        return new File(dir, prefix + generateId() + suffix).getAbsoluteFile();
    }

    protected String generateId() {
        Date now = new Date();
        long millis = now.getTime() - now.getTime() % 1000;
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(now) + "_" + millis;
    }

    protected boolean writeToFile(File file, byte[] data) {
        try {
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            return true;
        } catch (IOException e) {
            debug("failed to write to file " + file.getAbsolutePath() + ": " + e);
            return false;
        }
    }
}
