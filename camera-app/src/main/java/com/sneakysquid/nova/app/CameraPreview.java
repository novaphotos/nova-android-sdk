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

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * @author Joe Walnes
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private final ErrorReporter errorReporter;

    private Camera camera;
    private boolean created;
    private boolean destroyed;

    public CameraPreview(Context context, ErrorReporter errorReporter) {
        super(context);
        this.errorReporter = errorReporter;
        getHolder().addCallback(this);
    }

    public void beginPreview(Camera camera) {
        this.camera = camera;
        if (created && !destroyed) {
            startPreview();
        }
    }

    public void endPreview() {
        if (created && !destroyed) {
            stopPreview();
        }
        camera = null;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview();
        created = true;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
        destroyed = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Handle resize/rotate.
        if (holder.getSurface() == null || camera == null) {
            return;
        }
        stopPreview();
        startPreview();
    }

    private void startPreview() {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(getHolder());
                camera.startPreview();
            } catch (IOException e) {
                errorReporter.reportError("Could not preview camera");
            }
        }
    }

    private void stopPreview() {
        if (camera != null) {
            camera.stopPreview();
        }
    }
}