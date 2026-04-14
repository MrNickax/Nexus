package com.nickax.nexus.common.message;

import java.util.logging.Logger;

/**
 * Factory class for creating MessageFormatter instances based on the specified mode.
 * This factory supports three formatter modes: MINI_MESSAGE, LEGACY, and MIXED.
 */
public class MessageFormatterFactory {

    /**
     * Creates a MessageFormatter instance based on the specified mode.
     * The mode is case-insensitive. If an invalid mode is provided, defaults to MINI_MESSAGE.
     *
     * @param mode   the formatter mode to use. Valid values are:
     *               <ul>
     *               <li>MINI_MESSAGE - Uses MiniMessage tag-based formatting</li>
     *               <li>LEGACY - Uses legacy ampersand (&) color codes</li>
     *               <li>MIXED - Supports both legacy color codes and MiniMessage tags</li>
     *               </ul>
     * @param logger the logger instance used to log mode selection and warnings
     * @return a MessageFormatter instance corresponding to the specified mode,
     * or MiniMessageMessageFormatter if the mode is invalid
     */
    public static MessageFormatter create(String mode, Logger logger) {
        return switch (mode.toUpperCase()) {
            case "MINI_MESSAGE" -> {
                logger.info("Message formatter mode set to MINI_MESSAGE.");
                yield new MiniMessageMessageFormatter();
            }
            case "LEGACY" -> {
                logger.info("Message formatter mode set to LEGACY.");
                yield new LegacyMessageFormatter();
            }
            case "MIXED" -> {
                logger.info("Message formatter mode set to MIXED.");
                yield new MixedMessageFormatter();
            }
            default -> {
                logger.warning("Invalid message formatter mode '" + mode + "'. Defaulting to MINI_MESSAGE. Valid modes are: MINI_MESSAGE, LEGACY, MIXED.");
                yield new MiniMessageMessageFormatter();
            }
        };
    }
}