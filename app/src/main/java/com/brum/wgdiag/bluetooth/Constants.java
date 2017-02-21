package com.brum.wgdiag.bluetooth;

import com.brum.wgdiag.command.Command;
import com.brum.wgdiag.command.impl.Utils;

import java.util.Arrays;
import java.util.List;


/**
 * Common bluetooth constants.
 */
public class Constants {
    public static final List<Command> VERIFY_DEVICE_COMMANDS =
            Arrays.asList(Utils.createCommand("ATZ", "ELM327", 7500));
}
