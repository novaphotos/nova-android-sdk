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

import com.sneakysquid.nova.link.NovaFlashCallback;
import com.sneakysquid.nova.link.NovaFlashCommand;
import com.sneakysquid.nova.link.NovaLinkStatus;
import com.sneakysquid.nova.link.NovaLinkStatusCallback;
import com.sneakysquid.nova.link.android.AndroidBleNovaLink;

import static com.sneakysquid.nova.util.Debug.debug;

public class MainActivity extends Activity implements Camera.ShutterCallback, NovaLinkStatusCallback {

    private ErrorReporter errorReporter;
    private Camera camera;  // May be null
    private CameraPreview preview;
    private Camera.PictureCallback photoSaver;
    private AndroidBleNovaLink novaLink;

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
            preview.beginPreview(camera);
        }

        novaLink.enable();
        novaLink.scan();
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

    public void onCameraButtonClick() {
        debug("onCameraButtonClick()");
        if (camera == null) {
            errorReporter.reportError("Camera not found");
        } else {
            NovaFlashCommand flashCmd = new NovaFlashCommand(255, 255, 500); // TODO
            novaLink.flash(flashCmd, new NovaFlashCallback() {
                @Override
                public void onNovaFlashAcknowledged(boolean successful) {
                    debug(successful ? "flash SUCCESS" : "flash FAIL"); // TODO
                }
            });

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        // TODO
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            debug("take picture");
                            camera.takePicture(MainActivity.this, null, photoSaver);
                        }
                    });
                }
            }).start();

        }
    }

    @Override
    public void onShutter() {
        debug("onShutter()");

        // Resume preview
        if (camera != null) {
            preview.beginPreview(camera);
        }
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
