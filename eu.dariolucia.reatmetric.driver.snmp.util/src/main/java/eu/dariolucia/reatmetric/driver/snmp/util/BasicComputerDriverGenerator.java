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
import eu.dariolucia.reatmetric.driver.snmp.configuration.SnmpDeviceConfiguration;
import eu.dariolucia.reatmetric.processing.definition.ParameterProcessingDefinition;
import eu.dariolucia.reatmetric.processing.definition.ProcessingDefinition;
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

import java.io.IOException;
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

    private final Snmp snmp;
    private int externalIdStart;
    private String pathPrefix;

    private ProcessingDefinition processingDefinition;
    private SnmpDeviceConfiguration snmpDeviceConfiguration;

    public BasicComputerDriverGenerator() throws IOException {
        TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);
        transport.listen();
    }

    public void export(String snmpConnectionUrl, String community, int externalIdStart, String pathPrefix) {
        CommunityTarget<UdpAddress> target = buildTarget(snmpConnectionUrl, community);
        this.externalIdStart = externalIdStart;
        this.pathPrefix = pathPrefix;
        initialise();
        processSystem(target);
        processDevices(target);
        processStorage(target);
        processNetwork(target);
        finalise();
    }

    private void finalise() {
        // TODO
    }

    private void initialise() {
        processingDefinition = new ProcessingDefinition();
        processingDefinition.setPathPrefix(this.pathPrefix);

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
        int storageNb = 0;
        for (TableEvent event : events) {
            if (event.isError()) {
                System.err.println("Error event when parsing storage block");
                continue;
            }
            for (VariableBinding vb : event.getColumns()) {
                OID key = vb.getOid();
                // Get the idx
                if (key.startsWith(storageTypeOID)) {
                    // Type
                    addParameter(gc, groupName + storageNb, "Type", ValueTypeEnum.SIGNED_INTEGER, key);
                } else if (key.startsWith(storageDescrOID)) {
                    // Description
                    addParameter(gc, groupName + storageNb, "Description", ValueTypeEnum.CHARACTER_STRING, key);
                } else if (key.startsWith(storageAllocUnitOID)) {
                    // Allocation Unit
                    addParameter(gc, groupName + storageNb, "Allocation_Unit", ValueTypeEnum.SIGNED_INTEGER, key);
                } else if (key.startsWith(storageSizeOID)) {
                    // Storage size
                    addParameter(gc, groupName + storageNb, "Storage_Size", ValueTypeEnum.SIGNED_INTEGER, key, "bytes");
                } else if (key.startsWith(storageUsedOID)) {
                    // Storage used
                    addParameter(gc, groupName + storageNb, "Storage_Used", ValueTypeEnum.SIGNED_INTEGER, key, "bytes");
                }
            }
            ++storageNb;
        }
    }

    private void addParameter(GroupConfiguration gc, String parent, String name, ValueTypeEnum type, OID key, String unit) {
        // Add mapping to group
        gc.getOidEntryList().add(new OidEntry(key.format(), parent + "." + name, type));
        // Add parameter to model
        ParameterProcessingDefinition ppd = new ParameterProcessingDefinition();
        ppd.setUnit(unit);
        ppd.setRawType(type);
        ppd.setEngineeringType(type);
        ppd.setId(externalIdStart++);
        ppd.setLocation(parent + "." + name);
        processingDefinition.getParameterDefinitions().add(ppd);
    }

    private void addParameter(GroupConfiguration gc, String parent, String name, ValueTypeEnum type, OID key) {
        addParameter(gc, parent, name, type, key, null);
    }

    private void processNetwork(CommunityTarget<UdpAddress> target) {
        String groupName = "Network";
        GroupConfiguration gc = new GroupConfiguration();
        gc.setName(groupName);
        gc.setDistributePdu(false);
        gc.setPollingTime(2 * 60000); // Once every 2 minutes is enough
        this.snmpDeviceConfiguration.getGroupConfigurationList().add(gc);

        int networkNb = 0;
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
                // Get the idx
                if (key.startsWith(interfaceDescrOID)) {
                    addParameter(gc, groupName + networkNb, "Description", ValueTypeEnum.CHARACTER_STRING, key);
                } else if (key.startsWith(interfaceMacOID)) {
                    addParameter(gc, groupName + networkNb, "MAC", ValueTypeEnum.CHARACTER_STRING, key);
                } else if (key.startsWith(interfaceSpeedOID)) {
                    addParameter(gc, groupName + networkNb, "Speed", ValueTypeEnum.UNSIGNED_INTEGER, key);
                } else if (key.startsWith(interfaceTypeOID)) {
                    addParameter(gc, groupName + networkNb, "Type", ValueTypeEnum.UNSIGNED_INTEGER, key);
                } else if (key.startsWith(interfaceAdminOID)) {
                    addParameter(gc, groupName + networkNb, "Admin_Status", ValueTypeEnum.ENUMERATED, key);
                } else if (key.startsWith(interfaceOperOID)) {
                    addParameter(gc, groupName + networkNb, "Operational_Status", ValueTypeEnum.ENUMERATED, key);
                } else if (key.startsWith(interfaceInOctOID)) {
                    addParameter(gc, groupName + networkNb, "In_Octets", ValueTypeEnum.UNSIGNED_INTEGER, key, "bytes");
                } else if (key.startsWith(interfaceInErrorsOID)) {
                    addParameter(gc, groupName + networkNb, "In_Errors", ValueTypeEnum.UNSIGNED_INTEGER, key, "bytes");
                } else if (key.startsWith(interfaceInDiscardOID)) {
                    addParameter(gc, groupName + networkNb, "In_Discards", ValueTypeEnum.UNSIGNED_INTEGER, key, "bytes");
                } else if (key.startsWith(interfaceInUnknownOID)) {
                    addParameter(gc, groupName + networkNb, "In_Unknown", ValueTypeEnum.UNSIGNED_INTEGER, key, "bytes");
                } else if (key.startsWith(interfaceOutOctOID)) {
                    addParameter(gc, groupName + networkNb, "Out_Octets", ValueTypeEnum.UNSIGNED_INTEGER, key, "bytes");
                } else if (key.startsWith(interfaceOutDiscardsOID)) {
                    addParameter(gc, groupName + networkNb, "Out_Discards", ValueTypeEnum.UNSIGNED_INTEGER, key, "bytes");
                } else if (key.startsWith(interfaceOutErrorsOID)) {
                    addParameter(gc, groupName + networkNb, "Out_Errors", ValueTypeEnum.UNSIGNED_INTEGER, key, "bytes");
                }
            }
            ++networkNb;
        }
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
        int processorNb = 0;
        for (TableEvent event : events) {
            if (event.isError()) {
                System.err.println("Error event when parsing processor block");
                continue;
            }
            for (VariableBinding vb : event.getColumns()) {
                OID key = vb.getOid();
                if (key.startsWith(processorLoadOID)) {
                    addParameter(gc, groupName + processorNb, "Load", ValueTypeEnum.UNSIGNED_INTEGER, key);
                }
            }
            ++processorNb;
        }

        groupName = "Disk";
        gc = new GroupConfiguration();
        gc.setName(groupName);
        gc.setDistributePdu(false);
        gc.setPollingTime(60 * 60000); // Once every hour, this value never changes
        this.snmpDeviceConfiguration.getGroupConfigurationList().add(gc);

        // Disk table
        events = tUtils.getTable(target, new OID[] { diskStorageTableOID }, null, null);
        int diskNb = 0;
        for (TableEvent event : events) {
            if (event.isError()) {
                System.err.println("Error event when parsing disk block");
                continue;
            }
            for (VariableBinding vb : event.getColumns()) {
                OID key = vb.getOid();
                if (key.startsWith(diskCapacityOID)) {
                    addParameter(gc, groupName + diskNb, "Capacity", ValueTypeEnum.SIGNED_INTEGER, key, "KB");
                }
            }
            ++diskNb;
        }
    }

    private void processSystem(CommunityTarget<UdpAddress> target) {
        String groupName = "System";
        GroupConfiguration gc = new GroupConfiguration();
        gc.setName(groupName);
        gc.setDistributePdu(false);
        gc.setPollingTime(60000); // Once per minute
        this.snmpDeviceConfiguration.getGroupConfigurationList().add(gc);

        addParameter(gc, groupName, "Uptime", ValueTypeEnum.UNSIGNED_INTEGER, systemUptimeOID, "seconds");
        addParameter(gc, groupName, "Date", ValueTypeEnum.CHARACTER_STRING, systemDateOID, null);
        addParameter(gc, groupName, "Nb_Users", ValueTypeEnum.UNSIGNED_INTEGER, numUsersOID, null);
        addParameter(gc, groupName, "Description", ValueTypeEnum.CHARACTER_STRING, systemDescrOID, null);
        addParameter(gc, groupName, "Name", ValueTypeEnum.CHARACTER_STRING, systemNameOID, null);
        addParameter(gc, groupName, "Memory", ValueTypeEnum.UNSIGNED_INTEGER, memorySizeOID, "bytes");
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
            System.err.println("- <connection URL> e.g. udp:192.168.0.1/161");
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
