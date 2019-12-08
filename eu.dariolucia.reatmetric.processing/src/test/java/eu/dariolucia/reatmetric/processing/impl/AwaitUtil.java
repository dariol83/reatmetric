/*
 *   Copyright (c) 2019 Dario Lucia (https://www.dariolucia.eu)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package eu.dariolucia.reatmetric.processing.impl;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AwaitUtil {

    public static void awaitCondition(int maxMs, Supplier<Boolean> conditionChecker) throws InterruptedException {
        while(!conditionChecker.get()) {
            int minWait = Math.min(maxMs, 100);
            Thread.sleep(minWait);
            maxMs -= minWait;
            if(maxMs == 0) {
                break;
            }
        }
    }

    public static void awaitAndVerify(int maxMs, Supplier<Boolean> conditionChecker) throws InterruptedException {
        awaitCondition(maxMs, conditionChecker);
        assertTrue(conditionChecker.get());
    }

    public static void await(int ms) throws InterruptedException {
        Thread.sleep(ms);
    }
}
