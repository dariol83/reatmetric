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
import java.util.Map;

public class ChartPreset implements IPreset {
    private final List<Element> elements = new LinkedList<>();

    @Override
    public void load(InputStream is) {
        DocumentContext parsed = JsonPath.parse(is);
        List<Map<String, Object>> data = parsed.read("$[*]");
        for(Map<String, Object> e : data) {
            addElement(e);
        }
    }

    @Override
    public void save(PrintStream os) {
        os.println("[");
        int idx = 0;
        for(Element e : elements) {
            os.printf("{ type: \"%s\", names: %s }", e.getType(), e.getNamesAsJSON());
            if(idx != elements.size() - 1) {
                os.println(", ");
            } else {
                os.println();
            }
            ++idx;
        }
        os.println("]");
    }

    public void addElement(String type, String... names) {
        elements.add(new Element(type, names));
    }

    private void addElement(Map<String, Object> e) {
        String type = (String) e.get("type");
        List<String> names = (List<String>) e.get("names");
        this.elements.add(new Element(type, names.toArray(new String[0])));
    }

    public List<Element> getElements() {
        return elements;
    }

    public static class Element {
        private final String type;
        private final String[] names;

        public Element(String type, String[] names) {
            this.type = type;
            this.names = names;
        }

        public String getType() {
            return type;
        }

        public String[] getNames() {
            return names;
        }

        public String getNamesAsJSON() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            int idx = 0;
            for(String n : names) {
                sb.append("\"").append(n).append("\"");
                if(idx != names.length - 1) {
                    sb.append(", ");
                } else {
                    sb.append(" ");
                }
                ++idx;
            }
            sb.append("]");

            return sb.toString();
        }
    }
}
