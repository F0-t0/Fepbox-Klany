package pl.fepbox.klany.util;

import net.md_5.bungee.api.ChatColor;

/**
 * Utility for translating color placeholders to Bukkit ChatColor codes.
 * Supports both legacy '&' codes and readable names wrapped in <> or {}.
 * Example: "<YELLOW>Tekst" or "{GREEN}Text" or "&aText".
 */
public final class ColorUtil {

    private ColorUtil() {
    }

    public static String colorize(String input) {
        if (input == null) {
            return "";
        }
        String out = input;
        for (ChatColor color : ChatColor.values()) {
            String token = "<" + color.name() + ">";
            String token2 = "{" + color.name() + "}";
            out = out.replace(token, color.toString());
            out = out.replace(token2, color.toString());
        }
        return ChatColor.translateAlternateColorCodes('&', out);
    }
}
