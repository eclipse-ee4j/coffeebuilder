package org.eclipse.coffeebuilder.util;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HttpUtilTest {
    @Mock
    private Log mockLog;

    @Test
    public void testGetContent() throws IOException {
        // Mock the converter function
        Function<String, String> converter = mock(Function.class);
        when(converter.apply(any(String.class))).thenReturn("mocked response");

        // Mock the HttpUtil.Parameter
        HttpUtil.Parameter param = new HttpUtil.Parameter("key", "value");

        // Call the method
        String result = HttpUtil.getContent(mockLog, "http://example.com", converter, param);

        // Verify the result
        assertEquals("mocked response", result);
    }

    @Nested
    @DisplayName("Parameter record")
    class ParameterRecord {

        @Test
        @DisplayName("should store name and value")
        void storesNameAndValue() {
            HttpUtil.Parameter param = new HttpUtil.Parameter("key", "value");
            assertEquals("key", param.name());
            assertEquals("value", param.value());
        }
    }
}
