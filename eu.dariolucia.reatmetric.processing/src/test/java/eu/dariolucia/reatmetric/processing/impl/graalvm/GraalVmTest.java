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

package eu.dariolucia.reatmetric.processing.impl.graalvm;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

public class GraalVmTest {

    @Test
    void testGraalVM() {
        String script = "function eval() { if(AAAP0004 == \"PRESENT\") return 0; else return 1; } \n" +
                "eval();";
        try (Engine engine = Engine.create()) {
            Source source = Source.create("js", script);
            try (Context context = Context.newBuilder()
                    .engine(engine)
                    .build()) {
                Value bindings = context.getBindings("js");
                bindings.putMember("AAAP0004", "OFF");
                Value returnValue = context.eval(source);
                Object o = returnValue.as(Object.class);
                System.out.println(o);

                bindings.putMember("AAAP0004", "PRESENT");
                returnValue = context.eval(source);
                o = returnValue.as(Object.class);
                System.out.println(o);
            }
            try (Context context = Context.newBuilder()
                    .engine(engine)
                    .build()) {
                Value bindings = context.getBindings("js");
                bindings.putMember("AAAP0004", "PRESENT");
                Value returnValue = context.eval(source);
                Object o = returnValue.as(Object.class);
                System.out.println(o);
            }
        }
    }
}
