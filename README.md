Nova Android SDK
================

Official Android SDK for [Nova](https://wantnova.com). 

For more background see the [Nova SDK page](https://wantnova.com/sdk/).

This library hides the complexities of the BluetoothLE discovery and communication, making
it simple to interface with a Nova device to add flash capabilities to your application.

This repository contains two projects:

*   `nova-sdk`: The core SDK for interacting with Nova. This is designed to be embedded in
    other camera apps.
*   `nova-testapp`: A standalone Android app for manually invoking the SDK and viewing the
    result. You can use this to trigger the flash.

Requirements
------------

Nova uses Bluetooth Low Energy which is only available in Andriod 4.3 (API level 18) and
onwards.


Usage
-----

    // Initialization. One instance per app.
    NovaLink nova = new BluetoothLeNovaLink();

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
        NovaFlashCommand flashCmd = NovaFlashCommand.warm();

        // Perform flash. Because there's wireless communication involved, this isn't immediate
        // so this method is asynchronous, and you pass a callback to get notification of result.
        nova.beginFlash(flashCmd, new NovaCompletionCallback() {
          void onComplete(boolean successful) {
              if (successful) {
                  // flash activated: trigger camera shutter

                  // after camera has taken photo
                  nova.endFlash();
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

`NovaLink` should only ever be called from the Android main UI thread. All callbacks will
also be invoked on this thread.

License
-------

    Copyright 2013-2014 Sneaky Squid LLC.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
