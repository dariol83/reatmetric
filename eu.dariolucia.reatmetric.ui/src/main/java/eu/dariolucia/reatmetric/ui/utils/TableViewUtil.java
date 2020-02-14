/*
 * Copyright (c) 2019.  Dario Lucia (dario.lucia@gmail.com)
 * All rights reserved
 *
 * Right to reproduce, use, modify and distribute (in whole or in part) this library for demonstrations/trainings/study/commercial purposes
 * shall be granted by the author in writing.
 */


package eu.dariolucia.reatmetric.ui.utils;

import eu.dariolucia.reatmetric.ui.ReatmetricUI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javafx.collections.FXCollections;
import javafx.scene.control.Control;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 *
 * @author dario
 */
public class TableViewUtil {
    
    public static boolean restoreColumnConfiguration(String csystem, String cuser, String id, TableView<?> table) {
        if(csystem != null && cuser != null) {
            Properties p = ReatmetricUI.preferences().load(csystem, cuser, id);
            if(p != null) {
                // Construct a map: table column to information (width as double, position as int)
                Map<TableColumn, Object[]> infoMap = new HashMap<>();
                for(TableColumn tc : table.getColumns()) {
                    String storedTcProp = p.getProperty(tc.getText(), "");
                    if(!storedTcProp.isEmpty()) {
                        String[] splt = storedTcProp.split(" ", -1);
                        infoMap.put(tc, new Object[] {
                           Double.parseDouble(splt[0]),
                           Integer.parseInt(splt[1])
                        });        
                    } else {
                        infoMap.put(tc, new Object[0]);
                    }
                }
                // Re-order
                Collections.sort(table.getColumns(), new Comparator<TableColumn>() {
                    @Override
                    public int compare(TableColumn o1, TableColumn o2) {
                        Object[] first = infoMap.get(o1);
                        Object[] second = infoMap.get(o2);
                        if(first.length == 0 && second.length == 0) {
                            return o1.getText().compareTo(o2.getText());
                        } else if(first.length == 0 && second.length != 0) {
                            return -1;
                        } else if(first.length != 0 && second.length == 0) {
                            return 1;
                        } else {
                            return ((Integer)first[1]) - ((Integer)second[1]);
                        }
                    }
                });
                // Resize
                for(TableColumn tc : infoMap.keySet()) {
                    Object[] info = infoMap.get(tc);
                    if(info.length != 0) {
                        tc.setPrefWidth(((Double)info[0]).doubleValue());
                    }
                }
            }
            return true;
        }
        return false;
    }
    
    public static boolean persistColumnConfiguration(String csystem, String cuser, String id, TableView<?> table) {
        if(csystem != null && cuser != null) {
            Properties p = new Properties();
            for(TableColumn tc : table.getColumns()) {
                p.setProperty(tc.getText(), tc.getWidth() + " " + String.valueOf(table.getColumns().indexOf(tc)));
            }
            ReatmetricUI.preferences().save(csystem, cuser, id, p);
            return true;
        }
        return false;
    }
    
    public static Control buildNodeForPrinting(TableView<?> table) {
        List<?> items = new ArrayList<>(table.getItems());
        TableView<?> cloned = new TableView<>(FXCollections.observableArrayList(items));
        double width = 0;
        double height = table.getItems().size() * 24 + 30;
        for(TableColumn tc : table.getColumns()) {
            TableColumn newTc = new TableColumn();
            newTc.setText(tc.getText());
            newTc.setCellFactory(tc.getCellFactory());
            newTc.setCellValueFactory(tc.getCellValueFactory());
            newTc.setPrefWidth(tc.getWidth());
            width += tc.getWidth();
            cloned.getColumns().add(newTc);
        }
        cloned.setPrefWidth(width);
        cloned.setPrefHeight(height);
        return cloned;
    }
}
