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
