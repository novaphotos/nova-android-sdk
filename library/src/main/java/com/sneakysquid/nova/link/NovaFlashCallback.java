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
 * Clients implement this receive acknowledgment when flash completed (or failed).
 *
 * @see NovaLink#flash(NovaFlashCommand, NovaFlashCallback)
 *
 * @author Joe Walnes
 */
public interface NovaFlashCallback {

    /**
     * @param successful Whether the flash was succesful.
     */
    void onNovaFlashAcknowledged(boolean successful);
}
