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


package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.ui.ReatmetricUI;

import java.io.*;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author Dario Lucia
 */
public class PresetStorageManager {
    
    private final String presetStorageLocation = System.getProperty("user.home") + File.separator + ReatmetricUI.APPLICATION_NAME + File.separator + "presets";
    
    private final String fileExtension = "json";

    public List<String> getAvailablePresets(String system, String user, String viewId) {
        File folder = new File(this.presetStorageLocation + File.separator + system + File.separator + user + File.separator + viewId);
        if(!folder.exists()) {
            return Collections.emptyList();
        }
        List<String> presets = new LinkedList<>();
        for(File f : folder.listFiles()) {
            if(f.getName().endsWith(this.fileExtension)) {
                presets.add(f.getName().substring(0, f.getName().length() - this.fileExtension.length() - 1));
            }
        }
        return presets;
    }

    private String buildLocationKey(String id) {
        return id;
    }

    private File locateFile(String system, String user, String viewId, String locationKey) {
        String filePath = this.presetStorageLocation + File.separator + system + File.separator + user + File.separator + viewId + File.separator + locationKey + "." + this.fileExtension;
        return new File(filePath);
    }

    public <T extends IPreset> T load(String system, String user, String id, String viewId, Class<T> presetClass) {
        String locationKey = buildLocationKey(id);
        File propsFile = locateFile(system, user, viewId, locationKey);
        T toReturn = null;
        if(propsFile.exists() && propsFile.canRead()) {
            try {
                toReturn = presetClass.getDeclaredConstructor().newInstance();
                toReturn.load(new FileInputStream(propsFile));
            } catch(Exception e) {
                e.printStackTrace();
                toReturn = null;
            }
        }
        return toReturn;
    }

    public void save(String system, String user, String id, String viewId, IPreset preset) {
        String locationKey = buildLocationKey(id);
        File propsFile = locateFile(system, user, viewId, locationKey);
        try {
            propsFile.getParentFile().mkdirs();
            PrintStream ps = new PrintStream(new FileOutputStream(propsFile));
            preset.save(ps);
            ps.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

}
