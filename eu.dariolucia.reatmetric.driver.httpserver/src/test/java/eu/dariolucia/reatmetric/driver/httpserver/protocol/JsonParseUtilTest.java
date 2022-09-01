package eu.dariolucia.reatmetric.driver.httpserver.protocol;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonParseUtilTest {

    @Test
    void parseParameterDataFilter() {
        String toParse1 = "{ " +
                "\"parentPath\" : null, " +
                "\"parameterPathList\" : [\"PARAM1\", \"PARAM2\"], " +
                "\"routeList\" : null, " +
                "\"validityList\" : [\"VALID\", \"INVALID\"], " +
                "\"alarmStateList\" : [], " +
                "\"externalIdList\" : [123, 124, 12] " +
                "}";
        ParameterDataFilter pdf = JsonParseUtil.parseParameterDataFilter(new ByteArrayInputStream(toParse1.getBytes(StandardCharsets.UTF_8)));

        assertNull(pdf.getParentPath());
        assertTrue(pdf.getParameterPathList().contains(SystemEntityPath.fromString("PARAM1")));
        assertTrue(pdf.getParameterPathList().contains(SystemEntityPath.fromString("PARAM2")));
        assertEquals(2, pdf.getParameterPathList().size());
        assertNull(pdf.getRouteList());
        assertEquals(2, pdf.getValidityList().size());
        assertEquals(0, pdf.getAlarmStateList().size());
        assertEquals(3, pdf.getExternalIdList().size());
        assertTrue(pdf.getExternalIdList().contains(123));
        assertTrue(pdf.getExternalIdList().contains(124));
        assertTrue(pdf.getExternalIdList().contains(12));
    }

    @Test
    void parseMapObject() {
        String toParse1 = "[ { \"key1\": \"test1\" }, { \"key2\": 123 }, { \"key3\": 12.343 }, { \"key4\": true } ]";
        Map<String, Object> result = JsonParseUtil.parseMapInput(new ByteArrayInputStream(toParse1.getBytes(StandardCharsets.UTF_8)));
        assertEquals("test1", result.get("key1"));
        assertEquals(123, result.get("key2"));
        assertEquals(12.343, result.get("key3"));
        assertEquals(true, result.get("key4"));
    }
}