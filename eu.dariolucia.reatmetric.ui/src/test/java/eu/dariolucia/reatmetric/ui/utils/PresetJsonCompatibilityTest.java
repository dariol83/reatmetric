/*
 * Copyright (c)  2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.ui.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PresetJsonCompatibilityTest {

    @Test
    void chartPresetRoundTripPreservesChartAndNameOrder() {
        ChartPreset original = new ChartPreset();
        original.addElement("line", "Spacecraft.Temperature_\u00b0C", "Spacecraft.Voltage_\u03bcV");
        original.addElement("bar", "Spacecraft.Power");

        ChartPreset restored = new ChartPreset();
        restored.load(savedPreset(original));

        assertEquals(List.of("line", "bar"), restored.getElements().stream().map(ChartPreset.Element::getType).toList());
        assertArrayEquals(new String[] {"Spacecraft.Temperature_\u00b0C", "Spacecraft.Voltage_\u03bcV"}, restored.getElements().get(0).getNames());
        assertArrayEquals(new String[] {"Spacecraft.Power"}, restored.getElements().get(1).getNames());
    }

    @Test
    void andPresetRoundTripPreservesHeadingsPathsAndIntegerIds() {
        AndPreset original = new AndPreset();
        original.addElement(AndPreset.TYPE_HEADING, "T\u00e9l\u00e9m\u00e9trie", 0);
        original.addElement(AndPreset.TYPE_ELEMENT, "Spacecraft.Temperature_\u00b0C", 17);
        original.addElement(AndPreset.TYPE_ELEMENT, "Spacecraft.Counter", Integer.MAX_VALUE);

        AndPreset restored = new AndPreset();
        restored.load(savedPreset(original));

        assertEquals(List.of(AndPreset.TYPE_HEADING, AndPreset.TYPE_ELEMENT, AndPreset.TYPE_ELEMENT),
                restored.getElements().stream().map(AndPreset.Element::getType).toList());
        assertEquals(List.of("T\u00e9l\u00e9m\u00e9trie", "Spacecraft.Temperature_\u00b0C", "Spacecraft.Counter"),
                restored.getElements().stream().map(AndPreset.Element::getPath).toList());
        assertEquals(List.of(0, 17, Integer.MAX_VALUE), restored.getElements().stream().map(AndPreset.Element::getId).toList());
    }

    @Test
    void propertyKeyPresetRoundTripPreservesOrderedUnicodeKeys() {
        List<String> keys = List.of("route", "op\u00e9rateur", "priority", "\u6e29\u5ea6");
        PropertyKeyPreset original = new PropertyKeyPreset();
        original.setItems(keys);

        PropertyKeyPreset restored = new PropertyKeyPreset();
        restored.load(savedPreset(original));

        assertEquals(keys, restored.getItems());
    }

    private static InputStream savedPreset(IPreset preset) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            preset.save(output);
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }
}
