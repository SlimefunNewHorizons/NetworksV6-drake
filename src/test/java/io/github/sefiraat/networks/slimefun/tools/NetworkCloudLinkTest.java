package io.github.sefiraat.networks.slimefun.tools;

import io.github.sefiraat.networks.BukkitTestSupport;
import io.github.sefiraat.networks.listeners.CloudLinkListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NetworkCloudLinkTest extends BukkitTestSupport {

    @Test
    void protectsArmorToolsWeaponsAndUtilityItems() {
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.DIAMOND_HELMET));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.NETHERITE_CHESTPLATE));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.IRON_LEGGINGS));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.GOLDEN_BOOTS));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.NETHERITE_PICKAXE));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.DIAMOND_SWORD));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.IRON_AXE));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.STONE_SHOVEL));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.WOODEN_HOE));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.BOW));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.CROSSBOW));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.SHIELD));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.TRIDENT));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.MACE));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.ELYTRA));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.TOTEM_OF_UNDYING));
        assertTrue(NetworkCloudLink.isProtectedMaterial(Material.RECOVERY_COMPASS));

        assertFalse(NetworkCloudLink.isProtectedMaterial(Material.COBBLESTONE));
        assertFalse(NetworkCloudLink.isProtectedMaterial(Material.DIAMOND));
        assertFalse(NetworkCloudLink.isProtectedMaterial(Material.IRON_INGOT));
        assertFalse(NetworkCloudLink.isProtectedMaterial(Material.OAK_LOG));
        assertFalse(NetworkCloudLink.isProtectedMaterial(Material.COAL));
    }

    @Test
    void calculateAcceptedAmountClampsAnomalies() {
        assertEquals(64, NetworkCloudLink.calculateAcceptedAmount(64, 0));
        assertEquals(20, NetworkCloudLink.calculateAcceptedAmount(64, 44));
        assertEquals(0, NetworkCloudLink.calculateAcceptedAmount(64, 64));
        // Valores anomalos: remaining negativo o mayor al inicial
        assertEquals(64, NetworkCloudLink.calculateAcceptedAmount(64, -10));
        assertEquals(0, NetworkCloudLink.calculateAcceptedAmount(64, 99));
        assertEquals(0, NetworkCloudLink.calculateAcceptedAmount(0, 0));
    }

    @Test
    void depositAtomicFullPartialAndFullNetwork() {
        // 1. Deposito completo
        ItemStack fullStack = new ItemStack(Material.COBBLESTONE, 64);
        NetworkCloudLink.DepositResult fullRes = NetworkCloudLink.depositAtomic(fullStack, incoming -> incoming.setAmount(0));
        assertEquals(64, fullRes.accepted());
        assertEquals(0, fullRes.remaining());
        assertEquals(0, fullStack.getAmount());

        // 2. Deposito parcial (solo caben 20 de 64)
        ItemStack partialStack = new ItemStack(Material.IRON_INGOT, 64);
        NetworkCloudLink.DepositResult partialRes = NetworkCloudLink.depositAtomic(partialStack, incoming -> incoming.setAmount(44));
        assertEquals(20, partialRes.accepted());
        assertEquals(44, partialRes.remaining());
        assertEquals(44, partialStack.getAmount());

        // 3. Red llena (no acepta nada) -> stack intacto
        ItemStack blockedStack = new ItemStack(Material.DIAMOND, 32);
        NetworkCloudLink.DepositResult blockedRes = NetworkCloudLink.depositAtomic(blockedStack, incoming -> {
            // No acepta nada
        });
        assertEquals(0, blockedRes.accepted());
        assertEquals(32, blockedRes.remaining());
        assertEquals(32, blockedStack.getAmount());
    }

    @Test
    void dumpInventoryProtectsEquipmentAndOnlyTouchesSlots0To35() {
        // Simula array completo de 41 slots (0..35 storage, 36..39 armor, 40 offhand)
        ItemStack[] contents = new ItemStack[41];
        contents[0] = new ItemStack(Material.NETHERITE_PICKAXE, 1);
        contents[1] = new ItemStack(Material.RECOVERY_COMPASS, 1); // Cloud-Link
        contents[2] = new ItemStack(Material.COBBLESTONE, 64);
        contents[3] = new ItemStack(Material.DIAMOND, 10);
        contents[4] = new ItemStack(Material.SHIELD, 1);
        contents[5] = new ItemStack(Material.IRON_INGOT, 40);
        // Armor & Offhand con items normales para probar limite estricto 0..35
        contents[36] = new ItemStack(Material.COBBLESTONE, 16);
        contents[39] = new ItemStack(Material.DIAMOND_HELMET, 1);
        contents[40] = new ItemStack(Material.COBBLESTONE, 32);

        // Red con capacidad limitada de 80 items en total
        AtomicInteger remainingCapacity = new AtomicInteger(80);

        NetworkCloudLink.DumpSummary summary = NetworkCloudLink.dumpInventory(
                contents,
                NetworkCloudLink::isProtectedFromDump,
                incoming -> {
                    int cap = remainingCapacity.get();
                    int take = Math.min(cap, incoming.getAmount());
                    remainingCapacity.addAndGet(-take);
                    incoming.setAmount(incoming.getAmount() - take);
                }
        );

        // 64 cobble + 10 diamond + 6 iron = 80 depositados; quedan 34 iron en slot 5
        assertEquals(80, summary.itemsDeposited());
        assertEquals(1, summary.stacksRemaining());

        // Herramientas y Cloud-Link intactos
        assertNotNull(contents[0]);
        assertEquals(Material.NETHERITE_PICKAXE, contents[0].getType());
        assertNotNull(contents[1]);
        assertEquals(Material.RECOVERY_COMPASS, contents[1].getType());
        assertNotNull(contents[4]);
        assertEquals(Material.SHIELD, contents[4].getType());

        // Depositados completamente -> null
        assertNull(contents[2]);
        assertNull(contents[3]);

        // Parcialmente depositado -> sobrante exacto de 34
        assertNotNull(contents[5]);
        assertEquals(34, contents[5].getAmount());

        // Slots 36..40 JAMAS tocados
        assertNotNull(contents[36]);
        assertEquals(16, contents[36].getAmount());
        assertNotNull(contents[39]);
        assertEquals(Material.DIAMOND_HELMET, contents[39].getType());
        assertNotNull(contents[40]);
        assertEquals(32, contents[40].getAmount());
    }

    @Test
    void fullNotificationRateLimitRespectsCooldownPerPlayer() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();
        try {
            assertTrue(NetworkCloudLink.shouldNotifyFull(playerA, 10_000L, 5_000L));
            assertFalse(NetworkCloudLink.shouldNotifyFull(playerA, 12_000L, 5_000L));
            assertFalse(NetworkCloudLink.shouldNotifyFull(playerA, 14_999L, 5_000L));
            assertTrue(NetworkCloudLink.shouldNotifyFull(playerB, 11_000L, 5_000L));
            assertTrue(NetworkCloudLink.shouldNotifyFull(playerA, 15_000L, 5_000L));

            NetworkCloudLink.clearNotifyCooldown(playerA);
            assertTrue(NetworkCloudLink.shouldNotifyFull(playerA, 15_001L, 5_000L));
        } finally {
            NetworkCloudLink.clearNotifyCooldown(playerA);
            NetworkCloudLink.clearNotifyCooldown(playerB);
        }
    }

    @Test
    void cloudGridSessionTrackingWorksPerPlayer() {
        UUID playerId = UUID.randomUUID();
        Location dummyLoc = new Location(null, 10, 64, -20);
        assertFalse(CloudLinkListener.isCloudGridOpen(playerId));
        CloudLinkListener.markCloudGridOpen(playerId, dummyLoc);
        assertTrue(CloudLinkListener.isCloudGridOpen(playerId));
        CloudLinkListener.clearCloudGridOpen(playerId);
        assertFalse(CloudLinkListener.isCloudGridOpen(playerId));
    }
}
