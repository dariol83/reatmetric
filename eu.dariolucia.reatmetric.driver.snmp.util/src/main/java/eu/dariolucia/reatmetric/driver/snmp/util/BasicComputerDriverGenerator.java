/*
 * Copyright (c)  2024 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.snmp.util;

import eu.dariolucia.reatmetric.api.value.ValueTypeEnum;
import eu.dariolucia.reatmetric.driver.snmp.configuration.GroupConfiguration;
import eu.dariolucia.reatmetric.driver.snmp.configuration.OidEntry;
import eu.dariolucia.reatmetric.driver.snmp.configuration.OidEntryType;
import eu.dariolucia.reatmetric.driver.snmp.configuration.SnmpDeviceConfiguration;
import eu.dariolucia.reatmetric.processing.definition.*;
import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TableEvent;
import org.snmp4j.util.TableUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class BasicComputerDriverGenerator {
    private static final OID systemUptimeOID = new OID(".1.3.6.1.2.1.25.1.1.0"); // system uptime
    private static final OID systemDateOID = new OID(".1.3.6.1.2.1.25.1.2.0"); // system date
    private static final OID numUsersOID = new OID(".1.3.6.1.2.1.25.1.5.0"); // num users
    private static final OID systemDescrOID = new OID(".1.3.6.1.2.1.1.1.0"); // system description
    private static final OID systemNameOID = new OID(".1.3.6.1.2.1.1.5.0"); // system name
    private static final OID memorySizeOID = new OID(".1.3.6.1.2.1.25.2.2.0"); // memory size (KB)
    private static final OID processorTableOID = new OID(".1.3.6.1.2.1.25.3.3"); // processor table
    private static final OID processorLoadOID = new OID(".1.3.6.1.2.1.25.3.3.1.2"); // processor load prefix
    private static final OID diskStorageTableOID = new OID(".1.3.6.1.2.1.25.3.6"); // disk storage table
    private static final OID diskCapacityOID = new OID(".1.3.6.1.2.1.25.3.6.1.4"); // capacity prefix (KB)
    private static final OID storageTableOID = new OID(".1.3.6.1.2.1.25.2.3"); // storage table
    private static final OID storageTypeOID = new OID(".1.3.6.1.2.1.25.2.3.1.2"); // storage type prefix
    private static final OID storageDescrOID = new OID(".1.3.6.1.2.1.25.2.3.1.3"); // storage description prefix
    private static final OID storageAllocUnitOID = new OID(".1.3.6.1.2.1.25.2.3.1.4"); // storage allocation unit prefix
    private static final OID storageSizeOID = new OID(".1.3.6.1.2.1.25.2.3.1.5"); // storage size prefix
    private static final OID storageUsedOID = new OID(".1.3.6.1.2.1.25.2.3.1.6"); // storage used prefix
    private static final OID interfaceTableOID = new OID(".1.3.6.1.2.1.2.2"); // interface table
    private static final OID interfaceDescrOID = new OID(".1.3.6.1.2.1.2.2.1.2"); // interface description prefix
    private static final OID interfaceTypeOID = new OID(".1.3.6.1.2.1.2.2.1.3"); // interface type prefix
    private static final OID interfaceSpeedOID = new OID(".1.3.6.1.2.1.2.2.1.5"); // interface speed prefix
    private static final OID interfaceMacOID = new OID(".1.3.6.1.2.1.2.2.1.6"); // interface MAC prefix
    private static final OID interfaceAdminOID = new OID(".1.3.6.1.2.1.2.2.1.7"); // interface admin prefix
    private static final OID interfaceOperOID = new OID(".1.3.6.1.2.1.2.2.1.8"); // interface operation prefix
    private static final OID interfaceInOctOID = new OID(".1.3.6.1.2.1.2.2.1.10"); // interface in octets prefix
    private static final OID interfaceInDiscardOID = new OID(".1.3.6.1.2.1.2.2.1.13"); // interface in discards prefix
    private static final OID interfaceInErrorsOID = new OID(".1.3.6.1.2.1.2.2.1.14"); // interface in errors prefix
    private static final OID interfaceInUnknownOID = new OID(".1.3.6.1.2.1.2.2.1.15"); // interface in unknown prefix
    private static final OID interfaceOutOctOID = new OID(".1.3.6.1.2.1.2.2.1.16"); // interface out octets prefix
    private static final OID interfaceOutErrorsOID = new OID(".1.3.6.1.2.1.2.2.1.20"); // interface out errors prefix
    private static final OID interfaceOutDiscardsOID = new OID(".1.3.6.1.2.1.2.2.1.20"); // interface out discards prefix

    private Snmp snmp;
    private int externalIdStart;
    private String pathPrefix;
    private ProcessingDefinition processingDefinition;
    private SnmpDeviceConfiguration snmpDeviceConfiguration;

    public void export(String snmpConnectionUrl, String community, int externalIdStart, String pathPrefix) throws IOException {
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);
        transport.listen();

        CommunityTarget<UdpAddress> target = buildTarget(snmpConnectionUrl, community);
        this.externalIdStart = externalIdStart;
        if(pathPrefix.endsWith(".")) {
            pathPrefix = pathPrefix.substring(0, pathPrefix.length() - 1);
        }
        this.pathPrefix = pathPrefix;
        initialise();
        processSystem(target);
        processDevices(target);
        processStorage(target);
        processNetwork(target);
        finalise();

        this.snmp.close();
    }

    private void finalise() throws IOException {
        File modelFile = new File(pathPrefix.replace(".", "_") + "_" + "model.xml");
        if(!modelFile.exists()) {
            modelFile.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(modelFile);
        ProcessingDefinition.save(this.processingDefinition, fos);
        fos.close();

        File snmpDeviceFile = new File(pathPrefix.replace(".", "_") + "_" + "device.xml");
        if(!snmpDeviceFile.exists()) {
            snmpDeviceFile.createNewFile();
        }
        FileOutputStream fos2 = new FileOutputStream(snmpDeviceFile);
        SnmpDeviceConfiguration.save(this.snmpDeviceConfiguration, fos2);
        fos2.close();
    }

    private void initialise() {
        processingDefinition = new ProcessingDefinition();
        String thePathPrefix = this.pathPrefix;
        if(!thePathPrefix.endsWith(".")) {
            thePathPrefix += ".";
        }
        processingDefinition.setPathPrefix(thePathPrefix);
        snmpDeviceConfiguration = new SnmpDeviceConfiguration();
    }

    private void processStorage(CommunityTarget<UdpAddress> target) {
        String groupName = "Storage";
        GroupConfiguration gc = new GroupConfiguration();
        gc.setName(groupName);
        gc.setDistributePdu(false);
        gc.setPollingTime(2 * 60000); // Once every 2 minutes is enough
        this.snmpDeviceConfiguration.getGroupConfigurationList().add(gc);

        // Storage table
        TableUtils tUtils = new TableUtils(this.snmp, new DefaultPDUFactory());
        List<TableEvent> events = tUtils.getTable(target, new OID[] { storageTableOID }, null, null);
        for (TableEvent event : events) {
            if (event.isError()) {
                System.err.println("Error event when parsing storage block");
                continue;
            }
            for (VariableBinding vb : event.getColumns()) {
                OID key = vb.getOid();
                int storageNb = key.get(key.size() - 1);
                // Get the idx
                if (key.startsWith(storageTypeOID)) {
                    // Type
                    addParameter(gc, groupName + storageNb, "Type", OidEntryType.OID, key, null, storageTypeCalibration());
                } else if (key.startsWith(storageDescrOID)) {
                    // Description
                    addParameter(gc, groupName + storageNb, "Description", OidEntryType.STRING, key);
                } else if (key.startsWith(storageAllocUnitOID)) {
                    // Allocation Unit
                    addParameter(gc, groupName + storageNb, "Allocation_Unit", OidEntryType.LONG, key);
                } else if (key.startsWith(storageSizeOID)) {
                    // Storage size
                    addParameter(gc, groupName + storageNb, "Storage_Size", OidEntryType.LONG, key, "bytes");
                } else if (key.startsWith(storageUsedOID)) {
                    // Storage used
                    addParameter(gc, groupName + storageNb, "Storage_Used", OidEntryType.LONG, key, "bytes");
                }
            }
        }
    }

    private static EnumCalibration storageTypeCalibration() {
        EnumCalibration calibration = new EnumCalibration();
        calibration.setDefaultValue("Unknown");
        calibration.setApplicability(null);
        calibration.setPoints(new LinkedList<>());
        calibration.getPoints().add(new EnumCalibrationPoint(1, "Other"));
        calibration.getPoints().add(new EnumCalibrationPoint(2, "RAM"));
        calibration.getPoints().add(new EnumCalibrationPoint(3, "Virtual Memory"));
        calibration.getPoints().add(new EnumCalibrationPoint(4, "Fixed Disk"));
        calibration.getPoints().add(new EnumCalibrationPoint(5, "Removable Disk"));
        calibration.getPoints().add(new EnumCalibrationPoint(6, "Floppy Disk"));
        calibration.getPoints().add(new EnumCalibrationPoint(7, "Compact Disk"));
        calibration.getPoints().add(new EnumCalibrationPoint(8, "RAM Disk"));
        return calibration;
    }

    private void addParameter(GroupConfiguration gc, String parent, String name, OidEntryType type, OID key, String unit, CalibrationDefinition calibration) {
        // Add mapping to group
        gc.getOidEntryList().add(new OidEntry(key.format(), parent + "." + name, type));
        // Add parameter to model
        ParameterProcessingDefinition ppd = new ParameterProcessingDefinition();
        ppd.setUnit(unit);
        ppd.setRawType(type.toValueTypeEnum());
        ppd.setEngineeringType(type.toValueTypeEnum());
        ppd.setId(externalIdStart++);
        ppd.setLocation(parent + "." + name);
        if(calibration != null) {
            ppd.setCalibrations(new LinkedList<>(Collections.singletonList(calibration)));
            if(calibration instanceof EnumCalibration) {
                ppd.setEngineeringType(ValueTypeEnum.CHARACTER_STRING);
            }
        }
        processingDefinition.getParameterDefinitions().add(ppd);
    }

    private void addParameter(GroupConfiguration gc, String parent, String name, OidEntryType type, OID key, String unit) {
        addParameter(gc, parent, name, type, key, unit, null);
    }

    private void addParameter(GroupConfiguration gc, String parent, String name, OidEntryType type, OID key) {
        addParameter(gc, parent, name, type, key, null, null);
    }

    private void processNetwork(CommunityTarget<UdpAddress> target) {
        String groupName = "Network";
        GroupConfiguration gc = new GroupConfiguration();
        gc.setName(groupName);
        gc.setDistributePdu(false);
        gc.setPollingTime(2 * 60000); // Once every 2 minutes is enough
        this.snmpDeviceConfiguration.getGroupConfigurationList().add(gc);

        // Network table
        TableUtils tUtils = new TableUtils(this.snmp, new DefaultPDUFactory());
        List<TableEvent> events = tUtils.getTable(target, new OID[] { interfaceTableOID }, null, null);
        for (TableEvent event : events) {
            if (event.isError()) {
                System.err.println("Error event when parsing network block");
                continue;
            }
            for (VariableBinding vb : event.getColumns()) {
                OID key = vb.getOid();
                int networkNb = key.get(key.size() - 1);
                // Get the idx
                if (key.startsWith(interfaceDescrOID)) {
                    addParameter(gc, groupName + networkNb, "Description", OidEntryType.STRING, key);
                } else if (key.startsWith(interfaceMacOID)) {
                    addParameter(gc, groupName + networkNb, "MAC", OidEntryType.STRING, key);
                } else if (key.startsWith(interfaceSpeedOID)) {
                    addParameter(gc, groupName + networkNb, "Speed", OidEntryType.LONG, key);
                } else if (key.startsWith(interfaceTypeOID)) {
                    addParameter(gc, groupName + networkNb, "Type", OidEntryType.INTEGER, key); // TODO: add calibration
                } else if (key.startsWith(interfaceAdminOID)) {
                    addParameter(gc, groupName + networkNb, "Admin_Status", OidEntryType.INTEGER, key, null, networkStatusCalibration()); // 0: down; 1: up
                } else if (key.startsWith(interfaceOperOID)) {
                    addParameter(gc, groupName + networkNb, "Operational_Status", OidEntryType.INTEGER, key, null, networkStatusCalibration());  // 0: down; 1: up
                } else if (key.startsWith(interfaceInOctOID)) {
                    addParameter(gc, groupName + networkNb, "In_Octets", OidEntryType.LONG, key, "bytes");
                } else if (key.startsWith(interfaceInErrorsOID)) {
                    addParameter(gc, groupName + networkNb, "In_Errors", OidEntryType.LONG, key, "bytes");
                } else if (key.startsWith(interfaceInDiscardOID)) {
                    addParameter(gc, groupName + networkNb, "In_Discards", OidEntryType.LONG, key, "bytes");
                } else if (key.startsWith(interfaceInUnknownOID)) {
                    addParameter(gc, groupName + networkNb, "In_Unknown", OidEntryType.LONG, key, "bytes");
                } else if (key.startsWith(interfaceOutOctOID)) {
                    addParameter(gc, groupName + networkNb, "Out_Octets", OidEntryType.LONG, key, "bytes");
                } else if (key.startsWith(interfaceOutDiscardsOID)) {
                    addParameter(gc, groupName + networkNb, "Out_Discards", OidEntryType.LONG, key, "bytes");
                } else if (key.startsWith(interfaceOutErrorsOID)) {
                    addParameter(gc, groupName + networkNb, "Out_Errors", OidEntryType.LONG, key, "bytes");
                }
            }
        }
    }

    private static EnumCalibration networkStatusCalibration() {
        EnumCalibration calibration = new EnumCalibration();
        calibration.setDefaultValue("Unknown");
        calibration.setApplicability(null);
        calibration.setPoints(new LinkedList<>());
        calibration.getPoints().add(new EnumCalibrationPoint(0, "Down"));
        calibration.getPoints().add(new EnumCalibrationPoint(1, "Up"));
        return calibration;
    }

    private void processDevices(CommunityTarget<UdpAddress> target) {
        String groupName = "CPU";
        GroupConfiguration gc = new GroupConfiguration();
        gc.setName(groupName);
        gc.setDistributePdu(false);
        gc.setPollingTime(60000); // Once per minute
        this.snmpDeviceConfiguration.getGroupConfigurationList().add(gc);

        // Processor table
        TableUtils tUtils = new TableUtils(this.snmp, new DefaultPDUFactory());
        List<TableEvent> events = tUtils.getTable(target, new OID[] { processorTableOID }, null, null);
        for (TableEvent event : events) {
            if (event.isError()) {
                System.err.println("Error event when parsing processor block");
                continue;
            }
            int processorNb = 0;
            for (VariableBinding vb : event.getColumns()) {
                OID key = vb.getOid();
                if (key.startsWith(processorLoadOID)) {
                    addParameter(gc, groupName + processorNb, "Load", OidEntryType.LONG, key);
                }
                ++processorNb;
            }
        }

        groupName = "Disk";
        gc = new GroupConfiguration();
        gc.setName(groupName);
        gc.setDistributePdu(false);
        gc.setPollingTime(60 * 60000); // Once every hour, this value never changes
        this.snmpDeviceConfiguration.getGroupConfigurationList().add(gc);

        // Disk table
        events = tUtils.getTable(target, new OID[] { diskStorageTableOID }, null, null);

        for (TableEvent event : events) {
            if (event.isError()) {
                System.err.println("Error event when parsing disk block");
                continue;
            }
            int diskNb = 0;
            for (VariableBinding vb : event.getColumns()) {
                OID key = vb.getOid();
                if (key.startsWith(diskCapacityOID)) {
                    addParameter(gc, groupName + diskNb, "Capacity", OidEntryType.LONG, key, "KB");
                }
                ++diskNb;
            }
        }
    }

    private void processSystem(CommunityTarget<UdpAddress> target) {
        String groupName = "System";
        GroupConfiguration gc = new GroupConfiguration();
        gc.setName(groupName);
        gc.setDistributePdu(false);
        gc.setPollingTime(60000); // Once per minute
        this.snmpDeviceConfiguration.getGroupConfigurationList().add(gc);

        addParameter(gc, groupName, "Uptime", OidEntryType.LONG, systemUptimeOID, "seconds");
        addParameter(gc, groupName, "Date", OidEntryType.STRING, systemDateOID, null);
        addParameter(gc, groupName, "Nb_Users", OidEntryType.LONG, numUsersOID, null);
        addParameter(gc, groupName, "Description", OidEntryType.STRING, systemDescrOID, null);
        addParameter(gc, groupName, "Name", OidEntryType.STRING, systemNameOID, null);
        addParameter(gc, groupName, "Memory", OidEntryType.LONG, memorySizeOID, "bytes");
    }

    private CommunityTarget<UdpAddress> buildTarget(String snmpConnectionUrl, String community) {
        CommunityTarget<UdpAddress> target = new CommunityTarget<>();
        UdpAddress targetAddress = new UdpAddress(snmpConnectionUrl);
        target.setCommunity(new OctetString(community));
        target.setAddress(targetAddress);
        target.setRetries(2);
        target.setTimeout(5000);
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

    public static void main(String[] args) throws IOException {
        if(args.length != 4) {
            System.err.println("Usage: BasicComputerDriverGenerator <connection URL> <community name> <path prefix> <first external ID>");
            System.err.println("- <connection URL> e.g. 192.168.0.1/161 (UDP protocol used)");
            System.err.println("- <community name> must be provided;");
            System.err.println("- <path prefix> is the location prefix to be used for processing model parameters (e.g. \"SYSTEM.SERVERS\").");
            System.err.println("- <first external ID> is the first ID to be used when creating processing model parameters.");
            System.exit(1);
        }
        String connectionUrl = args[0];
        String community = args[1];
        String pathPrefix = args[2];
        int firstExternalId = Integer.parseInt(args[3]);
        BasicComputerDriverGenerator generator = new BasicComputerDriverGenerator();
        generator.export(connectionUrl, community, firstExternalId, pathPrefix);
        // Files are generated in the current working directory
    }
}
