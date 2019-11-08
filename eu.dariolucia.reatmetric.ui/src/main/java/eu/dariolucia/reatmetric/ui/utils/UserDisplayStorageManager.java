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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author dario
 */
public class UserDisplayStorageManager {
    
    private final String userDisplayTemplateStorageLocation = System.getProperty("user.home") + File.separator + ReatmetricUI.APPLICATION_NAME + File.separator + "udds";
    
    private final String templateFileExtension = "fxml";
    
    private final String userDisplayStorageLocation = System.getProperty("user.home") + File.separator + ReatmetricUI.APPLICATION_NAME + File.separator + "saved_udds";
    
    private final String userDisplayFileExtension = "udd";
    
    public File loadTemplate(String id) {
        String locationKey = buildLocationKey(id);
        File propsFile = new File(this.userDisplayTemplateStorageLocation + File.separator + locationKey + "." + this.templateFileExtension);
        if(propsFile != null && propsFile.exists() && propsFile.canRead()) {
            return propsFile;
        } else {
        	return null;
        }
    }
    
    public List<String> getAvailableTemplates() {
        File folder = new File(this.userDisplayTemplateStorageLocation);
        if(!folder.exists()) {
            return Collections.emptyList();
        }
        List<String> presets = new LinkedList<>();
        for(File f : folder.listFiles()) {
            if(f.getName().endsWith(this.templateFileExtension)) {
                presets.add(f.getName().substring(0, f.getName().length() - this.templateFileExtension.length() - 1));
            }
        }
        return presets;
    }

    private String buildLocationKey(String id) {
        return id;
    }
    
//  public void saveDisplay(String system, String user, String id, String viewId, Properties props) {
//  String locationKey = buildLocationKey(id);
//  File propsFile = locateFile(system, user, viewId, locationKey);
//  try {
//      propsFile.getParentFile().mkdirs();
//      props.store(new FileOutputStream(propsFile), MonitoringCentreUI.APPLICATION_NAME + " " + MonitoringCentreUI.APPLICATION_VERSION + " " + locationKey + " " + String.valueOf(new Date()));
//  } catch(IOException e) {
//      e.printStackTrace();
//  }
//}
//
//public Properties loadDisplay(String system, String user, String id, String viewId) {
//  String locationKey = buildLocationKey(id);
//  File propsFile = locateFile(system, user, viewId, locationKey);
//  Properties toReturn = null;
//  if(propsFile != null && propsFile.exists() && propsFile.canRead()) {
//      try {
//          Properties p = new OrderedProperties();
//          p.load(new FileInputStream(propsFile));
//          toReturn = p;
//      } catch(IOException e) {
//          e.printStackTrace();
//      }
//  }
//  return toReturn;
//}
}
