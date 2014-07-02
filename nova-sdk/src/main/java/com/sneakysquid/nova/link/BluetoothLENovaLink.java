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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.BLUETOOTH_SERVICE;
import static com.sneakysquid.nova.link.Debug.assertOnUiThread;
import static com.sneakysquid.nova.link.Debug.debug;

/**
 * Implementation of {@link com.sneakysquid.nova.link.NovaLink} backed by Android BluetoothLE
 * APIs, available in JellyBean 4.3 (API level 18) and onwards.
 *
 * @author Joe Walnes
 * @see com.sneakysquid.nova.link.NovaLink
 */
public class BluetoothLENovaLink implements NovaLink {

    private static class Cmd {
        int requestId;
        String msg;
        NovaCompletionCallback callback;
    }

    private static final int SCAN_INTERVAL = 1000; // How long between scans, in millis.
    private static final int SCAN_DURATION = 500; // How long to scan for, in millis.
    private static final int ACK_TIMEOUT = 2000; // How long before we give up waiting for ack from device, in millis.

    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final int PARSE_FAILED = -1;

    private final Activity activity;
    private final Set<NovaLinkStatusCallback> linkStatusCallbacks = new HashSet<NovaLinkStatusCallback>();

    private boolean enabled = false;
    private int nextRequestId = 0;
    private NovaLinkStatus status = NovaLinkStatus.Disabled;
    private final LinkedList<Cmd> awaitingSend = new LinkedList<Cmd>();
    private Cmd awaitingAck = null;
    private final AtomicBoolean startScanTimerAllow = new AtomicBoolean();
    private final AtomicBoolean stopScanTimerAllow = new AtomicBoolean();
    private final AtomicBoolean ackTimerAllow = new AtomicBoolean();
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothAdapter.LeScanCallback currentScan;
    private int strongestSignalRSSI;
    private BluetoothDevice strongestSignalDevice;
    private BluetoothDevice activeDevice;
    private BluetoothGatt activeGatt;
    private BluetoothGattCharacteristic requestCharacteristic;
    private BluetoothGattCharacteristic responseCharacteristic;

    /**
     * @param activity Main Android Activity for this app.
     */
    public BluetoothLENovaLink(Activity activity) {
        this.activity = activity;
    }

    /**
     * @see NovaLink#getStatus()
     */
    @Override
    public NovaLinkStatus getStatus() {
        return status;
    }

    private void setStatus(NovaLinkStatus newStatus) {
        debug("status = " + newStatus);
        if (newStatus != status) {
            status = newStatus;
            synchronized (linkStatusCallbacks) {
                for (NovaLinkStatusCallback linkStatusCallback : linkStatusCallbacks) {
                    linkStatusCallback.onNovaLinkStatusChange(newStatus);
                }
            }
        }
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
     * @see NovaLink#enable()
     */
    @Override
    public void enable() {
        assertOnUiThread();
        debug("enable()");

        if (enabled) {
            return;
        }
        enabled = true;

        setStatus(NovaLinkStatus.Idle);

        startScanTimerAllow.set(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (startScanTimerAllow.get() /* don't reset, want to repeat */) {
                    startScan();
                    new Handler().postDelayed(this, SCAN_INTERVAL); // Repeat
                }
            }
        }, SCAN_INTERVAL);

