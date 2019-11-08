/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
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
