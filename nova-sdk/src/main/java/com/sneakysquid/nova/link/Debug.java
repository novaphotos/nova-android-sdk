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

import android.util.Log;

/**
 * @author Joe Walnes
 */
public class Debug {

    /**
     * Log at debug level.
     */
    public static void debug(String msg, Object... args) {
        Log.d(obtainTagFromCallStack(2), String.format(msg, args));
    }

    /**
     * Check that the caller of this method is running on the UI thread associated
     * with an Activity. Throws IllegalThreadStateException if on any other thread.
     */
    public static void assertOnUiThread() throws IllegalThreadStateException {
        if (Thread.currentThread().getId() != 1) {
            throw new RuntimeException("Not on UI Thread (on thread " + Thread.currentThread().getId() + ")");
        }
    }

    /**
     * Generate a short tag name automatically by looking at the call stack and
     * determining the file that this was called from.
     *
     * @param stackDepth How many levels down the call stack to go. 0 is this call,
     *                   1 is the caller, 2 is the caller's caller, etc.
     */
    private static String obtainTagFromCallStack(int stackDepth) {
        String fileName = new Throwable().getStackTrace()[stackDepth].getFileName();
        return fileName.replaceFirst("^.*/", "").replaceFirst("\\.java$", "");
    }

}
