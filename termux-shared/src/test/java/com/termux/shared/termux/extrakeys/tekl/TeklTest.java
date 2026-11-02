package com.termux.shared.termux.extrakeys.tekl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeklTest {

    private static Map<String, String> aliasMap() {
        Map<String, String> map = new HashMap<>();
        map.put("BACKSLASH", "\\");
        map.put("QUOTE", "\"");
        map.put("APOSTROPHE", "'");
        return map;
    }

    private static Map<String, String> displayMap() {
        Map<String, String> map = new HashMap<>();
        map.put("UP", "\u2191");
        map.put("DOWN", "\u2193");
        map.put("LEFT", "\u2190");
        map.put("RIGHT", "\u2192");
        map.put("ENTER", "\u21b2");
        return map;
    }

    private static List<List<TeklKey>> compile(String source, Map<String, String> display,
                                               Map<String, String> aliases) throws TeklException {
        return TeklCompiler.compile(TeklParser.parse(source).rows, display, aliases);
    }

    // ── Parser ──────────────────────────────────────────────────────

    @Test
    public void parsesSimpleKey() throws TeklException {
        TeklProgram program = TeklParser.parse("ESC");
        assertEquals(1, program.rows.size());
        assertEquals(1, program.rows.get(0).size());
        TeklNode node = program.rows.get(0).get(0);
        assertEquals(TeklNode.Kind.KEY, node.kind);
        assertEquals("ESC", node.key);
        assertFalse(node.literal);
        assertFalse(node.special);
    }

    @Test
    public void parsesSpecialKey() throws TeklException {
        TeklNode node = TeklParser.parse("ctrl").rows.get(0).get(0);
        assertEquals(TeklNode.Kind.KEY, node.kind);
        assertTrue(node.special);
    }

    @Test
    public void parsesComboLabelAndPopup() throws TeklException {
        assertEquals(TeklNode.Kind.COMBO,
            TeklParser.parse("CTRL(C)").rows.get(0).get(0).kind);
        assertEquals(TeklNode.Kind.LABEL,
            TeklParser.parse("CTRL:C").rows.get(0).get(0).kind);
        assertEquals(TeklNode.Kind.POPUP,
            TeklParser.parse("ESC.DRAWER").rows.get(0).get(0).kind);
    }

    @Test
    public void parsesRowsSemicolonAndComments() throws TeklException {
        TeklProgram program = TeklParser.parse("# header\nESC;\n\nTAB # trailing\n");
        assertEquals(2, program.rows.size());
        assertEquals(1, program.rows.get(0).size());
        assertEquals(1, program.rows.get(1).size());
    }

    @Test
    public void quotedLiteralKeepsContentVerbatim() throws TeklException {
        TeklNode node = TeklParser.parse("\"\\\\\\\\\"").rows.get(0).get(0);
        assertTrue(node.literal);
        assertEquals("\\\\\\\\", node.key);
    }

    @Test
    public void rejectsDuplicateSpecialKey() {
        TeklException e = assertThrows(TeklException.class,
            () -> TeklParser.parse("CTRL ALT CTRL"));
        assertTrue(e.getMessage().contains("more than once"));
        assertTrue(e.getMessage().contains("row 1"));
    }

    @Test
    public void rejectsUnterminatedString() {
        TeklException e = assertThrows(TeklException.class,
            () -> TeklParser.parse("\"abc"));
        assertTrue(e.getMessage().contains("unterminated string"));
        assertTrue(e.getMessage().contains("line 1"));
    }

    @Test
    public void rejectsUnexpectedCharacter() {
        assertThrows(TeklException.class, () -> TeklParser.parse("ESC &"));
    }

    @Test
    public void rejectsQuotedLabel() {
        TeklException e = assertThrows(TeklException.class,
            () -> TeklParser.parse("ESC:\"x\""));
        assertTrue(e.getMessage().contains("label must not be quoted"));
    }

    @Test
    public void rejectsGlobalPropertyInExtraKeys() {
        TeklException e = assertThrows(TeklException.class,
            () -> TeklParser.parse("height = 2\nESC"));
        assertTrue(e.getMessage().contains("not allowed in extra-keys"));
    }

    // ── Compiler ────────────────────────────────────────────────────

    @Test
    public void compilesPlainKey() throws TeklException {
        TeklKey key = compile("ESC", displayMap(), aliasMap()).get(0).get(0);
        assertEquals("ESC", key.key);
        assertFalse(key.macro);
        assertEquals("ESC", key.display);
        assertNull(key.popup);
    }

    @Test
    public void appliesDisplayMap() throws TeklException {
        TeklKey key = compile("UP", displayMap(), aliasMap()).get(0).get(0);
        assertEquals("UP", key.key);
        assertEquals("\u2191", key.display);
    }

    @Test
    public void appliesAliases() throws TeklException {
        TeklKey key = compile("BACKSLASH", Collections.emptyMap(), aliasMap()).get(0).get(0);
        assertEquals("\\", key.key);
        assertEquals("\\", key.display);
    }

    @Test
    public void compilesMacro() throws TeklException {
        TeklKey key = compile("CTRL(C)", displayMap(), aliasMap()).get(0).get(0);
        assertTrue(key.macro);
        assertEquals("CTRL C", key.key);
        assertEquals("CTRL C", key.display);
    }

    @Test
    public void labelOverridesDisplay() throws TeklException {
        TeklKey key = compile("CTRL(C):copy", displayMap(), aliasMap()).get(0).get(0);
        assertTrue(key.macro);
        assertEquals("CTRL C", key.key);
        assertEquals("copy", key.display);
    }

    @Test
    public void emptyLabelIsAllowed() throws TeklException {
        TeklKey key = compile("ESC:", displayMap(), aliasMap()).get(0).get(0);
        assertEquals("", key.display);
    }

    @Test
    public void compilesPopup() throws TeklException {
        TeklKey key = compile("ESC.DRAWER", displayMap(), aliasMap()).get(0).get(0);
        assertEquals("ESC", key.key);
        assertNotNull(key.popup);
        assertEquals("DRAWER", key.popup.key);
    }

    @Test
    public void unknownIdentifiersKeepTheirCase() throws TeklException {
        assertEquals("fooBar", compile("fooBar", displayMap(), aliasMap()).get(0).get(0).key);
    }

    @Test
    public void rejectsLabelInsideCombo() {
        TeklException e = assertThrows(TeklException.class,
            () -> compile("CTRL(ESC:x)", displayMap(), aliasMap()));
        assertTrue(e.getMessage().contains("only keys allowed"));
    }

    // ── Default layout ──────────────────────────────────────────────

    @Test
    public void defaultLayoutParsesAndCompiles() throws TeklException {
        List<List<TeklKey>> rows = compile(TeklDefaults.DEFAULT_LAYOUT, displayMap(), aliasMap());

        assertEquals(2, rows.size());
        assertEquals(8, rows.get(0).size());
        assertEquals(8, rows.get(1).size());

        assertEquals("ESC", rows.get(0).get(0).key);
        assertEquals("DRAWER", rows.get(0).get(0).popup.key);

        assertEquals("`", rows.get(0).get(1).key);
        assertEquals("~", rows.get(0).get(1).popup.key);

        assertEquals("'", rows.get(0).get(4).key);
        assertEquals("\"", rows.get(0).get(4).popup.key);

        assertEquals("UP", rows.get(0).get(5).key);
        assertEquals("\u2191", rows.get(0).get(5).display);
        assertEquals("HOME", rows.get(0).get(5).popup.key);

        assertEquals("KEYBOARD", rows.get(0).get(7).key);

        assertEquals("\\", rows.get(1).get(3).key);
        assertEquals("|", rows.get(1).get(3).popup.key);

        assertEquals("LEFT", rows.get(1).get(4).key);
        assertEquals("<", rows.get(1).get(4).popup.key);
        assertEquals("DOWN", rows.get(1).get(5).key);
        assertEquals("END", rows.get(1).get(5).popup.key);
        assertEquals("RIGHT", rows.get(1).get(6).key);
        assertEquals(">", rows.get(1).get(6).popup.key);

        assertEquals("ENTER", rows.get(1).get(7).key);
        assertEquals("PASTE", rows.get(1).get(7).popup.key);
    }
}
