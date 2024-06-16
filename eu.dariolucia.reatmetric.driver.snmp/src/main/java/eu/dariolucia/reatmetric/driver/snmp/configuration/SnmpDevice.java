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

@XmlAccessorType(XmlAccessType.FIELD)
public class SnmpDevice {

    @XmlAttribute(name = "name", required = true)
    private String name;

    @XmlAttribute(name = "host", required = true)
    private String host;

    @XmlAttribute(name = "port")
    private int port = 161;

    @XmlAttribute(name = "user")
    private String user;

    @XmlAttribute(name = "password")
    private String password;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    @XmlTransient
    private SnmpDeviceConfiguration deviceConfiguration = null;

    public void initialise() {
        try {
            this.deviceConfiguration = SnmpDeviceConfiguration.load(new FileInputStream(getConfiguration()));
        } catch (IOException e) {
            // TODO: log
            e.printStackTrace();
        }
    }

    public SnmpDeviceConfiguration getDeviceConfiguration() {
        return deviceConfiguration;
    }
}
