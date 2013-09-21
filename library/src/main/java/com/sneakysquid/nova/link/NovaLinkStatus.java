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

package com.sneakysquid.nova.link;

/**
 * States of Nova link.
 *
 * @see NovaLink#getStatus()
 * @see NovaFlashCallback
 *
 * @author Joe Walnes
 */
public enum NovaLinkStatus {

    /**
     * BluetoothLE is not available on this device. This could be because the device does
     * not support it, or the user has not enabled it in their settings.
     */
    Disabled,

    /**
     * No devices are connected and nothing is being scanned for.
     */
    Disconnected,

    /**
     * No devices are connected, but we're currently scanning for one.
     */
    Scanning,

    /**
     * Device found and attempting to establish a connection.
     */
    Connecting,

    /**
     * BluetoothLE connection to device has been established. Performing final handshake
     * to prepare device for flashes.
     */
    Handshaking,

    /**
     * Connection to device is ready and waiting for flashes. Yay.
     */
    Ready,

    /**
     * Connection to device is established, but the device is currently busy
     * (most likely performing a flash).
     */
    Busy

}
