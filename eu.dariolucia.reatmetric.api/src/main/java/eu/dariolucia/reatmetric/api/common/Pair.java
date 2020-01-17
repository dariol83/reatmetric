package eu.dariolucia.reatmetric.api.common;

import java.io.Serializable;
import java.util.Objects;

public final class Pair<T, K> implements Serializable {

    public static <T,K> Pair<T, K> of(T first, K second) {
        return new Pair<>(first, second);
    }

    private final T first;
    private final K second;

    private Pair(T first, K second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public K getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) &&
                Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
