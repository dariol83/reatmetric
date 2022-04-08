/*
 * Copyright (c)  2022 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.remoting.util;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.util.Objects;

public class SimpleRMIClientSocketFactory implements RMIClientSocketFactory, Serializable {

    private final String localAddress;

    public SimpleRMIClientSocketFactory(String localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return new Socket(host, port, InetAddress.getByName(localAddress), 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleRMIClientSocketFactory that = (SimpleRMIClientSocketFactory) o;
        return Objects.equals(localAddress, that.localAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localAddress);
    }
}
