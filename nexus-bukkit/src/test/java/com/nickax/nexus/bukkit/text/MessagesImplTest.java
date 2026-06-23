package com.nickax.nexus.bukkit.text;

import com.nickax.nexus.api.text.TextFormat;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessagesImplTest {

    private ServerMock server;
    private Plugin plugin;
    private BukkitAudiences audiences;
    private MessagesImpl messages;

    @BeforeEach
    void start() {
        server = MockBukkit.mock();
        plugin = MockBukkit.createMockPlugin("Test");
        audiences = BukkitAudiences.create(plugin);
        messages = new MessagesImpl(audiences);
    }

    @AfterEach
    void stop() {
        audiences.close();
        MockBukkit.unmock();
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    void send_legacy_formatsAndDelivers() {
        PlayerMock player = server.addPlayer("Steve");
        messages.send(player, TextFormat.LEGACY, "&aHello");
        Component received = player.nextComponentMessage();
        assertNotNull(received);
        assertEquals("Hello", plain(received));
    }

    @Test
    void send_lines_joinsWithNewline() {
        PlayerMock player = server.addPlayer("Steve");
        messages.send(player, TextFormat.MINI_MESSAGE, List.of("<red>a", "<blue>b"));
        Component received = player.nextComponentMessage();
        assertNotNull(received);
        assertEquals("a\nb", plain(received));
    }

    @Test
    void broadcast_deliversToEveryOnlinePlayer() {
        PlayerMock one = server.addPlayer("One");
        PlayerMock two = server.addPlayer("Two");
        messages.broadcast(TextFormat.MINI_MESSAGE, "<green>hi");
        assertEquals("hi", plain(one.nextComponentMessage()));
        assertEquals("hi", plain(two.nextComponentMessage()));
    }

    @Test
    void sendActionBar_legacy_sendsViaSpigotActionBar() {
        // Players receive action bars through the Spigot chat API, not BukkitAudiences.
        Player player = mock(Player.class);
        Player.Spigot spigot = mock(Player.Spigot.class);
        when(player.spigot()).thenReturn(spigot);
        MessagesImpl sut = new MessagesImpl(mock(BukkitAudiences.class));

        sut.sendActionBar(player, TextFormat.LEGACY, "&aReward");

        ArgumentCaptor<BaseComponent[]> captor = ArgumentCaptor.forClass(BaseComponent[].class);
        verify(spigot).sendMessage(eq(ChatMessageType.ACTION_BAR), captor.capture());
        assertEquals("Reward", TextComponent.toPlainText(captor.getValue()));
    }

    @Test
    void sendActionBar_miniMessageConvenience_sendsViaSpigotActionBar() {
        Player player = mock(Player.class);
        Player.Spigot spigot = mock(Player.Spigot.class);
        when(player.spigot()).thenReturn(spigot);
        MessagesImpl sut = new MessagesImpl(mock(BukkitAudiences.class));

        sut.sendActionBar(player, "<green>Hi");

        ArgumentCaptor<BaseComponent[]> captor = ArgumentCaptor.forClass(BaseComponent[].class);
        verify(spigot).sendMessage(eq(ChatMessageType.ACTION_BAR), captor.capture());
        assertEquals("Hi", TextComponent.toPlainText(captor.getValue()));
    }

    @Test
    void send_lang_resolvesLocaleAndFormatsWithDialect() {
        // The lang template carries MiniMessage; LEGACY here only proves the dialect is applied.
        PlayerMock player = server.addPlayer("Steve");
        Audience audience = mock(Audience.class);
        BukkitAudiences mockAudiences = mock(BukkitAudiences.class);
        when(mockAudiences.sender(player)).thenReturn(audience);
        MessagesImpl sut = new MessagesImpl(mockAudiences);
        com.nickax.nexus.api.lang.Lang lang = new com.nickax.nexus.core.lang.LangBuilderImpl()
                .defaultLocale("en")
                .bundle("en", java.util.Map.of("greet", "<green>Hi {name}"))
                .build();

        sut.send(player, lang, TextFormat.MINI_MESSAGE, "greet",
                com.nickax.nexus.api.lang.Placeholder.of("name", "Bob"));

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);
        verify(audience).sendMessage(captor.capture());
        assertEquals("Hi Bob", plain(captor.getValue()));
    }
}