        startScan();
    }

    /**
     * @see NovaLink#disable()
     */
    @Override
    public void disable() {
        assertOnUiThread();
        debug("disable()");

        if (!enabled) {
            return;
        }
        enabled = false;

        disconnect();
        stopScan();

        startScanTimerAllow.set(false);
        stopScanTimerAllow.set(false);
        ackTimerAllow.set(false);

        setStatus(NovaLinkStatus.Disabled);
    }

    @Override
    public void refresh() {
        assertOnUiThread();
        if (enabled) {
            disable();
            enable();
        }
    }


    // ---------------------
    // Scan for Nova devices
    // ---------------------

    /**
     * Start scanning. Periodically called by timer.
     */
    void startScan() {
        assertOnUiThread();

        debug("start scan");

        if (stopScanTimerAllow.get()) {
            return; // Scan is already in progress.
        }

        BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = (bluetoothManager == null) ? null : bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            debug("bluetooth not enabled");
            setStatus(NovaLinkStatus.Disabled);
            activity.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return;
        }

        if (status != NovaLinkStatus.Idle) {
            return; // Either BT is disabled, or we're already attempting to connect.
        }

        strongestSignalDevice = null;
        strongestSignalRSSI = 0;

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

        if (!bluetoothAdapter.startLeScan(currentScan)) {
            debug("scan failed to start");
            return;
        }

        setStatus(NovaLinkStatus.Scanning);

        // Stop scanning after SCAN_DURATION.
        stopScanTimerAllow.set(true);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (stopScanTimerAllow.getAndSet(false)) {
                    stopScan();
                }
            }
        }, SCAN_DURATION);
    }

    private void onScannedDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        assertOnUiThread();

        if (!isNova(device)) {
            debug("onScannedDevice() IGNORE: " + deviceDetails(device));
            return;
        }
        debug("onScannedDevice() NOVA: " + deviceDetails(device));

        // TODO: Support pairing to multiple devices

        // If this device has a stronger signal than previously scanned devices, it's our best bet.
        if (strongestSignalDevice == null || rssi > strongestSignalRSSI) {
            strongestSignalDevice = device;
            strongestSignalRSSI = rssi;
        }
    }

    /**
     * Stop scanning. Periodically called by timer, sometime after startScan().
     */
    void stopScan() {
        if (currentScan != null) {
            bluetoothAdapter.stopLeScan(currentScan);
        }
        currentScan = null;

        BluetoothDevice device = strongestSignalDevice;
        strongestSignalDevice = null;
        strongestSignalRSSI = 0;

        if (device == null) {
            setStatus(NovaLinkStatus.Idle);
        } else {
            connect(device);
        }
    }

    private boolean isNova(BluetoothDevice device) {
        String name = device.getName();
        return name != null && name.equals("Nova");
    }


    // ------------------------------
    // Establish connection to device
    // ------------------------------

    private void connect(BluetoothDevice device) {
        debug("connect() " + deviceDetails(device));

        // Connects to the discovered device
        activeDevice = device;

        // These callbacks are generated by an internal Bluetooth thread.
        // Before we do anything we need to thunk back to the main thread.
        activeGatt = device.connectGatt(activity, false /* first connect false, subsequent true */,
                new BluetoothGattCallback() {

                    @Override
                    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothLENovaLink.this.onConnectionStateChange(gatt, status, newState);
                            }
                        });
                    }

                    @Override
                    public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothLENovaLink.this.onServicesDiscovered(gatt, status);
                            }
                        });
                    }

                    @Override
                    public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothLENovaLink.this.onCharacteristicChanged(gatt, characteristic);
                            }
                        });
                    }

                    @Override
                    public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                BluetoothLENovaLink.this.onCharacteristicWrite(gatt, characteristic, status);
                            }
                        });
                    }

                }
        );

        setStatus(NovaLinkStatus.Connecting);
    }

    private void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        assertOnUiThread();

        if (gatt != activeGatt) {
            return;
        }

        debug("onConnectionStateChange()");

        if (status != BluetoothGatt.GATT_SUCCESS) {
            debug("failed to connect");
            disconnect();
        } else if (newState == BluetoothProfile.STATE_CONNECTED) {
            debug("connected to " + deviceDetails(activeDevice));
            debug("discovering services...");
            gatt.discoverServices();
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            debug("failed to connect");
        } else {
            throw new IllegalArgumentException("Unexpected state: " + newState);
        }

    }

    private void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
        assertOnUiThread();

        if (gatt != activeGatt) {
            return;
        }

        debug("onServicesDiscovered()");

        if (status != BluetoothGatt.GATT_SUCCESS) {
            debug("failed to discover services");
            disconnect();
        } else {
            requestCharacteristic = null;
            responseCharacteristic = null;
            for (BluetoothGattService service : gatt.getServices()) {
                if (service.getUuid().toString().startsWith("0000fff0-")) {
                    debug("   found Nova service");
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getUuid().toString().startsWith("0000fff3-")) {
                            debug("    found Nova request characteristic");
                            requestCharacteristic = characteristic;
                        }
                        if (characteristic.getUuid().toString().startsWith("0000fff4-")) {
                            debug("    found Nova response characteristic");
                            responseCharacteristic = characteristic;
                        }
                    }
                }
            }

            if (requestCharacteristic == null || responseCharacteristic == null) {
                debug("failed to find Nova characteristics");
                disconnect();
            } else {

                // Listen for responses (calls onCharacteristicChanged())
                gatt.setCharacteristicNotification(responseCharacteristic, true);
                BluetoothGattDescriptor descriptor = responseCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                if (descriptor == null) {
                    debug("failed to locate CLIENT_CHARACTERISTIC_CONFIG in response descriptor");
                    disconnect();
                    return;
                }
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if (!gatt.writeDescriptor(descriptor)) {
                    debug("failed to write ENABLE_NOTIFICATION to response descriptor");
                    disconnect();
                    return;
                }

                // READY to rock!
                setStatus(NovaLinkStatus.Ready);
            }
        }
    }

    private void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        assertOnUiThread();

        if (gatt != activeGatt && characteristic != requestCharacteristic) {
            return;
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            debug("onCharacteristicWrite() failed : " + status);
            return;
        }

        debug("onCharacteristicWrite() success");
    }

    private void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        assertOnUiThread();

        if (gatt != activeGatt && characteristic != responseCharacteristic) {
            return;
        }

        String response = responseCharacteristic.getStringValue(0);
        debug("recv <-- %s", response);

        int responseId = parseAck(response);

        if (responseId == PARSE_FAILED) {
            debug("Failed to parse response '%s'", response);
            disconnect();
            return;
        }

        if (awaitingAck == null) {
            debug("Was not expecting ack (got: %d)", responseId);
            disconnect();
            return;
        }

        if (awaitingAck.requestId != responseId) {
            debug("Unexpected ack (got: %d, expected: %d)", responseId, awaitingAck.requestId);
            disconnect();
            return;
        }

        debug("ack  <-- %s", frameMsg(awaitingAck.requestId, awaitingAck.msg));

        NovaCompletionCallback callback = awaitingAck.callback;

        // No longer awaiting the ack.
        awaitingAck = null;

        // Cancel timeout timer.
        ackTimerAllow.set(false);

        // Send any queued outbound messages.
        processSendQueue();

        // Trigger user callback.
        callback.onComplete(true);
    }

    private void disconnect() {
        assertOnUiThread();

        if (activeGatt != null) {
            activeGatt.disconnect();
            activeGatt.close();
        }

        if (currentScan != null) {
            bluetoothAdapter.stopLeScan(currentScan);
        }
        currentScan = null;

        activeDevice = null;
        activeGatt = null;
        requestCharacteristic = null;
        responseCharacteristic = null;

        // Cancel timers
        ackTimerAllow.set(false);
        stopScanTimerAllow.set(false);

        // Abort any queued requests.
        if (awaitingAck != null) {
            awaitingAck.callback.onComplete(false);
            awaitingAck = null;
        }

        for (Cmd cmd : awaitingSend) {
            cmd.callback.onComplete(false);
        }
        awaitingSend.clear();

        setStatus(NovaLinkStatus.Idle);
    }


    // -----------------------
    // Send commands to device
    // -----------------------

    @Override
    public void beginFlash(NovaFlashCommand flashCmd, NovaCompletionCallback callback) {
        assertOnUiThread();

        if (flashCmd.isPointless()) {
            // settings say that flash is effectively off
            request(offCmd(), callback);
        } else {
            request(lightCmd(flashCmd.getWarmness(), flashCmd.getCoolness(), flashCmd.getDuration()), callback);
        }
    }

    @Override
    public void beginFlash(NovaFlashCommand flashCmd) {
        assertOnUiThread();

        beginFlash(flashCmd, null);
    }

    @Override
    public void endFlash(NovaCompletionCallback callback) {
        assertOnUiThread();

        request(offCmd(), callback);
    }

    @Override
    public void endFlash() {
        assertOnUiThread();

        endFlash(null);
    }

    @Override
    public void ping(NovaCompletionCallback callback) {
        assertOnUiThread();

        request(pingCmd(), callback);
    }

    private void request(String msg, NovaCompletionCallback callback) {
        assertOnUiThread();

        if (this.status != NovaLinkStatus.Ready) {
            callback.onComplete(false);
            return;
        }

        if (++nextRequestId == 255) {
            nextRequestId = 0;
        }

        Cmd cmd = new Cmd();
        cmd.requestId = nextRequestId;
        cmd.msg = msg;
        cmd.callback = callback;

        awaitingSend.add(cmd);
        processSendQueue();
    }

    private void processSendQueue() {
        assertOnUiThread();

        // If we're not waiting for anything to be acked, go ahead and send the next cmd in the outbound queue.
        if (awaitingAck == null && !awaitingSend.isEmpty()) {

            // Shift first command from front of awaitingSend queue.
            Cmd cmd = awaitingSend.removeFirst();

            String body = frameMsg(cmd.requestId, cmd.msg);
            debug("send --> %s", body);

            // Write to device.
            requestCharacteristic.setValue(body);
            if (!activeGatt.writeCharacteristic(requestCharacteristic)) {
                debug("Failed to write value");
                activeGatt.abortReliableWrite(activeDevice);
                cmd.callback.onComplete(false);
                return;
            }

            // Now we're waiting for this.
            awaitingAck = cmd;

            // Set timer for acks so we don't hang forever waiting.
            ackTimerAllow.set(true);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (ackTimerAllow.getAndSet(false)) {
                        ackTookTooLong();
                    }
                }
            }, ACK_TIMEOUT);
        }
    }

    private void ackTookTooLong() {
        assertOnUiThread();

        if (awaitingAck != null) {
            debug("Timeout waiting for %s ack", frameMsg(awaitingAck.requestId, awaitingAck.msg));
            awaitingAck.callback.onComplete(false);
        }

        awaitingAck = null;
        processSendQueue();
    }

    private String frameMsg(int requestId, String msg) {
        // Requests are framed "(xx:yy)" where xx is 2 digit hex requestId and yy is body string.
        // e.g. "(00:P)"
        //      "(4A:L,00,FF,05DC)"
        return String.format("(%02X:%s)", requestId, msg);
    }

    private String pingCmd() {
        return "P";
    }

    private String lightCmd(int warmPwm, int coolPwm, int timeoutMillis) {
        // Light cmd is formatted "L,w,c,t" where w and c are warm/cool pwm duty cycles as 2 digit hex
        // and t is 4 digit hex timeout.
        // e.g. "L,00,FF,05DC" (means light with warm=0, cool=255, timeout=1500ms)
        return String.format("L,%02X,%02X,%04X", warmPwm, coolPwm, timeoutMillis);
    }

    private String offCmd() {
        return "O";
    }

    private int parseAck(String fullmsg) {
        // Parses "(xx:A)" packet where xx is hex value for resultId.

        Pattern regex = Pattern.compile("\\(([0-9A-Za-z][0-9A-Za-z]):A\\)");
        Matcher matcher = regex.matcher(fullmsg);

        if (matcher.matches()) {
            String requestIdHex = matcher.group(1);
            return Integer.parseInt(requestIdHex, 16);
        } else {
            return PARSE_FAILED;
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private String deviceDetails(BluetoothDevice device) {
        return "BluetoothDevice(name=" + device.getName()
                + ", address=" + device.getAddress()
                + ", bluetoothClass=" + device.getBluetoothClass()
                + ", uuids=" + Arrays.toString(device.getUuids())
                + ")";
    }
}
