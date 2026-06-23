package com.nickax.nexus.bukkit.command;

import com.nickax.nexus.api.command.Arguments;
import com.nickax.nexus.api.command.Command;
import com.nickax.nexus.api.command.CommandFeedback;
import com.nickax.nexus.api.command.CommandResult;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BukkitCommandsTest {

    private ServerMock server;
    private Plugin plugin;
    private BukkitAudiences audiences;

    @BeforeEach
    void start() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("Test");
        audiences = BukkitAudiences.create(plugin);
    }

    @AfterEach
    void stop() {
        audiences.close();
        MockBukkit.unmock();
    }

    @Test
    void register_thenDispatchThroughServer_runsExecutor() {
        AtomicInteger given = new AtomicInteger();
        Command command = Command.named("give")
                .argument("amount", Arguments.integer(1, 64))
                .executes(ctx -> given.set(ctx.getInt("amount")))
                .build();
        new BukkitCommands(plugin, audiences).register(plugin, command);

        assertNotNull(server.getCommandMap().getCommand("give"));
        server.dispatchCommand(server.getConsoleSender(), "give 10");
        assertEquals(10, given.get());
    }

    @Test
    void register_withCustomFeedback_invokesItOnFailure() {
        Player player = server.addPlayer("Bob"); // not op, so the permission check fails
        AtomicReference<CommandResult> seen = new AtomicReference<>();
        Command command = Command.named("secret")
                .permission("nexus.secret")
                .executes(ctx -> {})
                .build();
        CommandFeedback feedback = (sender, result) -> {
            seen.set(result);
            return null; // returning null sends nothing
        };
        new BukkitCommands(plugin, audiences).register(plugin, command, feedback);

        server.dispatchCommand(player, "secret");
        assertEquals(CommandResult.NO_PERMISSION, seen.get());
    }

    @Test
    void unregisterAll_stopsDispatch() {
        java.util.concurrent.atomic.AtomicInteger runs = new java.util.concurrent.atomic.AtomicInteger();
        com.nickax.nexus.api.command.Command command = com.nickax.nexus.api.command.Command.named("ping")
                .executes(ctx -> runs.incrementAndGet())
                .build();
        BukkitCommands commands = new BukkitCommands(plugin, audiences);
        commands.register(plugin, command);
        server.dispatchCommand(server.getConsoleSender(), "ping");
        assertEquals(1, runs.get());

        commands.unregisterAll();
        try {
            server.dispatchCommand(server.getConsoleSender(), "ping");
        } catch (Throwable ignored) {
            // an unknown command may throw in MockBukkit — that's an acceptable "not dispatched" signal
        }
        assertEquals(1, runs.get(), "executor must not run after unregisterAll");
    }

    @Test
    void unregister_byOwner_stopsDispatch() {
        AtomicInteger runs = new AtomicInteger();
        Command command = Command.named("pong")
                .executes(ctx -> runs.incrementAndGet())
                .build();
        BukkitCommands commands = new BukkitCommands(plugin, audiences);
        commands.register(plugin, command);
        server.dispatchCommand(server.getConsoleSender(), "pong");
        assertEquals(1, runs.get());

        commands.unregister(plugin);
        try {
            server.dispatchCommand(server.getConsoleSender(), "pong");
        } catch (Throwable ignored) {
            // an unknown command may throw in MockBukkit — that's an acceptable "not dispatched" signal
        }
        assertEquals(1, runs.get(), "executor must not run after unregister(owner)");
    }
}
