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

import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;

import com.sneakysquid.nova.link.NovaFlashCommand;
import com.sneakysquid.nova.link.NovaLink;
import com.sneakysquid.nova.link.NovaLinkStatus;
import com.sneakysquid.nova.link.NovaLinkStatusCallback;
import com.sneakysquid.nova.link.android.AndroidBleNovaLink;

import static com.sneakysquid.nova.util.Debug.assertOnUiThread;
import static com.sneakysquid.nova.util.Debug.debug;

/**
 * @author Joe Walnes
 */
public class MainActivity extends Activity implements NovaLinkStatusCallback {

    protected ErrorReporter errorReporter;
    protected Camera camera;  // May be null
    protected CameraPreview preview;
    protected PhotoSaver photoSaver;
    protected NovaLink novaLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        debug("onCreate()");
        super.onCreate(savedInstanceState);

        errorReporter = new ModalErrorReporter();
        photoSaver = new PhotoSaver(errorReporter);
        preview = new CameraPreview(this, errorReporter);
        novaLink = new AndroidBleNovaLink(this);
        novaLink.registerStatusCallback(this);

        errorReporter.reportError("Application started");

        wireUpUi();
    }

    protected void wireUpUi() {
        setContentView(R.layout.activity_main);
        ((FrameLayout) findViewById(R.id.camera_preview)).addView(preview);

        (findViewById(R.id.button_capture)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCameraButtonClick();
            }
        });
    }

    @Override
    protected void onResume() {
        debug("onResume()");
        super.onResume();

        camera = Camera.open();
        if (camera == null) {
            errorReporter.reportError("Camera not found");
        } else {
            configureCamera(camera);
            preview.beginPreview(camera);
        }

        novaLink.enable();
    }

    @Override
    protected void onPause() {
        debug("onPause()");
        super.onPause();

        preview.endPreview();
        if (camera != null) {
            camera.release();
            camera = null;
        }

        novaLink.disable();
    }

    protected void configureCamera(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();

        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        camera.setParameters(parameters);
    }

    public void onCameraButtonClick() {
        debug("onCameraButtonClick()");
        if (camera == null) {
            errorReporter.reportError("Camera not found");
        } else {
            NovaFlashCommand flashCmd = flashCommand();
            // Take photo
            Runnable takePhoto = takePhotoCommand(flashCmd, new TakePhoto.Callback() {
                @Override
                public void onPhotoTaken(byte[] jpeg) {
                    debug("onPhotoTaken()");
                    assertOnUiThread();

                    // When finished...
                    // Save
                    photoSaver.save(jpeg);
                    // Resume preview
                    if (camera != null) {
                        preview.beginPreview(camera);
                    }
                }
            });
            takePhoto.run();
        }
    }

    protected NovaFlashCommand flashCommand() {
        return new NovaFlashCommand(255, 255, 500);  // TODO: Values from UI
    }

    protected TakePhoto takePhotoCommand(NovaFlashCommand flashCmd, TakePhoto.Callback result) {
        return new TakePhoto(this, camera, novaLink, flashCmd, result);
    }

    @Override
    public void onNovaLinkStatusChange(NovaLinkStatus status) {

    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }

}
