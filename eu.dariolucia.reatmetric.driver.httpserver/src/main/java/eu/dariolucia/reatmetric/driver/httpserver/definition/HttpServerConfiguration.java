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

package eu.dariolucia.reatmetric.driver.httpserver.definition;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.io.InputStream;

@XmlRootElement(name = "httpserver", namespace = "http://dariolucia.eu/reatmetric/driver/httpserver")
@XmlAccessorType(XmlAccessType.FIELD)
public class HttpServerConfiguration {

    public static HttpServerConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(HttpServerConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            return (HttpServerConfiguration) u.unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlAttribute(name = "host", required = true)
    private String host;

    @XmlAttribute(name = "port")
    private int port = 8080;

    @XmlAttribute(name = "https")
    private boolean https = false;

    @XmlAttribute(name = "keystore-password")
    private String keyStorePassword = "";

    @XmlAttribute(name = "keystore-type")
    private String keyStoreType = "JKS";

    @XmlAttribute(name = "keystore-location")
    private String keyStoreLocation = "";

    @XmlAttribute(name = "keymanager-password")
    private String keyManagerPassword = "";

    @XmlAttribute(name = "keymanager-algorithm")
    private String keyManagerAlgorithm = "SunX509";

    @XmlAttribute(name = "trustmanager-algorithm")
    private String trustManagerAlgorithm = "SunX509";

    @XmlAttribute(name = "ssl-protocol")
    private String sslProtocol = "TLS";

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

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyManagerPassword() {
        return keyManagerPassword;
    }

    public void setKeyManagerPassword(String keyManagerPassword) {
        this.keyManagerPassword = keyManagerPassword;
    }

    public String getKeyStoreType() {
        return keyStoreType;
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    public String getKeyStoreLocation() {
        return keyStoreLocation;
    }

    public void setKeyStoreLocation(String keyStoreLocation) {
        this.keyStoreLocation = keyStoreLocation;
    }

    public String getKeyManagerAlgorithm() {
        return keyManagerAlgorithm;
    }

    public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
        this.keyManagerAlgorithm = keyManagerAlgorithm;
    }

    public String getTrustManagerAlgorithm() {
        return trustManagerAlgorithm;
    }

    public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
        this.trustManagerAlgorithm = trustManagerAlgorithm;
    }

    public String getSslProtocol() {
        return sslProtocol;
    }

    public void setSslProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }
}
