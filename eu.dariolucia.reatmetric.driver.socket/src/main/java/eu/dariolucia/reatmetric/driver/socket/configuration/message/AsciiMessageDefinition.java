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

package eu.dariolucia.reatmetric.driver.socket.configuration.message;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.*;

@XmlAccessorType(XmlAccessType.FIELD)
public class AsciiMessageDefinition extends MessageDefinition<String> {

    public static final String VAR_PREFIX = "${{";

    public static final String VAR_POSTFIX = "}}$";

    @XmlElement(name = "template")
    private String template; // This is a template message, something like "asdas asd asd as ${{param1}}$ sad ${{param2}}$" // NOSONAR

    @XmlElement(name = "symbol")
    private List<SymbolTypeFormat> symbols; // Map symbol to type/format for correct formatting

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public List<SymbolTypeFormat> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<SymbolTypeFormat> symbols) {
        this.symbols = symbols;
    }

    /* ***************************************************************
     * Internal operations
     * ***************************************************************/

    private transient final List<String> literals = new ArrayList<>();
    private transient final List<String> variables = new ArrayList<>();
    private transient final Map<String, SymbolTypeFormat> variable2type = new TreeMap<>();

    // TODO: remember symbols with autoincrement

    @Override
    public void initialise() {
        // Sanitize the template to have proper blank characters
        this.template = template.replace("\\n", "\n")
                .replace("\\r", "\r").replace("\\t", "\t");
        int currentStart = 0;
        while(currentStart < template.length()) {
            // Tokenize and build internals
            int varStartIndex = template.indexOf(VAR_PREFIX);
            if (varStartIndex == -1) {
                // Not found: add last literal and go out
                literals.add(template.substring(currentStart));
                return;
            }
            int varEndIndex = template.indexOf(VAR_POSTFIX, varStartIndex);
            // Add the literal before (can be an empty string) and the string name
            literals.add(template.substring(currentStart, varStartIndex));
            String variableName = template.substring(varStartIndex + VAR_PREFIX.length(), varEndIndex);
            currentStart = varEndIndex + VAR_POSTFIX.length();
            variables.add(variableName);
            // Lookup for symbol
            Optional<SymbolTypeFormat> format = getSymbols().stream().filter(o -> o.getName().equals(variableName)).findFirst();
            if(format.isPresent()) {
               variable2type.put(variableName, format.get());
            } else {
                throw new RuntimeException("Cannot find any format configuration for symbol " + variableName);
            }
        }
    }

    @Override
    public Map<String, Object> decode(String id, String messageToProcess) {
        Map<String, Object> valueMap = new LinkedHashMap<>();
        // Start from the literals
        int currentStart = 0;
        int literalIndex = 0;
        int variableIndex = 0;
        while(currentStart < messageToProcess.length()) {
            // Move forward by literal
            currentStart += literals.get(literalIndex).length();
            literalIndex++;
            // Extract from current start to ...
            String valueString = null;
            if(literalIndex < literals.size()) {
                // ... the start of the next literal
                valueString = messageToProcess.substring(currentStart, messageToProcess.indexOf(literals.get(literalIndex)));
            } else {
                // ... the end of the string
                valueString = messageToProcess.substring(currentStart);
            }
            // Update the position in the string
            currentStart += valueString.length();

            // Process the read value
            if(variableIndex < variables.size()) {
                String variableName = variables.get(variableIndex++);
                SymbolTypeFormat stf = variable2type.get(variableName);
                Object value = null;
                // Read/parse value
                if (stf != null) {
                    value = stf.parse(valueString);
                    // Add to map
                    valueMap.put(variableName, value);
                } else {
                    // TODO: log and ignore
                }
            }
        }
        return valueMap;
    }

    @Override
    public String identify(String messageToIdentify) {
        int currentStart = 0;
        for(String literal : literals) {
            if(literal.isEmpty()) {
                continue;
            }
            int idx = messageToIdentify.indexOf(literal, currentStart);
            if(idx == -1) {
                return null;
            } else {
                currentStart = idx + literal.length();
            }
        }
        return getId();
    }

    @Override
    public String encode(String id, Map<String, Object> data) {
        String result = template;
        for(Map.Entry<String, Object> e : data.entrySet()) {
            SymbolTypeFormat stf = variable2type.get(e.getKey());
            result = result.replace(VAR_PREFIX + e.getKey() + VAR_POSTFIX, stf.dump(e.getValue()));
        }

        // TODO: apply replacement for symbols with autoincrement

        return result;
    }
}
