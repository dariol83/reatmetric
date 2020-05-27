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
import javafx.collections.FXCollections;
import javafx.scene.control.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author dario
 */
public class TableViewUtil {

    private static final Logger LOG = Logger.getLogger(TableViewUtil.class.getName());

    public static boolean restoreColumnConfiguration(String csystem, String cuser, String id, TreeTableView<?> table) {
        try {
            if(csystem != null && cuser != null) {
                Properties p = ReatmetricUI.preferences().load(csystem, cuser, id);
                if(p != null) {
                    // Construct a map: table column to information (width as double, position as int)
                    Map<TreeTableColumn, Object[]> infoMap = new HashMap<>();
                    for(TreeTableColumn tc : table.getColumns()) {
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
                    Collections.sort(table.getColumns(), (Comparator<TreeTableColumn>) (o1, o2) -> {
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
                    });
                    // Resize
                    for(TreeTableColumn tc : infoMap.keySet()) {
                        Object[] info = infoMap.get(tc);
                        if(info.length != 0) {
                            tc.setPrefWidth(((Double)info[0]).doubleValue());
                        }
                    }
                }
                return true;
            }
        } catch (Exception e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Cannot restore tree table settings for " + csystem + ", " + cuser + ", " + id + ": " + e.getMessage(), e);
            }
        }
        return false;
    }

    public static boolean restoreColumnConfiguration(String csystem, String cuser, String id, TableView<?> table) {
        try {
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
                    Collections.sort(table.getColumns(), (Comparator<TableColumn>) (o1, o2) -> {
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
        } catch (Exception e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Cannot restore table settings for " + csystem + ", " + cuser + ", " + id + ": " + e.getMessage(), e);
            }
        }
        return false;
    }
    
    public static boolean persistColumnConfiguration(String csystem, String cuser, String id, TableView<?> table) {
        try {
            if (csystem != null && cuser != null) {
                Properties p = new Properties();
                for (TableColumn tc : table.getColumns()) {
                    p.setProperty(tc.getText(), tc.getWidth() + " " + table.getColumns().indexOf(tc));
                }
                ReatmetricUI.preferences().save(csystem, cuser, id, p);
                return true;
            }
        } catch (Exception e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Cannot persist table settings for " + csystem + ", " + cuser + ", " + id + ": " + e.getMessage(), e);
            }
        }
        return false;
    }

    public static boolean persistColumnConfiguration(String csystem, String cuser, String id, TreeTableView<?> table) {
        try {
            if (csystem != null && cuser != null) {
                Properties p = new Properties();
                for (TreeTableColumn tc : table.getColumns()) {
                    p.setProperty(tc.getText(), tc.getWidth() + " " + table.getColumns().indexOf(tc));
                }
                ReatmetricUI.preferences().save(csystem, cuser, id, p);
                return true;
            }
        } catch (Exception e) {
            if(LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Cannot persist tree table settings for " + csystem + ", " + cuser + ", " + id + ": " + e.getMessage(), e);
            }
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

    public static Control buildNodeForPrinting(TreeTableView<?> table) {
        TreeItem<?> root = clone(table.getRoot());
        TreeTableView<?> cloned = new TreeTableView<>(root);
        double width = 0;
        for(TreeTableColumn tc : table.getColumns()) {
            TreeTableColumn newTc = new TreeTableColumn();
            newTc.setText(tc.getText());
            newTc.setCellFactory(tc.getCellFactory());
            newTc.setCellValueFactory(tc.getCellValueFactory());
            newTc.setPrefWidth(tc.getWidth());
            width += tc.getWidth();
            cloned.getColumns().add(newTc);
        }
        cloned.setPrefWidth(width);
        return cloned;
    }

    private static TreeItem<?> clone(TreeItem<?> root) {
        TreeItem<?> toReturn = new TreeItem<>(root.getValue());
        for(TreeItem<?> child : root.getChildren()) {
            clone(child, toReturn);
        }
        return toReturn;
    }

    private static void clone(TreeItem<?> child, TreeItem toReturn) {
        TreeItem cloned = new TreeItem<>(child.getValue());
        toReturn.getChildren().add(cloned);
        for(TreeItem<?> inChild : child.getChildren()) {
            clone(inChild, cloned);
        }
    }
}
