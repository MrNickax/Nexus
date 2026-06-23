package com.nickax.nexus.api.command;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A command node: a name with aliases, an optional permission, optional
 * player-only gating, positional typed arguments, subcommands, and an optional
 * executor. Build with {@link #named(String)}.
 */
public final class Command {

    private final String name;
    private final List<String> aliases;
    private final String permission;
    private final String description;
    private final boolean requiresPlayer;
    private final List<Argument> arguments;
    private final Map<String, Command> subcommands;
    private final CommandExecutor executor;

    /**
     * Constructs a Command from a builder, copying all configured settings.
     *
     * @param builder the builder containing the command configuration
     */
    private Command(Builder builder) {
        this.name = builder.name;
        this.aliases = List.copyOf(builder.aliases);
        this.permission = builder.permission;
        this.description = builder.description;
        this.requiresPlayer = builder.requiresPlayer;
        this.arguments = List.copyOf(builder.arguments);
        this.subcommands = new LinkedHashMap<>(builder.subcommands);
        this.executor = builder.executor;
    }

    /**
     * Returns a new builder for a command with the given name.
     *
     * @param name the command name
     * @return a new builder
     */
    public static @NotNull Builder named(@NotNull String name) {
        return new Builder(name);
    }

    /**
     * Returns the primary name of this command.
     *
     * @return the command name
     */
    public @NotNull String name() { return name; }

    /**
     * Returns the alias list for this command.
     *
     * @return the aliases (may be empty)
     */
    public @NotNull List<String> aliases() { return aliases; }

    /**
     * Returns the permission required to execute this command.
     *
     * @return the permission node, or {@code null} if no permission is required
     */
    public @Nullable String permission() { return permission; }

    /**
     * Returns the human-readable description of this command.
     *
     * @return the description, or {@code null} if none was set
     */
    public @Nullable String description() { return description; }

    /**
     * Looks up a subcommand by name or alias (case-insensitive).
     *
     * @param token the token to match against subcommand names and aliases
     * @return the matching subcommand, or {@code null} if none
     */
    private @Nullable Command findSub(String token) {
        Command direct = subcommands.get(token.toLowerCase());
        if (direct != null) {
            return direct;
        }
        for (Command sub : subcommands.values()) {
            if (sub.aliases.stream().anyMatch(a -> a.equalsIgnoreCase(token))) {
                return sub;
            }
        }
        return null;
    }

    /**
     * Dispatches this command for a sender and argument tokens.
     *
     * @param sender the sender
     * @param args   the tokens (for this node)
     * @return the result
     */
    public @NotNull CommandResult dispatch(@NotNull Sender sender, @NotNull String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            return CommandResult.NO_PERMISSION;
        }
        if (args.length > 0) {
            Command sub = findSub(args[0]);
            if (sub != null) {
                return sub.dispatch(sender, Arrays.copyOfRange(args, 1, args.length));
            }
        }
        if (requiresPlayer && !sender.isPlayer()) {
            return CommandResult.PLAYER_ONLY;
        }
        if (executor == null) {
            return CommandResult.NO_EXECUTOR;
        }
        Map<String, Object> parsed = new HashMap<>();
        int index = 0;
        for (Argument argument : arguments) {
            if (index >= args.length) {
                if (argument.optional()) {
                    continue;
                }
                return CommandResult.INVALID_USAGE;
            }
            String token;
            if (argument.type().greedy()) {
                token = String.join(" ", Arrays.copyOfRange(args, index, args.length));
                index = args.length;
            } else {
                token = args[index];
                index++;
            }
            try {
                parsed.put(argument.name(), argument.type().parse(token));
            } catch (ArgumentParseException e) {
                return CommandResult.INVALID_USAGE;
            }
        }
        executor.execute(new CommandContextImpl(sender, args, parsed));
        return CommandResult.SUCCESS;
    }

    /**
     * Suggests tab-completions for a sender and partial tokens.
     *
     * @param sender the sender
     * @param args   the partial tokens
     * @return suggestions
     */
    public @NotNull List<String> complete(@NotNull Sender sender, @NotNull String[] args) {
        if (permission != null && !sender.hasPermission(permission)) {
            return List.of();
        }
        if (args.length == 0) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (Command sub : subcommands.values()) {
                if (sub.name.toLowerCase().startsWith(prefix)
                        && (sub.permission == null || sender.hasPermission(sub.permission))) {
                    out.add(sub.name);
                }
            }
            if (!arguments.isEmpty()) {
                out.addAll(arguments.get(0).type().suggest(args[0]));
            }
            return out;
        }
        Command sub = findSub(args[0]);
        if (sub != null) {
            return sub.complete(sender, Arrays.copyOfRange(args, 1, args.length));
        }
        if (!arguments.isEmpty()) {
            int index = Math.min(args.length - 1, arguments.size() - 1);
            Argument argument = arguments.get(index);
            if (index == args.length - 1 || argument.type().greedy()) {
                return argument.type().suggest(args[args.length - 1]);
            }
        }
        return List.of();
    }

    /**
     * Fluent {@link Command} builder.
     */
    public static final class Builder {
        private final String name;
        private final List<String> aliases = new ArrayList<>();
        private String permission;
        private String description;
        private boolean requiresPlayer;
        private final List<Argument> arguments = new ArrayList<>();
        private final Map<String, Command> subcommands = new LinkedHashMap<>();
        private CommandExecutor executor;

        /**
         * Constructs a new Builder for a command with the given name.
         *
         * @param name the command name
         */
        private Builder(@NotNull String name) {
            this.name = name;
        }

        /**
         * Adds one or more alternative names for this command.
         *
         * @param aliases alternative names
         * @return this builder
         */
        public @NotNull Builder alias(@NotNull String... aliases) {
            this.aliases.addAll(Arrays.asList(aliases));
            return this;
        }

        /**
         * Sets the permission required to execute or see this command.
         *
         * @param permission the required permission node
         * @return this builder
         */
        public @NotNull Builder permission(@NotNull String permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Sets a human-readable description for help listings.
         *
         * @param description a help description
         * @return this builder
         */
        public @NotNull Builder description(@NotNull String description) {
            this.description = description;
            return this;
        }

        /**
         * Marks this command as player-only; the console will receive
         * {@link CommandResult#PLAYER_ONLY} when it tries to run it.
         *
         * @return this builder
         */
        public @NotNull Builder requiresPlayer() {
            this.requiresPlayer = true;
            return this;
        }

        /**
         * Adds a required positional argument.
         *
         * @param name the argument name (used for context lookup)
         * @param type the argument type
         * @return this builder
         */
        public @NotNull Builder argument(@NotNull String name, @NotNull ArgumentType<?> type) {
            this.arguments.add(new Argument(name, type, false));
            return this;
        }

        /**
         * Adds an optional positional argument. Optional arguments must be declared
         * after all required ones.
         *
         * @param name the argument name (used for context lookup)
         * @param type the argument type
         * @return this builder
         */
        public @NotNull Builder optionalArgument(@NotNull String name, @NotNull ArgumentType<?> type) {
            this.arguments.add(new Argument(name, type, true));
            return this;
        }

        /**
         * Registers a subcommand. A command node should use either subcommands or
         * positional arguments at one level, not both.
         *
         * @param command the subcommand to register
         * @return this builder
         * @throws IllegalArgumentException if a subcommand with the same name is already registered
         */
        public @NotNull Builder subcommand(@NotNull Command command) {
            String key = command.name.toLowerCase();
            if (subcommands.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate subcommand: " + command.name);
            }
            subcommands.put(key, command);
            return this;
        }

        /**
         * Sets the executor that runs when this node is matched.
         *
         * @param executor the executor
         * @return this builder
         */
        public @NotNull Builder executes(@NotNull CommandExecutor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Builds and returns the configured command.
         *
         * @return the built command
         * @throws IllegalStateException if optional arguments precede required ones, or
         *                               if the command has neither an executor nor subcommands
         */
        public @NotNull Command build() {
            boolean seenOptional = false;
            for (Argument argument : arguments) {
                if (seenOptional && !argument.optional()) {
                    throw new IllegalStateException("Required argument '" + argument.name()
                            + "' declared after an optional argument in command '" + name + "'");
                }
                seenOptional |= argument.optional();
            }
            if (executor == null && subcommands.isEmpty()) {
                throw new IllegalStateException("Command '" + name
                        + "' has neither an executor nor subcommands");
            }
            return new Command(this);
        }
    }
}
