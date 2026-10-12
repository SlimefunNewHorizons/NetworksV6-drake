package io.github.sefiraat.networks.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

/**
 * Regresion del ticket 268: los nodos no entraban al grid hasta colocarlos tres veces.
 *
 * El BlockListener de Slimefun escucha BlockPlaceEvent en HIGHEST y se registra antes que
 * SyncListener, porque Networks se habilita despues de su dependencia. Slimefun persiste el id
 * y llama a NetworkObject.onPlace --que registra el nodo-- y solo entonces corre SyncListener,
 * que borraba ese registro sin comprobar nada.
 */
class SyncListenerPlacementTest {

    private static final Path SYNC_LISTENER_SOURCE = Path.of(
        "src/main/java/io/github/sefiraat/networks/listeners/SyncListener.java"
    );
    private static final Path NETWORK_OBJECT_SOURCE = Path.of(
        "src/main/java/io/github/sefiraat/networks/slimefun/network/NetworkObject.java"
    );
    private static final Path NETWORK_INTEGRITY_SOURCE = Path.of(
        "src/main/java/io/github/sefiraat/networks/utils/NetworkIntegrity.java"
    );

    @Test
    void placingARealMachineDoesNotClearTheNodeSlimefunJustRegistered() throws IOException {
        String source = Files.readString(SYNC_LISTENER_SOURCE, StandardCharsets.UTF_8);
        int guard = source.indexOf("if (NetworkIntegrity.isNetworksMachine(location)) {");
        int clear = source.indexOf("NetworkUtils.clearNetwork(location);", guard);

        assertTrue(guard > 0, "onBlockPlace debe salir cuando la coordenada es una maquina NTW real");
        assertTrue(clear > guard, "el clearNetwork de onBlockPlace debe quedar detras de la guarda");
    }

    @Test
    void placementOverwritesAStaleDefinitionOfADifferentType() throws IOException {
        String source = Files.readString(NETWORK_OBJECT_SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("previous.getType() != this.nodeType"),
            "addToRegistry usa putIfAbsent: onPlace debe purgar la definicion del tipo anterior");
    }

    @Test
    void physicalMaterialCheckMirrorsSlimefunWithoutLinkingToAnUnpublishedApi() throws IOException {
        String source = Files.readString(NETWORK_INTEGRITY_SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("matchesPhysicalMaterial(block.getType(), item.getItem().getType())"));
        assertFalse(source.contains("import com.github.drakescraft_labs.slimefun4.utils.BlockStorageIntegrity"));
        assertFalse(source.contains("BlockStorageIntegrity.matches("));
        assertFalse(source.contains("actual == Material.PLAYER_WALL_HEAD"),
            "la copia local solo cubria cabezas y borraba el resto de variantes de pared");
    }

    @Test
    void wallVariantsAreNotTreatedAsGhosts() {
        assertTrue(NetworkIntegrity.matchesPhysicalMaterial(Material.PLAYER_WALL_HEAD, Material.PLAYER_HEAD));
        assertTrue(NetworkIntegrity.matchesPhysicalMaterial(Material.WALL_TORCH, Material.TORCH));
        assertTrue(NetworkIntegrity.matchesPhysicalMaterial(Material.OAK_WALL_SIGN, Material.OAK_SIGN));
        assertFalse(NetworkIntegrity.matchesPhysicalMaterial(Material.DIRT, Material.PLAYER_HEAD));
    }
}
