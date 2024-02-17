/*
 * Copyright (c)  2023 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.ui.utils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class PropertyKeyPreset implements IPreset {
    private final List<String> elements = new LinkedList<>();

    @Override
    public void load(InputStream is) {
        DocumentContext parsed = JsonPath.parse(is);
        List<String> data = parsed.read("$[*]");
        elements.addAll(data);
    }

    @Override
    public void save(PrintStream os) {
        os.println("[");
        int idx = 0;
        for(String e : elements) {
            os.printf("\"%s\"", e);
            if(idx != elements.size() - 1) {
                os.println(", ");
            } else {
                os.println();
            }
            ++idx;
        }
        os.println("]");
    }

    public void setItems(List<String> items) {
        elements.clear();
        elements.addAll(items);
    }

    public List<String> getItems() {
        return elements;
    }

}
