/*
 * Copyright (c)  2021 Dario Lucia (https://www.dariolucia.eu)
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
package eu.dariolucia.reatmetric.driver.socket.configuration;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public enum AsciiEncoding {
    ISO(StandardCharsets.ISO_8859_1),
    US_ASCII(StandardCharsets.US_ASCII),
    UTF8(StandardCharsets.UTF_8),
    UTF16(StandardCharsets.UTF_16);

    private final Charset charset;

    AsciiEncoding(Charset charset) {
        this.charset = charset;
    }

    public Charset getCharset() {
        return charset;
    }
}
