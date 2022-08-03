package eu.dariolucia.reatmetric.driver.httpserver.protocol;

import eu.dariolucia.reatmetric.api.model.SystemEntityPath;
import eu.dariolucia.reatmetric.api.parameters.ParameterDataFilter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

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
}