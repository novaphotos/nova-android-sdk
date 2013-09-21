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

import java.io.Serializable;

/**
 * Details of flash to send to Nova.
 *
 * @see NovaLink#flash(NovaFlashCommand, NovaFlashCallback)
 *
 * @author Joe Walnes
 */
public final class NovaFlashCommand implements Serializable {

    private int warmness;
    private int coolness;
    private int duration;

    /**
     * @param warmness Brightness of warm LEDs (0=off, 255=fullpower)
     * @param coolness Brightness of cool LEDs (0=off, 255=fullpower)
     * @param duration Duration of flash in millis (max 65535)
     */
    public NovaFlashCommand(int warmness, int coolness, int duration) {
        setWarmness(warmness);
        setCoolness(coolness);
        setDuration(duration);
    }

    public NovaFlashCommand() {
        this(0, 0, 0);
    }

    /**
     * @return Brightness of warm LEDs (0=off, 255=fullpower)
     */
    public int getWarmness() {
        return warmness;
    }

    /**
     * @param warmness Brightness of warm LEDs (0=off, 255=fullpower)
     */
    public void setWarmness(int warmness) {
        validateRange("warmness", 0, 255, warmness);
        this.warmness = warmness;
    }

    /**
     * @return Brightness of cool LEDs (0=off, 255=fullpower)
     */
    public int getCoolness() {
        return coolness;
    }

    /**
     * @param coolness Brightness of cool LEDs (0=off, 255=fullpower)
     */
    public void setCoolness(int coolness) {
        validateRange("coolness", 0, 255, coolness);
        this.coolness = coolness;
    }

    /**
     * @return Duration of flash in millis (max 65535)
     */
    public int getDuration() {
        return duration;
    }

    /**
     * @param duration Duration of flash in millis (max 65535)
     */
    public void setDuration(int duration) {
        validateRange("duration", 0, 65535, duration);
        this.duration = duration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NovaFlashCommand that = (NovaFlashCommand) o;

        if (coolness != that.coolness) return false;
        if (duration != that.duration) return false;
        if (warmness != that.warmness) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = warmness;
        result = 31 * result + coolness;
        result = 31 * result + duration;
        return result;
    }

    @Override
    public String toString() {
        return "NovaFlashCommand{" +
                "warmness=" + warmness +
                ", coolness=" + coolness +
                ", duration=" + duration +
                '}';
    }

    private void validateRange(String fieldName, int min, int max, int value) throws IllegalArgumentException {
        if (value < min || value > max) {
            throw new IllegalArgumentException(fieldName + " must be in range " + min
                    + " to " + max + " (value is " + value + ")");
        }
    }

    /**
     * Encode command as packet to be sent to device.
     */
    public byte[] toPacket() {
        return new byte[] {
                'F',
                '!',
                (byte)warmness,
                (byte)coolness,
                (byte)(duration & 0xFF), // low byte
                (byte)((duration >> 8) & 0xFF), // high byte
                0
        };
    }

}
