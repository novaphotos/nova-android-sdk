/*
 * Copyright (C) 2013 Sneaky Squid LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
public class PhotoSaver {

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

    public void save(byte[] jpeg) {
        File file = uniqueFile();
        debug("onPictureTaken() saving to %s", file);

        if (!writeToFile(file, jpeg)) {
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
