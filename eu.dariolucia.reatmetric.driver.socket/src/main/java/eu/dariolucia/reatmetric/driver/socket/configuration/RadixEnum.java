package eu.dariolucia.reatmetric.driver.socket.configuration;

public enum RadixEnum {
    BIN(2),
    OCT(8),
    DEC(10),
    HEX(16);

    private int radix;

    RadixEnum(int radix) {
        this.radix = radix;
    }

    public int getRadix() {
        return radix;
    }
}
