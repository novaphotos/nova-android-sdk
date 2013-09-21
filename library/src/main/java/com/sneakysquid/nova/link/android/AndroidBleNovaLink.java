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

package com.sneakysquid.nova.link.android;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;

import com.sneakysquid.nova.link.NovaFlashCallback;
import com.sneakysquid.nova.link.NovaFlashCommand;
import com.sneakysquid.nova.link.NovaLink;
import com.sneakysquid.nova.link.NovaLinkStatus;
import com.sneakysquid.nova.link.NovaLinkStatusCallback;

import java.util.HashSet;
import java.util.Set;

import static android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;
import static android.content.Context.BLUETOOTH_SERVICE;
import static com.sneakysquid.nova.util.Debug.assertOnUiThread;
import static com.sneakysquid.nova.util.Debug.debug;

/**
 * Implementation of {@link com.sneakysquid.nova.link.NovaLink} backed by Android BluetoothLE
 * APIs, available in JellyBean 4.3 (API level 18) and onwards.
 *
 * @author Joe Walnes
 * @see com.sneakysquid.nova.link.NovaLink
 */
public class AndroidBleNovaLink extends BluetoothGattCallback implements NovaLink {

    private final BluetoothAdapter bluetoothAdapter;
    private final Activity activity;
    private final Set<NovaLinkStatusCallback> linkStatusCallbacks = new HashSet<NovaLinkStatusCallback>();

    private NovaLinkStatus status;
    private BluetoothGatt connected;
    private BluetoothAdapter.LeScanCallback currentScan;
    private BluetoothGattCharacteristic characteristic;
    private NovaFlashCallback flashCallback;

    /**
     * @param activity Main Android Activity for this app.
     */
    public AndroidBleNovaLink(Activity activity) {
        this.activity = activity;
        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        status = NovaLinkStatus.Disconnected;
    }

    /**
     * @see NovaLink#getStatus()
     */
    @Override
    public NovaLinkStatus getStatus() {
        return status;
    }

    /**
     * @see NovaLink#registerStatusCallback(NovaLinkStatusCallback)
     */
    @Override
    public void registerStatusCallback(NovaLinkStatusCallback callback) {
        synchronized (linkStatusCallbacks) {
            linkStatusCallbacks.add(callback);
        }
    }

    /**
     * @see NovaLink#unregisterStatusCallback(NovaLinkStatusCallback)
     */
    @Override
    public void unregisterStatusCallback(NovaLinkStatusCallback callback) {
        synchronized (linkStatusCallbacks) {
            linkStatusCallbacks.remove(callback);
        }
    }

    /**
     * @see NovaLink#disable()
     */
    @Override
    public void disable() {
        assertOnUiThread(activity);
        debug("disable()");
    }

    /**
     * @see NovaLink#enable()
     */
    @Override
    public void enable() {
        assertOnUiThread(activity);
        debug("enable()");
        scan();
    }

    public void scan() {
        assertOnUiThread(activity);
        debug("scan()");

        updateLinkStatus(NovaLinkStatus.Scanning);

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            updateLinkStatus(NovaLinkStatus.Disconnected);
            debug("bluetooth not available");
            activity.startActivity(new Intent(ACTION_REQUEST_ENABLE));
            // TODO: Rescan once enabled
            return;
        }

        // TODO: Timeout

