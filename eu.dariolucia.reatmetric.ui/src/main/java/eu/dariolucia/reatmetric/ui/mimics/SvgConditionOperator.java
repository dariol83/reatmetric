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

package eu.dariolucia.reatmetric.ui.mimics;

import eu.dariolucia.reatmetric.api.value.ValueUtil;

import java.util.Objects;
import java.util.function.BiFunction;

public enum SvgConditionOperator implements BiFunction<Object, Object, Boolean> {
    LT,
    GT,
    LTE,
    GTE,
    EQ,
    NQ;

    @Override
    public Boolean apply(Object o, Object o2) {
        switch(this) {
            case EQ:
                return innerEquals(o, o2);
            case NQ:
                return !innerEquals(o, o2);
            case LTE:
                return innerEquals(o, o2) || innerCompare(o, o2) < 0;
            case GTE:
                return innerEquals(o, o2) || innerCompare(o, o2) > 0;
            case LT:
                return innerCompare(o, o2) < 0;
            case GT:
                return innerCompare(o, o2) > 0;
            default:
                throw new IllegalStateException("Missing support for operator " + this);
        }
    }

    private boolean innerEquals(Object o1, Object o2) {
        if(o1 instanceof Number) {
            // Assume that also o2 is a number
            Number n1 = (Number) o1;
            Number n2 = (Number) o2;
            if(n1 instanceof Double || n2 instanceof Double) {
                return n1.doubleValue() == n2.doubleValue();
            } else if(n1 instanceof Long || n2 instanceof Long) {
                return n1.longValue() == n2.longValue();
            } else {
                return n1.intValue() == n2.intValue();
            }
        } else {
            return Objects.equals(o1, o2);
        }
    }

    /**
     * When comparing boxed numbers (long, int, double), the standard equality and compare do not work well.
     *
     * @param o1 the first object to compare
     * @param o2 the second object to compare
     * @return -1 if o1 is before o2, 1 if o1 is after o2, 0 if equal
     */
    private int innerCompare(Object o1, Object o2) {
        if(o1 instanceof Number) {
            // Assume that also o2 is a number
            Number n1 = (Number) o1;
            Number n2 = (Number) o2;
            if(n1 instanceof Double || n2 instanceof Double) {
                double result = n1.doubleValue() - n2.doubleValue();
                return result == 0 ? 0 : (result < 0 ? -1 : 1);
            } else if(n1 instanceof Long || n2 instanceof Long) {
                long result = n1.longValue() - n2.longValue();
                return result == 0 ? 0 : (result < 0 ? -1 : 1);
            } else {
                return n1.intValue() - n2.intValue();
            }
        } else {
            return ValueUtil.compare(o1, o2);
        }
    }
}
