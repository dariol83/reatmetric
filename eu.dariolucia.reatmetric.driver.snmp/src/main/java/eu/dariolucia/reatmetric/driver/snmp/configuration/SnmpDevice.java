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

package eu.dariolucia.reatmetric.driver.snmp.configuration;

import jakarta.xml.bind.annotation.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@XmlAccessorType(XmlAccessType.FIELD)
public class SnmpDevice {

    private static final Logger LOG = Logger.getLogger(SnmpDevice.class.getName());

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "connection-string", required = true)
    private String connectionString; // example: udp:192.168.0.1/161

    @XmlAttribute(name = "timeout")
    private int timeout = 2000; // in ms

    @XmlAttribute(name = "retries")
    private int retries = 2;

    // TODO: security model missing, enum for level missing
    @XmlAttribute(name = "security-name")
    private String securityName;

    @XmlAttribute(name = "security-level")
    private String securityLevel;

    @XmlAttribute(name = "community")
    private String community = "public";

    @XmlAttribute(name = "version")
    private SnmpVersionEnum version = SnmpVersionEnum.V3;

    @XmlAttribute(name = "path", required = true)
    private String path;

    @XmlElement(name="configuration", required = true)
    private String configuration;

    @XmlElement(name="set-command", required = true)
    private SetCommandConfiguration setCommandConfiguration;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getSecurityName() {
        return securityName;
    }

    public void setSecurityName(String securityName) {
        this.securityName = securityName;
    }

    public String getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(String securityLevel) {
        this.securityLevel = securityLevel;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public SnmpVersionEnum getVersion() {
        return version;
    }

    public void setVersion(SnmpVersionEnum version) {
        this.version = version;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public SetCommandConfiguration getSetCommandConfiguration() {
        return setCommandConfiguration;
    }

    public void setSetCommandConfiguration(SetCommandConfiguration setCommandConfiguration) {
        this.setCommandConfiguration = setCommandConfiguration;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    @XmlTransient
    private SnmpDeviceConfiguration deviceConfiguration = null;

    public void initialise() {
        try {
            this.deviceConfiguration = SnmpDeviceConfiguration.load(new FileInputStream(getConfiguration()));
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Cannot initialise device configuration file " + getConfiguration() + ": " + e.getMessage(), e);
        }
    }

    public SnmpDeviceConfiguration getDeviceConfiguration() {
        return deviceConfiguration;
    }
}
