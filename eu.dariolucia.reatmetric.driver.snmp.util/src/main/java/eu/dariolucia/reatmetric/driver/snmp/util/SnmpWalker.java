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

/*
 * Original work: https://github.com/micmiu/snmp-tutorial/blob/master/snmp4j-1x-demo/src/main/java/com/micmiu/snmp4j/demo1x/SnmpWalk.java
 * Released under Apache License 2.0.
 * Author: Michael
 *
 */
package eu.dariolucia.reatmetric.driver.snmp.util;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

public class SnmpWalker {

    public static void snmpWalk(String ip, String community, String targetOid) {
        Address address = GenericAddress.parse("udp:" + ip + "/" + 161);
        CommunityTarget<Address> target = new CommunityTarget<>();
        target.setCommunity(new OctetString(community));
        target.setAddress(address);
        target.setVersion(SnmpConstants.version2c);
        target.setTimeout(5000); // milliseconds
        target.setRetries(3);

        Snmp snmp = null;
        try(TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping()) {
            snmp = new Snmp(transport);
            transport.listen();

            PDU pdu = new PDU();
            OID targetOID = new OID(targetOid);
            pdu.add(new VariableBinding(targetOID));

            boolean finished = false;
            while (!finished) {
                VariableBinding vb = null;
                ResponseEvent<Address> respEvent = snmp.getNext(pdu, target);
                PDU response = respEvent.getResponse();
                if (null == response) {
                    break;
                } else {
                    vb = response.get(0);
                }
                // check finish
                finished = walkFinished(targetOID, pdu, vb);
                if (!finished) {
                    System.out.println(vb.getOid() + " (" + vb.getVariable().getSyntaxString() + ") =  " + vb.getVariable());
                    pdu.setRequestID(new Integer32(0));
                    pdu.set(0, vb);
                } else {
                    snmp.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (snmp != null) {
                try {
                    snmp.close();
                } catch (IOException ex1) {
                    snmp = null;
                }
            }
        }
    }

    private static boolean walkFinished(OID targetOID, PDU pdu,
                                        VariableBinding vb) {
        boolean finished = false;
        if (pdu.getErrorStatus() != 0) {
            System.out.println("responsePDU.getErrorStatus() != 0");
            System.out.println(pdu.getErrorStatusText());
            finished = true;
        } else if (vb.getOid() == null) {
            System.out.println("vb.getOid() == null");
            finished = true;
        } else if (vb.getOid().size() < targetOID.size()) {
            System.out.println("vb.getOid().size() < targetOID.size()");
            finished = true;
        } else if (targetOID.leftMostCompare(targetOID.size(), vb.getOid()) != 0) {
            System.out.println("targetOID.leftMostCompare() != 0");
            finished = true;
        } else if (Null.isExceptionSyntax(vb.getVariable().getSyntax())) {
            System.out
                    .println("Null.isExceptionSyntax(vb.getVariable().getSyntax())");
            finished = true;
        } else if (vb.getOid().compareTo(targetOID) <= 0) {
            System.out.println("[true] Variable received is not successor of requested one:");
            System.out.println(vb + " <= " + targetOID);
            finished = true;
        }
        return finished;
    }

    public static void main(String[] args) {
        if(args.length != 3) {
            System.err.println("Usage: SnmpWalker <host> <community name> <starting OID>");
            System.err.println("- <host> can be a name or IP address. UDP on port 161 is used by default;");
            System.err.println("- <community name> must be provided;");
            System.err.println("- <starting OID> is the starting OID (e.g. \"1.3.6.1.2.1\").");
            System.exit(1);
        }
        String ip = args[0];
        String community = args[1];
        String targetOid = args[2];
        SnmpWalker.snmpWalk(ip, community, targetOid);
    }
}
