/*
 * Copyright (C) 2013-2014 Sneaky Squid LLC.
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

package com.sneakysquid.nova.link;

/**
 * Interface to Nova flash device. This takes care of the BluetoothLE communication details
 * with Nova.
 * <p/>
 * <h2>Usage:</h2>
 * <pre>
 *
 * // Initialization. One instance per app.
 * NovaLink nova = new BluetoothLENovaLink();  // For Android 4.3 onwards
 *
 * // Notify user when Nova connection changes state. See NovaLinkStatus for these states.
 * nova.registerStatusCallback(new NovaLinkStatusCallback() {
 *     void onNovaLinkStatusChange(NovaLinkStatus status) {
 *         if (status == NovaLinkStatus.Ready) {
 *             // enable camera
 *         } else {
 *             // disable camera
 *         }
 *     }
 *   }
 * });
 *
 * // When application wakes up (e.g. onResume()), call this to start searching for Nova devices.
 * nova.enable();
 *
 * // Sometime later, when user wants to take a photo.
 * if (nova.getStatus() == NovaLinkStatus.Ready) {
 *
 *     // Flash brightness/color/time settings.
 *     NovaFlashCommand flashCmd = NovaFlashCommand.gentle();
 *
 *     // Perform flash. Because there's wireless communication involved, this isn't immediate
 *     // so this method is asynchronous, and you pass a callback to get notification of result.
 *     nova.beginFlash(flashCmd, new NovaCompletionCallback() {
 *       void onNovaFlashAcknowledged(boolean successful) {
 *           if (successful) {
 *               // flash activated: trigger camera shutter.
 *
 *               // after camera has taken photo
 *               nova.endFlash();
 *           } else {
 *               // show error to user
 *           }
 *       }
 *     });
 * }
 *
 * // When the flash is no longer needed (e.g. application goes into background with onPause()),
 * // disable the flash. This conserves battery life of both the flash and the phone.
 * nova.disable();
 *
 * <h2>Callbacks and threading</h2>
 * <p>NovaLink is designed to be used on a single thread only. All the callbacks will also be
 * dispatched on the same thread. On Android this is the main/UI thread.</p>
 * </pre>
 *
 * @author Joe Walnes
 */
public interface NovaLink {

    /**
     * Enable Nova. This will activate BluetoothLE, begin scanning and attempt to connect.
     */
    void enable();

    /**
     * Disable Nova. Disconnects (if connected) and stops scanning. Conserves battery life.
     */
    void disable();

    /**
     * Forgets any currently paired devices and performs a rescan.
     */
    void refresh();

    /**
     * Tell Nova to begin the flash.
     * <p/>
     * Warmness/coolness/duration settings are set in the supplied {@link NovaFlashCommand}.
     * <p/>
     * Because there's wireless communication involved, this isn't immediate so this method is
     * asynchronous, and you pass a callback to get notification of result. Only when the callback
     * is triggered should you activate the camera shutter.
     * <p/>
     * The callback will occur on the main/UI thread. The callback may be null.
     * <p/>
     * The flash will remain lit until endFlash() is called, or the duration (in NovaFlashCommand)
     * expires, or the BluetoothLE connection is terminated.
     *
     * @see NovaFlashCommand
     * @see #endFlash(NovaCompletionCallback)
     * @see NovaCompletionCallback
     */
    void beginFlash(NovaFlashCommand flashCmd, NovaCompletionCallback callback);

    /**
     * @see #beginFlash(NovaFlashCommand, NovaCompletionCallback)
     */
    void beginFlash(NovaFlashCommand flashCmd);

    /**
     * Tell Nova to end a flash that was previously started with beginFlash(). The flash
     * will end eventually, even without this call due to the duration specified when calling
     * begin flash. This is a way to terminate the flash early to conserve power.
     *
     * @see #beginFlash(NovaFlashCommand, NovaCompletionCallback)
     */
    void endFlash(NovaCompletionCallback callback);

    /**
     * @see #endFlash(NovaCompletionCallback)
     */
    void endFlash();

    /**
     * Ping the Nova device. Can be useful for diagnostics.
     */
    void ping(NovaCompletionCallback callback);

    /**
     * Gets current status of Nova connection.
     *
     * @see NovaLinkStatus
     * @see NovaLinkStatusCallback
     * @see #registerStatusCallback(NovaLinkStatusCallback)
     * @see #unregisterStatusCallback(NovaLinkStatusCallback)
     */
    NovaLinkStatus getStatus();

    /**
     * Register callback for notifiaction when the {@link NovaLinkStatus} changes.
     * <p/>
     * Callback will be run on the main/UI thread.
     *
     * @see #getStatus()
     * @see #unregisterStatusCallback(NovaLinkStatusCallback)
     * @see NovaLinkStatus
     * @see NovaLinkStatusCallback
     */
    void registerStatusCallback(NovaLinkStatusCallback callback);

    /**
     * Unregisters previously registered callback.
     *
     * @see #registerStatusCallback(NovaLinkStatusCallback)
     */
    void unregisterStatusCallback(NovaLinkStatusCallback callback);

}
