package com.brum.wgdiag.command.diag;

import com.brum.wgdiag.command.Command;
import com.brum.wgdiag.command.impl.DiagUtils;
import com.brum.wgdiag.command.diag.impl.PackageImpl;
import com.brum.wgdiag.command.impl.Utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Diagnostic command packages.
 */
public class Packages {

    private static final List<Command> INIT_COMMANDS = Arrays.asList(
        Utils.createCommand("ATZ", "ELM327", 7500),
        Utils.createCommand("ATSP5", "OK"),
        Utils.createCommand("ATWM8115F13E", "OK"),
        Utils.createCommand("ATSH8115F1", "OK"),
        Utils.createCommand("ATFI", "BUS INIT: OK"),
        Utils.createCommand("81", "C1 EF 8F"),
        Utils.createCommand("27 01", "67 01"),
        Utils.createCommand("27 02 CD 46", "7F 27"),
        Utils.createCommand("31 25 00", "71 25")
    );

    static Package MAF_PACKAGE = new PackageImpl(
        "MAF",
        "MAF readings - actual and specified",
        INIT_COMMANDS,
        Arrays.asList(
            DiagUtils.createCommand(
                "21 20",
                "61 20 ",
                1000,
                Arrays.asList(
                    DiagUtils.createField(14, new DecimalFormat("#"), "maf_actual", "MAF actual"),
                    DiagUtils.createField(16, new DecimalFormat("#"), "maf_spec", "MAF specified")
    ))));

    static Package RAIL_PRESSURE_PACKAGE = new PackageImpl(
        "Rail pressure",
        "Rail pressure - actual and specified.",
        INIT_COMMANDS,
        Arrays.asList(
            DiagUtils.createCommand(
                "21 12",
                "61 12 ",
                1000,
                Arrays.asList(
                    DiagUtils.createField(20, 2, new BigDecimal("0"), new BigDecimal("10"), new DecimalFormat("####.# bar"), "rail_actual", "Rail pressure")
            )),
            DiagUtils.createCommand(
                "21 22",
                "61 22 ",
                1000,
                Arrays.asList(
                    DiagUtils.createField( 18, 2, new BigDecimal("0"), new BigDecimal("10"), new DecimalFormat("####.# bar"), "rail_spec", "Rail pressure specified")
    ))));

    static Package MAP_PACKAGE = new PackageImpl(
        "MAP",
        "Manifold air pressure - actual and specified.",
        INIT_COMMANDS,
        Arrays.asList(
            DiagUtils.createCommand(
                "21 12",
                "61 12 ",
                1000,
                Arrays.asList(
                    DiagUtils.createField(18, 2, new BigDecimal("0"), new BigDecimal("1"), new DecimalFormat("#### mbar"), "map_actual", "MAP actual")
            )),
            DiagUtils.createCommand(
                "21 22",
                "61 22 ",
                1000,
                Arrays.asList(
                    DiagUtils.createField(16, 2, new BigDecimal("0"), new BigDecimal("1"), new DecimalFormat("#### mbar"), "map_spec", "MAP specified")
    ))));

    static Package INJECTOR_CORRECTION_PACKAGE = new PackageImpl(
        "Injector corrections",
        "Injector correction coefficients, IQ and RPM.",
        INIT_COMMANDS,
        Arrays.asList(
            DiagUtils.createCommand(
                "21 28",
                "61 28 ",
                1000,
                Arrays.asList(
                    DiagUtils.createField(2, 2, new BigDecimal("0"), new BigDecimal("1"), new DecimalFormat("#### rpm"), "rpm", "RPM"),
                    DiagUtils.createField(4, 2, new BigDecimal("0"), new BigDecimal("100"), new DecimalFormat("##.# mg"), "iq", "IQ"),
                    DiagUtils.createField(18, 2, new BigDecimal("0"), new BigDecimal("100"), new DecimalFormat("+#.##;-#.##"), "inj1_cor", "Injector 1 correction"),
                    DiagUtils.createField(20, 2, new BigDecimal("0"), new BigDecimal("100"), new DecimalFormat("+#.##;-#.##"), "inj2_cor", "Injector 2 correction"),
                    DiagUtils.createField(22, 2, new BigDecimal("0"), new BigDecimal("100"), new DecimalFormat("+#.##;-#.##"), "inj3_cor", "Injector 3 correction"),
                    DiagUtils.createField(24, 2, new BigDecimal("0"), new BigDecimal("100"), new DecimalFormat("+#.##;-#.##"), "inj4_cor", "Injector 4 correction"),
                    DiagUtils.createField(26, 2, new BigDecimal("0"), new BigDecimal("100"), new DecimalFormat("+#.##;-#.##"), "inj5_cor", "Injector 5 correction")
    ))));

    static Package MISC_PACKAGE = new PackageImpl(
        "Misc data",
        "IAT, TPS, coolant temperature, IQ, RPM.",
        INIT_COMMANDS,
        Arrays.asList(DiagUtils.createCommand(
            "21 12",
            "61 12 ",
            1000,
            Arrays.asList(DiagUtils.createField(2, 2, new BigDecimal("-273.1"), new BigDecimal("10"), new DecimalFormat("## C"), "coolant", "Coolant (C)"),
                    DiagUtils.createField(4, 2, new BigDecimal("-273.1"), new BigDecimal("10"), new DecimalFormat("## C"), "iat", "IAT"),
                    DiagUtils.createField(14, 2, new BigDecimal("0"), new BigDecimal("100"), new DecimalFormat("# '%'"), "tps", "TPS"),
                    DiagUtils.createField(30, 2, new BigDecimal("0"), new BigDecimal("1"), new DecimalFormat("# mbar"), "aap???", "AAP???"))),
                DiagUtils.createCommand(
                    "21 28",
                    "61 28 ",
                    1000,
                    Arrays.asList(DiagUtils.createField(2, 2, new BigDecimal("0"), new BigDecimal("1"), new DecimalFormat("#### rpm"), "rpm", "RPM"),
                            DiagUtils.createField(4, 2, new BigDecimal("0"), new BigDecimal("100"), new DecimalFormat("##.# mg"), "iq", "IQ"))
    )));

    private static Package POWER_SYPPLY_PACKAGE = new PackageImpl(
            "Battery voltage",
            "Battery voltage",
            Collections.<Command>emptyList(),
            Arrays.asList(
                    DiagUtils.createCommand("ATRV", null, 1000, Arrays.asList(DiagUtils.createTextField("Voltage", "Battery voltage")))
                    )
    );

    public static List<Package> PACKAGES = Arrays.asList(
            INJECTOR_CORRECTION_PACKAGE,
            RAIL_PRESSURE_PACKAGE,
            MAF_PACKAGE,
            MAP_PACKAGE,
            MISC_PACKAGE,
            POWER_SYPPLY_PACKAGE);
}