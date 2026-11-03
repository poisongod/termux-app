package com.termux.shared.termux.extrakeys.tekl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Runs the shared fixtures from {@code termux-extrakey-lang/conformance} against the Java parser.
 *
 * <p>The same manifest drives the Rust test, so both implementations must agree on what is valid
 * and on the reported line/column. Refresh the resources with
 * {@code scripts/sync-conformance.sh} in the CLI repo.</p>
 */
public class TeklConformanceTest {

    private static final String ROOT = "/tekl-conformance/";

    @Test
    public void fixturesMatchTheSharedManifest() throws Exception {
        String manifest = readText(ROOT + "manifest.tsv");
        int checked = 0;

        for (String raw : manifest.split("\n")) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            String[] cols = raw.split("\t", -1);
            String name = cols[0].trim();
            String expect = cols[1].trim();
            int line = parsePosition(cols, 2);
            int column = parsePosition(cols, 3);
            int rows = cols.length > 4 ? parsePosition(cols, 4) : -1;

            String source = readText(ROOT + "cases/" + name + ".tekl");

            try {
                TeklProgram program = TeklParser.parse(source);
                List<List<TeklKey>> compiled = TeklCompiler.compile(
                    program.rows, Collections.emptyMap(), Collections.emptyMap());
                assertEquals(name + ": expected a parse error", "ok", expect);
                if (rows >= 0) {
                    assertEquals(name + ": wrong row count", rows, compiled.size());
                }
            } catch (TeklException e) {
                // Parse and compile errors are both "rejected"; compile-stage errors have no
                // position, just like the Rust CompileError.
                assertEquals(name + ": expected success but got " + e.getMessage(),
                    "error", expect);
                if (line > 0) {
                    assertEquals(name + ": wrong line (" + e.getMessage() + ")",
                        line, e.getLine());
                    assertEquals(name + ": wrong column (" + e.getMessage() + ")",
                        column, e.getColumn());
                }
            }
            checked++;
        }

        assertTrue("manifest lists no cases", checked > 0);
    }

    private static int parsePosition(String[] cols, int index) {
        if (index >= cols.length) {
            return 0;
        }
        String value = cols[index].trim();
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    private static String readText(String resource) throws IOException {
        try (InputStream in = TeklConformanceTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                fail("missing test resource " + resource
                    + "; run scripts/sync-conformance.sh in termux-extrakey-lang");
                throw new AssertionError("unreachable");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
