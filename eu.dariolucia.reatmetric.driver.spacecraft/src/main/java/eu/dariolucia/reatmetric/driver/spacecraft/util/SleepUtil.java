/*
 * Copyright (c) 2020.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.driver.spacecraft.util;

import java.util.function.Supplier;

public class SleepUtil {

    public static boolean conditionalSleep(int totalMs, int checkTime, Supplier<Boolean> condition) {
        long time = System.currentTimeMillis() + totalMs;
        while(System.currentTimeMillis() < time) {
            if(condition.get()) {
                // Go on
                return false;
            }
            try {
                Thread.sleep(checkTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return true;
    }
}
