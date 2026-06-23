package com.nickax.nexus.api.command;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTest {

    /** Configurable fake sender. */
    static final class FakeSender implements Sender {
        boolean player = true;
        boolean permitted = true;
        Component lastMessage;
        @Override public String name() { return "tester"; }
        @Override public Optional<UUID> uuid() { return Optional.of(new UUID(0, 1)); }
        @Override public boolean hasPermission(String permission) { return permitted; }
        @Override public boolean isPlayer() { return player; }
        @Override public Audience audience() {
            return new Audience() {
                @Override public void sendMessage(net.kyori.adventure.text.Component message) { lastMessage = message; }
            };
        }
    }

    @Test
    void dispatch_runsExecutorWithParsedArgs() {
        AtomicReference<CommandContext> seen = new AtomicReference<>();
        Command command = Command.named("give")
                .argument("amount", Arguments.integer(1, 64))
                .executes(seen::set)
                .build();

        CommandResult result = command.dispatch(new FakeSender(), new String[]{"10"});

        assertEquals(CommandResult.SUCCESS, result);
        assertEquals(10, seen.get().getInt("amount"));
    }

    @Test
    void dispatch_missingRequiredArg_isInvalidUsage() {
        Command command = Command.named("give")
                .argument("amount", Arguments.integer(1, 64))
                .executes(ctx -> { })
                .build();
        assertEquals(CommandResult.INVALID_USAGE, command.dispatch(new FakeSender(), new String[]{}));
    }

    @Test
    void dispatch_invalidArg_isInvalidUsage() {
        Command command = Command.named("give")
                .argument("amount", Arguments.integer(1, 64))
                .executes(ctx -> { })
                .build();
        assertEquals(CommandResult.INVALID_USAGE, command.dispatch(new FakeSender(), new String[]{"999"}));
    }

    @Test
    void dispatch_noPermission_isBlocked() {
        Command command = Command.named("op").permission("nexus.op").executes(ctx -> { }).build();
        FakeSender sender = new FakeSender();
        sender.permitted = false;
        assertEquals(CommandResult.NO_PERMISSION, command.dispatch(sender, new String[]{}));
    }

    @Test
    void dispatch_playerOnly_blocksConsole() {
        Command command = Command.named("home").requiresPlayer().executes(ctx -> { }).build();
        FakeSender console = new FakeSender();
        console.player = false;
        assertEquals(CommandResult.PLAYER_ONLY, command.dispatch(console, new String[]{}));
    }

    @Test
    void dispatch_routesToSubcommand() {
        AtomicReference<String> ran = new AtomicReference<>();
        Command command = Command.named("economy")
                .subcommand(Command.named("balance").executes(ctx -> ran.set("balance")).build())
                .subcommand(Command.named("pay")
                        .argument("amount", Arguments.integer(1, 1000))
                        .executes(ctx -> ran.set("pay:" + ctx.getInt("amount"))).build())
                .build();

        assertEquals(CommandResult.SUCCESS, command.dispatch(new FakeSender(), new String[]{"pay", "50"}));
        assertEquals("pay:50", ran.get());
    }

    @Test
    void dispatch_group_withoutExecutor_isNoExecutor() {
        Command command = Command.named("economy")
                .subcommand(Command.named("balance").executes(ctx -> { }).build())
                .build();
        assertEquals(CommandResult.NO_EXECUTOR, command.dispatch(new FakeSender(), new String[]{}));
    }

    @Test
    void complete_suggestsSubcommandNames() {
        Command command = Command.named("economy")
                .subcommand(Command.named("balance").executes(ctx -> { }).build())
                .subcommand(Command.named("pay").executes(ctx -> { }).build())
                .build();
        List<String> suggestions = command.complete(new FakeSender(), new String[]{"ba"});
        assertTrue(suggestions.contains("balance"));
    }

    @Test
    void complete_suggestsArgumentValues() {
        Command command = Command.named("set")
                .argument("flag", Arguments.bool())
                .executes(ctx -> { })
                .build();
        assertEquals(List.of("true", "false"), command.complete(new FakeSender(), new String[]{""}));
    }

    @Test
    void reply_sendsToSenderAudience() {
        FakeSender sender = new FakeSender();
        Command command = Command.named("ping")
                .executes(ctx -> ctx.reply(Component.text("pong"))).build();
        command.dispatch(sender, new String[]{});
        assertEquals("pong", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText().serialize(sender.lastMessage));
    }

    @Test
    void build_optionalBeforeRequired_throws() {
        assertThrows(IllegalStateException.class, () -> Command.named("x")
                .optionalArgument("a", Arguments.word())
                .argument("b", Arguments.word())
                .executes(ctx -> { })
                .build());
    }

    @Test
    void build_noExecutorNoSubcommands_throws() {
        assertThrows(IllegalStateException.class, () -> Command.named("x").build());
    }

    @Test
    void subcommand_duplicate_throws() {
        assertThrows(IllegalArgumentException.class, () -> Command.named("x")
                .subcommand(Command.named("a").executes(ctx -> { }).build())
                .subcommand(Command.named("a").executes(ctx -> { }).build()));
    }

    @Test
    void dispatch_optionalArgument_absentThenPresent() {
        java.util.concurrent.atomic.AtomicReference<Boolean> present = new java.util.concurrent.atomic.AtomicReference<>();
        Command command = Command.named("x")
                .optionalArgument("a", Arguments.word())
                .executes(ctx -> present.set(ctx.has("a")))
                .build();
        command.dispatch(new FakeSender(), new String[]{});
        assertFalse(present.get());
        command.dispatch(new FakeSender(), new String[]{"hi"});
        assertTrue(present.get());
    }
}
