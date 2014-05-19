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

import java.io.Serializable;

/**
 * Details of flash to send to Nova.
 *
 * @author Joe Walnes
 */
public final class NovaFlashCommand implements Serializable {

    private final int warmness;
    private final int coolness;
    private final int duration;

    public static final int DEFAULT_DURATION = 1500;

    /**
     * @param warmness Brightness of warm LEDs (0=off, 255=fullpower)
     * @param coolness Brightness of cool LEDs (0=off, 255=fullpower)
     * @param duration Duration of flash in millis (max 65535)
     */
    public NovaFlashCommand(int warmness, int coolness, int duration) {
        validateRange("warmness", 0, 255, warmness);
        validateRange("coolness", 0, 255, coolness);
        validateRange("duration", 0, 65535, duration);

        this.warmness = warmness;
        this.coolness = coolness;
        this.duration = duration;
    }

    /**
     * @param warmness Brightness of warm LEDs (0=off, 255=fullpower)
     * @param coolness Brightness of cool LEDs (0=off, 255=fullpower)
     */
    public NovaFlashCommand(int warmness, int coolness) {
        this(warmness, coolness, DEFAULT_DURATION);
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
     * @return Brightness of cool LEDs (0=off, 255=fullpower)
     */
    public int getCoolness() {
        return coolness;
    }
    /**
     * @return Duration of flash in millis (max 65535)
     */
    public int getDuration() {
        return duration;
    }

    /**
     * Is the flash pointless? i.e. LEDs are off, or duration is 0.
     */
    public boolean isPointless() {
        return duration == 0 || (coolness == 0 && warmness == 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        NovaFlashCommand that = (NovaFlashCommand) o;
        return this.warmness == that.warmness
               && this.coolness == that.coolness
               && this.duration == that.duration;
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
     * Preset settings for flash 'off'. No flash will occur.
     */
    public static NovaFlashCommand off() {
        return new NovaFlashCommand();
    }

    /**
     * Preset settings for 'gentle' flash.
     * A slight warm light, ideal for closeup photos.
     */
    public static NovaFlashCommand gentle() {
        return new NovaFlashCommand(127, 0);
    }

    /**
     * Preset settings for 'warm' flash.
     * A bright natural looking warm light, ideal for people portraits at night.
     */
    public static NovaFlashCommand warm() {
        return new NovaFlashCommand(255, 0);
    }

    /**
     * Preset settings for 'bright' flash.
     * The brightest mode, with all LEDs at full brightness.
     */
    public static NovaFlashCommand bright() {
        return new NovaFlashCommand(255, 255);
    }

    /**
     * Custom brightness settings. Warm and cool LEDs are each in range 0-255, where 0=off and 255=max.
     */
    public static NovaFlashCommand custom(int warm, int cool) {
        return new NovaFlashCommand(warm, cool);
    }

    /**
     * Custom brightness settings. Warm and cool LEDs are each in range 0-255, where 0=off and 255=max.
     * Duration in millis.
     */
    public static NovaFlashCommand custom(int warm, int cool, int duration) {
        return new NovaFlashCommand(warm, cool, duration);
    }


}
