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

import eu.dariolucia.reatmetric.api.processing.IProcessingModelFactory;

open module eu.dariolucia.reatmetric.processing {
    requires java.logging;
    requires jakarta.xml.bind;
    requires java.scripting;

    requires eu.dariolucia.reatmetric.api;
    requires org.codehaus.groovy;

    exports eu.dariolucia.reatmetric.processing.extension;
    exports eu.dariolucia.reatmetric.processing.definition;

    uses eu.dariolucia.reatmetric.processing.extension.ICalibrationExtension;
    uses eu.dariolucia.reatmetric.processing.extension.ICheckExtension;

    provides IProcessingModelFactory with eu.dariolucia.reatmetric.processing.impl.ProcessingModelFactoryImpl;
    provides eu.dariolucia.reatmetric.processing.extension.ICalibrationExtension with eu.dariolucia.reatmetric.processing.extension.internal.IdentityCalibration;
    provides eu.dariolucia.reatmetric.processing.extension.ICheckExtension with eu.dariolucia.reatmetric.processing.extension.internal.NoCheck;
}