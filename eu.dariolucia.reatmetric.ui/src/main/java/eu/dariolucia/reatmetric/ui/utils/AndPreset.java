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

public class AndPreset {

    public static final int TYPE_HEADING = 2;
    public static final int TYPE_ELEMENT = 1;

    private final List<Element> elements = new LinkedList<>();

    public void load(InputStream is) {
        DocumentContext parsed = JsonPath.parse(is);
        List<Map<String, Object>> data = parsed.read("$[*]");
        for(Map<String, Object> e : data) {
            addElement(e);
        }
    }

    public void save(PrintStream os) {
        os.println("[");
        int idx = 0;
        for(Element e : elements) {
            os.printf("{ type: %d, name: \"%s\", id: %d }", e.getType(), e.getPath(), e.getId());
            if(idx != elements.size() - 1) {
                os.println(", ");
            } else {
                os.println();
            }
            ++idx;
        }
        os.println("]");
    }

    public void addElement(int type, String name, int id) {
        elements.add(new Element(type, name, id));
    }

    private void addElement(Map<String, Object> e) {
        int type = (int) e.get("type");
        String name = (String) e.get("name");
        int id = (int) e.get("id");
        this.elements.add(new Element(type, name, id));
    }

    public List<Element> getElements() {
        return elements;
    }

    public static class Element {
        private final int type;
        private final String path;
        private final int id;

        public Element(int type, String path, int id) {
            this.type = type;
            this.path = path;
            this.id = id;
        }

        public int getType() {
            return type;
        }

        public String getPath() {
            return path;
        }

        public int getId() {
            return id;
        }
    }
}
