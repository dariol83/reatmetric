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

package eu.dariolucia.reatmetric.driver.test;

import eu.dariolucia.reatmetric.api.common.Pair;
import eu.dariolucia.reatmetric.api.processing.IActivityHandler;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandEncoder {

    private final AtomicInteger commandTagSequencer = new AtomicInteger(0);

    public Pair<Integer, byte[]> encode(IActivityHandler.ActivityInvocation activityInvocation) {
        // Read the activityInvocation arguments and encode accordingly
        int eqId = (Integer) activityInvocation.getArguments().get(TestDriver.EQUIPMENT_ID_ARGKEY);
        int commandId = (Integer) activityInvocation.getArguments().get(TestDriver.COMMAND_ID_ARGKEY);
        Number arg1 = (Number) activityInvocation.getArguments().get(TestDriver.ARG_1_ARGKEY);
        if(arg1 == null) {
            arg1 = -1;
        }
        Number arg2 = (Number) activityInvocation.getArguments().get(TestDriver.ARG_2_ARGKEY);
        if(arg2 == null) {
            arg2 = -1;
        }
        Number arg3 = (Number) activityInvocation.getArguments().get(TestDriver.ARG_3_ARGKEY);
        if(arg3 == null) {
            arg3 = -1;
        }
        byte firstByte = (byte) eqId;
        firstByte <<= 4;
        firstByte |= 0x0F;
        ByteBuffer cmdBuffer = ByteBuffer.allocate(21);
        cmdBuffer.put(firstByte);
        cmdBuffer.putInt(commandId);
        int tag = commandTagSequencer.incrementAndGet();
        cmdBuffer.putInt(tag);
        cmdBuffer.putInt(arg1.intValue());
        cmdBuffer.putInt(arg2.intValue());
        cmdBuffer.putInt(arg3.intValue());
        return Pair.of(tag, cmdBuffer.array());
    }
}
