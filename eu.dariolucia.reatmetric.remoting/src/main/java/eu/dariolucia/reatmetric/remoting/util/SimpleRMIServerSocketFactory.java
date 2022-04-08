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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

public class SimpleRMIServerSocketFactory implements RMIServerSocketFactory {

    private final String localAddress;

    public SimpleRMIServerSocketFactory(String localAddress) {
        this.localAddress = localAddress;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return new ServerSocket(port, 50, InetAddress.getByName(localAddress));
    }
}
