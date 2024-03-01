/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.spacecraft.test;

import eu.dariolucia.reatmetric.api.value.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class CltuReader {

    private final InputStream stream;

    public CltuReader(InputStream is) {
        this.stream = is;
    }

    public byte[] readNext() throws IOException {
        // read EB90
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] header = stream.readNBytes(2);
        if(header.length == 0) {
            throw new IOException("End of stream");
        }
        if(header[0] != (byte) 0xEB || header[1] != (byte) 0x90) {
            throw new IOException("No valid header found: " + StringUtil.toHexDump(header));
        }
        bos.write(header);
        // read 8 bytes: if it is C5 C5 C5 C5 C5 C5 C5 79, done, else go ahead
        byte[] end = StringUtil.toByteArray("C5C5C5C5C5C5C579");
        while(true) {
            byte[] block = stream.readNBytes(8);
            bos.write(block);
            if(Arrays.equals(end, block)) {
                return bos.toByteArray();
            }
        }
    }
}
