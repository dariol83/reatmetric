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


package eu.dariolucia.reatmetric.ui.preferences;

import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author dario
 */
public class PreferencesManager {
    
    private final String propertiesStorageLocation = System.getProperty("user.home") + File.separator + ReatmetricUI.APPLICATION_NAME + File.separator + "prefs";
    
    private final String fileExtension = "dat";
    
    private final Map<String, Properties> propertiesCache = new HashMap<>();
    
    public synchronized void save(String system, String user, String id, Properties props) {
        String locationKey = buildLocationKey(system, user, id);
        File propsFile = locateFile(locationKey);
        try {
            propsFile.getParentFile().mkdirs();
            props.store(new FileOutputStream(propsFile), ReatmetricUI.APPLICATION_NAME + " " + ReatmetricUI.APPLICATION_VERSION + " " + locationKey + " " + String.valueOf(new Date()));
            this.propertiesCache.put(locationKey, props);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    public synchronized Properties load(String system, String user, String id) {
        return load(system, user, id, false);
    }
    
    public synchronized Properties load(String system, String user, String id, boolean reload) {
        String locationKey = buildLocationKey(system, user, id);
        Properties toReturn = this.propertiesCache.get(locationKey);
        if(toReturn == null || reload) {
            File propsFile = locateFile(locationKey);
            if(propsFile != null && propsFile.exists() && propsFile.canRead()) {
                try {
                    Properties p = new Properties();
                    p.load(new FileInputStream(propsFile));
                    toReturn = p;
                    this.propertiesCache.put(locationKey, toReturn);
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return toReturn;
    }

    private String buildLocationKey(String system, String user, String id) {
        return system + "_" + user + "_" + id;
    }

    private File locateFile(String locationKey) {
        String filePath = this.propertiesStorageLocation + File.separator + locationKey + "." + this.fileExtension;
        return new File(filePath);
    }
}
