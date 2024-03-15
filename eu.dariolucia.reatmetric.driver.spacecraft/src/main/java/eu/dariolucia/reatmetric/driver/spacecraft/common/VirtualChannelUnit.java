/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.common;

import eu.dariolucia.ccsds.tmtc.util.AnnotatedObject;

/**
 * This class is used to deliver the data contained inside a frame, when the Virtual Channel Access service is used.
 */
public class VirtualChannelUnit extends AnnotatedObject {

    private final byte[] data;

    public VirtualChannelUnit(byte[] data) {
        if(data == null) {
            throw new NullPointerException("data is null");
        }
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public int getLength() {
        return data.length;
    }
}
