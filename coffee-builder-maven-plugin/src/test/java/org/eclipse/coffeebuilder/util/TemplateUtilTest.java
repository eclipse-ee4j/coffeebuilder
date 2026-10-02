package org.eclipse.coffeebuilder.util;

import freemarker.template.TemplateException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TemplateUtilTest {

    @TempDir
    Path tempDirectory;

    @Test
    void renderingFailureIsPropagatedWithoutWritingPartialFile() {
        var output = tempDirectory.resolve("Broken.java");

        IOException exception = assertThrows(IOException.class,
            () -> TemplateUtil.getInstance().createJavaBeanFile(mock(Log.class), Map.of(), output));

        assertInstanceOf(TemplateException.class, exception.getCause());
        assertFalse(Files.exists(output));
    }
}
