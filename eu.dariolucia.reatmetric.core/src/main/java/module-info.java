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

import eu.dariolucia.reatmetric.api.IReatmetricSystem;

open module eu.dariolucia.reatmetric.core {
    requires java.logging;
    requires java.xml.bind;

    requires eu.dariolucia.reatmetric.api;
    requires eu.dariolucia.reatmetric.processing;

    exports eu.dariolucia.reatmetric.core.api;
    exports eu.dariolucia.reatmetric.core.api.exceptions;
    exports eu.dariolucia.reatmetric.core.configuration;

    uses eu.dariolucia.reatmetric.core.api.IDriver;
    uses eu.dariolucia.reatmetric.api.archive.IArchiveFactory;
    uses eu.dariolucia.reatmetric.api.processing.IProcessingModelFactory;

    provides IReatmetricSystem with eu.dariolucia.reatmetric.core.ServiceCoreImpl;
}