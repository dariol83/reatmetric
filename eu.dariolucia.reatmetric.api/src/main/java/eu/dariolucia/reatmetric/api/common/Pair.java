/*
 * Copyright (c)  2020 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.api.common;

import java.io.Serializable;
import java.util.Objects;

/**
 * A pair class that contains two related values in a single object.
 *
 * Objects of this class are immutable. Values shall be immutable: failing to have immutable objects as values
 * may result in undefined behaviour.
 *
 * @param <T> type of the first value
 * @param <K> type of the second value
 */
public final class Pair<T, K> implements Serializable {

    /**
     * Factory method.
     *
     * @param first the first value, can be null
     * @param second the second value, can be null
     * @param <T> the first value type
     * @param <K> the second value type
     * @return the Pair object
     */
    public static <T,K> Pair<T, K> of(T first, K second) {
        return new Pair<>(first, second);
    }

    private final T first;
    private final K second;

    private Pair(T first, K second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Return the first value.
     *
     * @return the first value (can be null)
     */
    public T getFirst() {
        return first;
    }

    /**
     * Return the second value.
     *
     * @return the second value (can be null)
     */
    public K getSecond() {
        return second;
    }

    /**
     * A Pair is equal to another one if the first values are equals(...) and the second values are equals(...)
     *
     * @param o the Pair object, to check equality against
     * @return true if the pairs are equals, otherwise false
     */
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

    @Override
    public String toString() {
        return "Pair[" +
                "first=" + first +
                ", second=" + second +
                ']';
    }
}
