/*
 * Copyright (c)  2022 Dario Lucia (https://www.dariolucia.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package eu.dariolucia.reatmetric.driver.automation.base.definition;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;

@XmlRootElement(name = "automation", namespace = "http://dariolucia.eu/reatmetric/driver/automation")
@XmlAccessorType(XmlAccessType.FIELD)
public class AutomationConfiguration {

    private static final String HOME_VAR = "$HOME";
    private static final String HOME_DIR = System.getProperty("user.home");

    public static AutomationConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(AutomationConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            AutomationConfiguration o = (AutomationConfiguration) u.unmarshal(is);
            if(o.getScriptFolder() != null) {
                o.setScriptFolder(o.getScriptFolder().replace(HOME_VAR, HOME_DIR));
            }
            return o;
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlElement(name = "max-parallel-scripts")
    private int maxParallelScripts = 1;

    @XmlElement(name = "script-folder", required = true)
    private String scriptFolder;

    public int getMaxParallelScripts() {
        return maxParallelScripts;
    }

    public void setMaxParallelScripts(int maxParallelScripts) {
        this.maxParallelScripts = maxParallelScripts;
    }

    public String getScriptFolder() {
        return scriptFolder;
    }

    public void setScriptFolder(String scriptFolder) {
        this.scriptFolder = scriptFolder;
    }

}
