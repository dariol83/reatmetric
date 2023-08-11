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
