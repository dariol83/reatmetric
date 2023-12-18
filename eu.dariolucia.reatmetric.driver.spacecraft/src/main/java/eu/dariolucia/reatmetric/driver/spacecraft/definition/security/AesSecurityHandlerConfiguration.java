/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.spacecraft.definition.security;

import eu.dariolucia.reatmetric.api.value.StringUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

@XmlRootElement(name = "aes-security-handler", namespace = "http://dariolucia.eu/reatmetric/driver/spacecraft/security/aes")
@XmlAccessorType(XmlAccessType.FIELD)
public class AesSecurityHandlerConfiguration {

    public static AesSecurityHandlerConfiguration load(InputStream is) throws IOException {
        try {
            JAXBContext jc = JAXBContext.newInstance(AesSecurityHandlerConfiguration.class);
            Unmarshaller u = jc.createUnmarshaller();
            return (AesSecurityHandlerConfiguration) u.unmarshal(is);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }

    @XmlElement(name = "salt")
    private String salt = null; // Used for encryption and decryption

    @XmlElement(name = "tm-spi", required = true)
    private List<SpiPassword> tmSpis = new LinkedList<>();

    @XmlElement(name = "tc-spi", required = true)
    private List<SpiPassword> tcSpis = new LinkedList<>();

    @XmlAttribute(name = "default-tc-spi")
    private int defaultTcSpi = 0;

    @XmlAttribute(name = "tc-spi-parameter-path")
    private String tcSpiParameterPath; // Point to a ENUMERATED, SIGNED, or UNSIGNED INTEGER parameter, raw/source value is used

    @XmlTransient
    private volatile byte[] saltByteArray; // NOSONAR

    public String getSalt() {
        return salt;
    }

    public byte[] getSaltAsByteArray() {
        if(saltByteArray == null && salt != null) {
            saltByteArray = StringUtil.toByteArray(salt);
        }
        return saltByteArray;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public List<SpiPassword> getTmSpis() {
        return tmSpis;
    }

    public void setTmSpis(List<SpiPassword> tmSpis) {
        this.tmSpis = tmSpis;
    }

    public List<SpiPassword> getTcSpis() {
        return tcSpis;
    }

    public void setTcSpis(List<SpiPassword> tcSpis) {
        this.tcSpis = tcSpis;
    }

    public int getDefaultTcSpi() {
        return defaultTcSpi;
    }

    public void setDefaultTcSpi(int defaultTcSpi) {
        this.defaultTcSpi = defaultTcSpi;
    }

    public String getTcSpiParameterPath() {
        return tcSpiParameterPath;
    }

    public void setTcSpiParameterPath(String tcSpiParameterPath) {
        this.tcSpiParameterPath = tcSpiParameterPath;
    }
}
