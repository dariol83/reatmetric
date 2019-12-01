package eu.dariolucia.reatmetric.api.value;

public class ValueException extends Exception {
    public ValueException(String message) {
        super(message);
    }

    public ValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
