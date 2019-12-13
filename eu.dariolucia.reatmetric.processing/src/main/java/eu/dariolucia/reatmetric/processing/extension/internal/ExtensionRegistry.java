/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved.
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes shall be granted by the author in writing.
 */

package eu.dariolucia.reatmetric.processing.extension.internal;

import eu.dariolucia.reatmetric.processing.extension.ICalibrationExtension;
import eu.dariolucia.reatmetric.processing.extension.ICheckExtension;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.TreeMap;

public class ExtensionRegistry {

    private static Map<String, ICalibrationExtension> calibrationExtensionMap = null;

    public static ICalibrationExtension resolveCalibration(String function) {
        synchronized(ICalibrationExtension.class) {
            if (calibrationExtensionMap == null) {
                calibrationExtensionMap = new TreeMap<>();
                ServiceLoader<ICalibrationExtension> loader = ServiceLoader.load(ICalibrationExtension.class);
                for(ICalibrationExtension ext : loader) {
                    calibrationExtensionMap.put(ext.getFunctionName(), ext);
                }
            }
        }
        return calibrationExtensionMap.get(function);
    }

    private static Map<String, ICheckExtension> checkExtensionMap = null;

    public static ICheckExtension resolveCheck(String function) {
        synchronized(ICalibrationExtension.class) {
            if (checkExtensionMap == null) {
                checkExtensionMap = new TreeMap<>();
                ServiceLoader<ICheckExtension> loader = ServiceLoader.load(ICheckExtension.class);
                for(ICheckExtension ext : loader) {
                    checkExtensionMap.put(ext.getFunctionName(), ext);
                }
            }
        }
        return checkExtensionMap.get(function);
    }
}
