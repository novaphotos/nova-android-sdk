Nova Android SDK
================

Official Android SDK for [Nova](https://wantnova.com). 

For more background see the [Nova SDK page](https://wantnova.com/sdk/).

This library hides the complexities of the BluetoothLE discovery and communication, making
it simple to interface with a Nova device to add flash capabilities to your application.

This repository contains two projects:

*   `library`: The core API for interacting with Nova. This is designed to be embedded in
    other camera apps.
*   `camera-app`: The standalone Nova camera app. This is a reference application for the library.

Usage
-----

	// Initialization. One instance per app.
    NovaLink nova = new AndroidBleNovaLink();  // For Android 4.3 onwards

    // Notify user when Nova connection changes state. See NovaLinkStatus for these states.
    nova.registerStatusCallback(new NovaLinkStatusCallback() {
        void onNovaLinkStatusChange(NovaLinkStatus status) {
            if (status == NovaLinkStatus.Ready) {
                // enable camera
            } else {
                // disable camera
            }
        }
      }
    });

    // When application wakes up (e.g. onResume()), call this to start searching for Nova devices.
    nova.enable();

    // Sometime later, when user wants to take a photo.
    if (nova.getStatus() == NovaLinkStatus.Ready) {

        // Flash brightness/color/time settings.
        NovaFlashCommand flashCmd = new NovaFlashCommand();
        flashCmd.setWarmness(255); // 0 (off) to 255 (full power)
        flashCmd.setCoolness(20);  // 0 (off) to 255 (full power)
        flashCmd.setDuration(200); // Milliseconds. Max 65535

        // Perform flash. Because there's wireless communication involved, this isn't immediate
        // so this method is asynchronous, and you pass a callback to get notification of result.
        nova.flash(flashCmd, new NovaFlashCallback() {
          void onNovaFlashAcknowledged(boolean successful) {
              if (successful) {
                  // flash activated: trigger camera shutter
              } else {
                  // show error to user
              }
          }
        });
    }

    // When the flash is no longer needed (e.g. application goes into background with onPause()),
    // disable the flash. This conserves battery life of both the flash and the phone.
    nova.disable();


Callbacks and threading
-----------------------

`NovaLink` is designed to be used on a single thread only. All the callbacks will also be
dispatched on the same thread. On Android this is the main/UI thread.


Including in your app
---------------------

There are two ways you can use the library in your own application.

1.  Copy source code. Simply copy the source code in the `library` project in to your
    own project. Because it has no external dependencies, this is the least hassle
    and it avoid you having to understand the build tools.

2.  Build into Android `.aar` library. Run `make`.


License
-------

    Copyright 2013 Sneaky Squid LLC.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
