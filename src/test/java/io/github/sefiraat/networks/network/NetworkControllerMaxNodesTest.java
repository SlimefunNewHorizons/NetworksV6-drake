package io.github.sefiraat.networks.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkControllerMaxNodesTest {

    private static final Path CONTROLLER_SOURCE = Path.of(
        "src/main/java/io/github/sefiraat/networks/slimefun/network/NetworkController.java"
    );

    @Test
    void controllerUsesSafeMaxNodesGetter() throws IOException {
        String source = Files.readString(CONTROLLER_SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("getMaxNodes()"), "NetworkController must declare and use getMaxNodes()");
        assertTrue(source.contains("final NetworkRoot networkRoot = new NetworkRoot(location, NodeType.CONTROLLER, getMaxNodes());"),
            "rebuildNetwork must pass getMaxNodes() to NetworkRoot");
        assertFalse(source.contains("NodeType.CONTROLLER, maxNodes.getValue()"),
            "rebuildNetwork must not call maxNodes.getValue() directly without caching/guards");
    }

    @Test
    void controllerGuardsUnregisteredStateAndCachesPostRegister() throws IOException {
        String source = Files.readString(CONTROLLER_SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("if (getState() == ItemState.UNREGISTERED)"),
            "getMaxNodes must check for UNREGISTERED state to avoid premature ItemSetting warnings");
        assertTrue(source.contains("void postRegister()"),
            "NetworkController must override postRegister to cache maxNodes once settings are loaded");
        assertTrue(source.contains("cachedMaxNodes"),
            "NetworkController must maintain cachedMaxNodes");
    }
}
