package com.balugaq.netex.api.enums;

import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;

/**
 * Minecraft versions for version judgement
 *
 * @author lijinhong11
 * @author balugaq
 */
@NullMarked
public record MinecraftVersion(int major, int minor, int patch) implements Comparable<MinecraftVersion> {
    public static final MinecraftVersion UNKNOWN = new MinecraftVersion();
    public static final MinecraftVersion V1_20 = MinecraftVersion.of(1, 20);
    public static final MinecraftVersion V1_20_5 = MinecraftVersion.of(1, 20, 5);
    public static final MinecraftVersion V1_21 = MinecraftVersion.of(1, 21);

    public MinecraftVersion() {
        this(999, 999, 999);
    }

    public static MinecraftVersion current() {
        return MinecraftVersion.of(Bukkit.getMinecraftVersion());
    }

    public static MinecraftVersion of(String version) {
        return deserialize(version);
    }

    public boolean isAtLeast(String version) {
        return isAtLeast(of(version));
    }

    public boolean isAtLeast(MinecraftVersion version) {
        return this.major > version.major ||
            this.major == version.major && this.minor > version.minor ||
            this.major == version.major && this.minor == version.minor && this.patch >= version.patch;
    }

    public boolean isBefore(String version) {
        return isBefore(of(version));
    }

    public boolean isBefore(MinecraftVersion version) {
        return !isAtLeast(version) && this != version;
    }

    /*
     * Acepta "1.21.11", "26.1", "26.1.2" y tambien versiones con sufijo como "1.21.11-pre5" o
     * "26.1-rc-1": de cada parte se toman solo los digitos del principio. Antes el sufijo hacia que
     * parseUnsignedInt lanzara NumberFormatException, y Networks.onEnable solo atrapa
     * NoClassDefFoundError: el plugin no arrancaba en una pre-release o release candidate.
     */
    public static MinecraftVersion deserialize(String s) {
        String[] split = s.split("\\.");
        if (split.length < 2) throw new IllegalArgumentException("Invalid version string: " + s);
        int major = leadingNumber(split[0], s);
        int minor = leadingNumber(split[1], s);
        int patch = split.length == 2 ? 0 : leadingNumber(split[2], s);
        return MinecraftVersion.of(major, minor, patch);
    }

    private static int leadingNumber(String part, String full) {
        int end = 0;
        while (end < part.length() && Character.isDigit(part.charAt(end))) {
            end++;
        }
        if (end == 0) throw new IllegalArgumentException("Invalid version string: " + full);
        return Integer.parseUnsignedInt(part.substring(0, end));
    }

    public static MinecraftVersion of(int major, int minor, int patch) {
        return new MinecraftVersion(major, minor, patch);
    }

    public static MinecraftVersion of(int major, int minor) {
        return of(major, minor, 0);
    }

    @Override
    public int compareTo(MinecraftVersion o) {
        return Integer.compare(this.major * 1000000 + this.minor * 10000 + this.patch, o.major * 1000000 + o.minor * 10000 + o.patch);
    }

    public String humanize() {
        return major + "." + minor + "." + patch;
    }
}