        if (currentScan != null) {
            debug("stopping existing scan");
            bluetoothAdapter.stopLeScan(currentScan);
        }
        currentScan = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onScannedDevice(device, rssi, scanRecord);
                    }
                });
            }
        };
        debug("starting new scan");
        if (!bluetoothAdapter.startLeScan(currentScan)) {
            debug("scan failed to start");
            updateLinkStatus(NovaLinkStatus.Disconnected);
            bluetoothAdapter.stopLeScan(currentScan);
            currentScan = null;
        }
    }

    protected void onScannedDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        assertOnUiThread(activity);
        debug("onScannedDevice(%s,%s,%s,%s)", device, device.getName(), rssi, scanRecord);

        if (status != NovaLinkStatus.Scanning) {
            debug("ignoring device because we've already found another");
            return;
        }

        if (isNova(device)) {
            debug("scanned Nova");

            if (currentScan != null) {
                debug("stop scanning");
                bluetoothAdapter.stopLeScan(currentScan);
                currentScan = null;
            }
            if (this.connected != null) {
                debug("closing existing connection");
                this.connected.disconnect();
                this.connected.close();
                this.connected = null;
            }

            updateLinkStatus(NovaLinkStatus.Connecting);
            this.connected = device.connectGatt(activity, false /* first connect false, subsequent true */, this);
        }
    }

    protected boolean isNova(BluetoothDevice device) {
        String name = device.getName();
        return name != null && (name.equals("S-Power")
                || name.equals("Noon")
                || name.equals("Nova"));
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        assertOnUiThread(activity);
        debug("onConnectionStateChange()");

        if (status != BluetoothGatt.GATT_SUCCESS) {
            debug("failed to connect");
            updateLinkStatus(NovaLinkStatus.Disconnected);
        } else if (newState == STATE_CONNECTED) {
            updateLinkStatus(NovaLinkStatus.Handshaking);
            gatt.discoverServices();
        } else if (newState == STATE_DISCONNECTED) {
            updateLinkStatus(NovaLinkStatus.Disconnected);
        } else {
            throw new IllegalArgumentException("Unexpected state: " + newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        assertOnUiThread(activity);
        debug("onServicesDiscovered()");

        if (status != BluetoothGatt.GATT_SUCCESS) {
            debug("failed to discover services");
            updateLinkStatus(NovaLinkStatus.Disconnected);
        } else {
            boolean found = false;
            for (BluetoothGattService service : gatt.getServices()) {
                debug("service: %s", service.getUuid());
                if (service.getUuid().toString().startsWith("0000fff0-")) {
                    debug("found Nova service");
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        debug("characteristic: %s", characteristic.getUuid());
                        if (characteristic.getUuid().toString().startsWith("0000fff3-")) {
                            debug("found Nova characteristic");
                            found = true;
                            this.characteristic = characteristic;
                        }
                    }
                }
            }
            updateLinkStatus(found ? NovaLinkStatus.Ready : NovaLinkStatus.Disconnected);
        }
    }

    @Override
    public void flash(NovaFlashCommand flashCmd, NovaFlashCallback callback) {
        assertOnUiThread(activity);
        debug("flash(%s)", flashCmd);

        if (status != NovaLinkStatus.Ready) {
            debug("Not ready: " + status);
            callback.onNovaFlashAcknowledged(false);
            return;
        }

        updateLinkStatus(NovaLinkStatus.Busy);

        characteristic.setValue(flashCmd.toPacket());
        if (!connected.writeCharacteristic(characteristic)) {
            debug("Failed to write value");
            connected.abortReliableWrite(connected.getDevice());
            callback.onNovaFlashAcknowledged(false);
            return;
        }

        debug("written characterestic: %s", characteristic);
        this.flashCallback = callback;
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        assertOnUiThread(activity);
        debug("onReliableWriteCompleted(%s,%s)", gatt, status);

        if (flashCallback != null) {
            flashCallback.onNovaFlashAcknowledged(status == BluetoothGatt.GATT_SUCCESS);
            flashCallback = null;
        }

        updateLinkStatus(NovaLinkStatus.Ready);
    }

    protected void updateLinkStatus(NovaLinkStatus status) {
        assertOnUiThread(activity);
        this.status = status;
        debug("linkStatus = %s", status);
        synchronized (linkStatusCallbacks) {
            for (NovaLinkStatusCallback callback : linkStatusCallbacks) {
                callback.onNovaLinkStatusChange(status);
            }
        }
    }

}
