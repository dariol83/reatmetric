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

package eu.dariolucia.reatmetric.core;

import eu.dariolucia.reatmetric.api.IReatmetricRegister;
import eu.dariolucia.reatmetric.api.IReatmetricSystem;
import eu.dariolucia.reatmetric.api.common.exceptions.ReatmetricException;

import java.util.Collections;
import java.util.List;

/**
 * Implementation of a registry that returns a single system (configured via system property).
 */
public class ReatmetricRegisterImpl implements IReatmetricRegister {

    private static final String INIT_FILE_KEY = "reatmetric.core.config"; // Absolute location of the configuration file, to configure the core instance

    private static IReatmetricSystem system;

    @Override
    public List<IReatmetricSystem> availableSystems() throws ReatmetricException {
        synchronized (ReatmetricRegisterImpl.class) {
            if(system == null) {
                system = new ReatmetricSystemImpl(System.getProperty(INIT_FILE_KEY));
            }
            return Collections.singletonList(system);
        }
    }
}
