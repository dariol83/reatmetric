/*
 * Copyright (c) 2026 Dario Lucia (https://www.dariolucia.eu)
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

package eu.dariolucia.reatmetric.driver.httpserver.protocol;

import com.jayway.jsonpath.InvalidJsonException;
import com.jayway.jsonpath.PathNotFoundException;
import eu.dariolucia.reatmetric.api.common.LongUniqueId;
import eu.dariolucia.reatmetric.api.model.AlarmState;
import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterData;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import eu.dariolucia.reatmetric.api.parameters.Validity;
import eu.dariolucia.reatmetric.api.rawdata.RawDataFilter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonInputConfidenceTest {

    private static final String CLEAR_PARAMETER_FILTER = """
            {"parentPath": null, "parameterPathList": null, "routeList": null,
             "validityList": null, "alarmStateList": null, "externalIdList": null}
            """;

    @Test
    void inputDistinguishesExplicitNullFromMissingProperty() {
        assertNull(JsonParseUtil.parseInput(body("{\"input\": null}"), Object.class));
        assertThrows(PathNotFoundException.class, () -> JsonParseUtil.parseInput(body("{}"), Object.class));
    }

    @Test
    void malformedJsonIsRejected() {
        assertThrows(InvalidJsonException.class, () -> JsonParseUtil.parseInput(body("{"), Object.class));
    }

    @Test
    void mapInputPreservesNumbersUnicodeAndNullAndMergesInOrder() {
        Map<String, Object> result = JsonParseUtil.parseMapInput(body("""
                [{"small": 7, "large": 4294967296, "real": 12.5, "enabled": true,
                  "label": "caffè 🚀", "optional": null, "replacement": "before"},
                 {"replacement": "after"}]
                """));

        assertEquals(7, result.get("small"));
        assertEquals(4294967296L, result.get("large"));
        assertEquals(12.5, result.get("real"));
        assertEquals(true, result.get("enabled"));
        assertEquals("caffè 🚀", result.get("label"));
        assertTrue(result.containsKey("optional"));
        assertNull(result.get("optional"));
        assertEquals("after", result.get("replacement"));
        assertEquals(7, result.size());
    }

    @Test
    void nullParameterCriteriaAcceptDataAndEmptyCriteriaRejectData() {
        ParameterData item = new ParameterData(new LongUniqueId(1), Instant.EPOCH, 12, "TEMP",
                SystemEntityPath.fromString("ROOT.TEMP"), 12.5, 125L, "route-1",
                Validity.VALID, AlarmState.NOMINAL, null, Instant.EPOCH, null);
        ParameterDataFilter clear = JsonParseUtil.parseParameterDataFilter(body(CLEAR_PARAMETER_FILTER));
        ParameterDataFilter empty = JsonParseUtil.parseParameterDataFilter(body(
                CLEAR_PARAMETER_FILTER.replace("\"parameterPathList\": null", "\"parameterPathList\": []")));

        assertTrue(clear.isClear());
        assertTrue(clear.test(item));
        assertFalse(empty.isClear());
        assertFalse(empty.test(item));
    }

    @Test
    void mandatoryFilterPropertiesAndEnumValuesAreValidated() {
        assertThrows(PathNotFoundException.class, () -> JsonParseUtil.parseParameterDataFilter(body("{}")));
        assertThrows(IllegalArgumentException.class, () -> JsonParseUtil.parseParameterDataFilter(body(
                CLEAR_PARAMETER_FILTER.replace("\"validityList\": null", "\"validityList\": [\"UNKNOWN_VALUE\"]"))));
    }

    @Test
    void rawDataLiveSelectionIsOptionalAndPreservesFalse() {
        String filter = """
                {"nameContains": null, "contentSet": true, "routeList": null,
                 "typeList": null, "sourceList": null, "qualityList": null}
                """;
        RawDataFilter unspecified = JsonParseUtil.parseRawDataFilter(body(filter));
        RawDataFilter repatriated = JsonParseUtil.parseRawDataFilter(body(filter.replace("\"nameContains\"", "\"liveSet\": false, \"nameContains\"")));

        assertTrue(unspecified.isWithData());
        assertNull(unspecified.getLive());
        assertEquals(false, repatriated.getLive());
    }

    private static ByteArrayInputStream body(String json) {
        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    }
}
