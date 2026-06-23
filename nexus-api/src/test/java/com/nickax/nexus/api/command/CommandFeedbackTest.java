package com.nickax.nexus.api.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommandFeedbackTest {

    private final CommandFeedback defaults = CommandFeedback.defaults();

    @Test
    void defaults_returnAMessageForEachFailure() {
        assertEquals("You don't have permission.", defaults.message(new CommandTest.FakeSender(), CommandResult.NO_PERMISSION));
        assertEquals("Only players can use this.", defaults.message(new CommandTest.FakeSender(), CommandResult.PLAYER_ONLY));
        assertEquals("Invalid usage.", defaults.message(new CommandTest.FakeSender(), CommandResult.INVALID_USAGE));
        assertEquals("Unknown subcommand.", defaults.message(new CommandTest.FakeSender(), CommandResult.NO_EXECUTOR));
    }

    @Test
    void defaults_returnNullForSuccess() {
        assertNull(defaults.message(new CommandTest.FakeSender(), CommandResult.SUCCESS));
    }

    @Test
    void customFeedback_canLocalisePerResult() {
        CommandFeedback spanish = (sender, result) -> result == CommandResult.NO_PERMISSION
                ? "<red>Sin permiso."
                : null;
        assertEquals("<red>Sin permiso.", spanish.message(new CommandTest.FakeSender(), CommandResult.NO_PERMISSION));
        assertNull(spanish.message(new CommandTest.FakeSender(), CommandResult.INVALID_USAGE));
    }
}
