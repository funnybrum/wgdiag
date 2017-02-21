package com.brum.wgdiag.command.diag;


import com.brum.wgdiag.command.Command;
import com.brum.wgdiag.command.Processor;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class BasicCommandTest {
    private static final Map<String, String> RESPONSES = new HashMap<String, String>();
    static {
        RESPONSES.put(
            "21 12",
            "61 12 0B 3E 0A DB 08 B7 08 B7 00 00 02 FD 0B A1 02 4A 03 AE 0B BB 01 32 01 2B 00 6F 09 7F 03 A0 00 00");
        RESPONSES.put(
            "21 20",
            "61 20 03 5A 03 3C 03 FE 00 01 00 94 00 4A 01 B3 01 6E 01 0C 03 02 02 66 00 84 00 5C 03 A0 03 03");
        RESPONSES.put(
            "21 22",
            "61 22 0B 43 0A DD 08 B7 08 B7 00 00 00 00 02 43 03 AD 0B CD 03 B8 02 67 02 E4 02 E4 02 85 08 B7 00 0C");
        RESPONSES.put(
            "21 26",
            "61 26 00 00 00 00 00 00 5B 37 7F FF 00 00 2F A0 00 29 00 29 00 29 00 29 00 48 00 23 00 00 0B 41");
        RESPONSES.put(
            "21 28",
            "61 28 02 EE 03 2C 02 EE 02 EE 02 EC 02 EE 02 EE 00 00 00 CC FF 3C 00 68 FF FB FF 94 00 00 F6");
    }

    private void verifyPackage(Package pkg, Map<String, String> expected) {
        Map<String, String> parsed = new HashMap<>();
        Set<String> processedCommands = new HashSet<>();
        Iterator<DiagCommand> cmdIter = pkg.getCommandIterator();

        while (true) {
            DiagCommand cmd = cmdIter.next();
            if (processedCommands.contains(cmd.getRequestCommand())) {
                break;
            }
            processedCommands.add(cmd.getRequestCommand());

            String response = RESPONSES.get(cmd.getRequestCommand());
            cmd.verifyResponse(response);
            parsed.putAll(cmd.parseResponse(response));
        }

        assertEquals(expected.size(), parsed.size());
        for (String key : expected.keySet()) {
            assertTrue(parsed.keySet().contains(key));
            assertEquals("Failed to validate " + key, expected.get(key), parsed.get(key));
        }
    }

    @Test
    public void testMAFParser() throws Exception {
        Map<String, String> expected = new HashMap<>();
        expected.put("maf_actual", "435");
        expected.put("maf_spec", "366");

        verifyPackage(Packages.MAF_PACKAGE, expected);
    }

    @Test
    public void testRailPressureParser() throws Exception {
        Map<String, String> expected = new HashMap<>();
        expected.put("rail_actual", "300.3 bar");
        expected.put("rail_spec", "302.1 bar");

        verifyPackage(Packages.RAIL_PRESSURE_PACKAGE, expected);
    }

    @Test
    public void testMAPParser() throws Exception {
        Map<String, String> expected = new HashMap<>();
        expected.put("map_actual", "942 mbar");
        expected.put("map_spec", "941 mbar");

        verifyPackage(Packages.MAP_PACKAGE, expected);
    }

    @Test
    public void testInjectorCorrectionParser() throws Exception {
        Map<String, String> expected = new HashMap<>();
        expected.put("rpm", "750 rpm");
        expected.put("iq", "8.1 mg");
        expected.put("inj1_cor", "+2.04");
        expected.put("inj2_cor", "-1.96");
        expected.put("inj3_cor", "+1.04");
        expected.put("inj4_cor", "-0.05");
        expected.put("inj5_cor", "-1.08");

        verifyPackage(Packages.INJECTOR_CORRECTION_PACKAGE, expected);
    }

    @Test
    public void testMiscParser() throws Exception {
        Map<String, String> expected = new HashMap<>();
        expected.put("rpm", "750 rpm");
        expected.put("iq", "8.1 mg");
        expected.put("coolant", "15 C");
        expected.put("iat", "5 C");
        expected.put("tps", "30 %");
        expected.put("aap???", "928 mbar");

        verifyPackage(Packages.MISC_PACKAGE, expected);
    }

}