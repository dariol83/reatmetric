/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author dario
 */
public class PresetStorageManager {
    
    private final String presetStorageLocation = System.getProperty("user.home") + File.separator + ReatmetricUI.APPLICATION_NAME + File.separator + "presets";
    
    private final String fileExtension = "dat";
    
    public void save(String system, String user, String id, String viewId, Properties props) {
        String locationKey = buildLocationKey(id);
        File propsFile = locateFile(system, user, viewId, locationKey);
        try {
            propsFile.getParentFile().mkdirs();
            props.store(new FileOutputStream(propsFile), ReatmetricUI.APPLICATION_NAME + " " + ReatmetricUI.APPLICATION_VERSION + " " + locationKey + " " + String.valueOf(new Date()));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public Properties load(String system, String user, String id, String viewId) {
        String locationKey = buildLocationKey(id);
        File propsFile = locateFile(system, user, viewId, locationKey);
        Properties toReturn = null;
        if(propsFile != null && propsFile.exists() && propsFile.canRead()) {
            try {
                Properties p = new OrderedProperties();
                p.load(new FileInputStream(propsFile));
                toReturn = p;
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return toReturn;
    }
    
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
}
