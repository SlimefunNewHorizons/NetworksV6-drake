package io.github.sefiraat.networks.utils;

import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import com.github.drakescraft_labs.slimefun4.libraries.dough.protection.Interaction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recuerda durante unos segundos si el dueno de un nodo puede tocar el inventario de al lado.
 *
 * El Vanilla Grabber y el Vanilla Pusher comprueban ese permiso en cada tick: construyen un
 * OfflinePlayer y preguntan al gestor de protecciones, que a su vez consulta a WorldGuard. Con
 * varias decenas de estos nodos en el servidor esa comprobacion se repite constantemente para
 * responder casi siempre lo mismo, porque las protecciones cambian de tarde en tarde y los nodos
 * no se mueven.
 *
 * Cinco segundos es un compromiso deliberado: si alguien pierde el acceso a una zona, sus nodos
 * dejan de robar dentro de ese margen, que es mucho menos de lo que tarda nadie en notarlo, y a
 * cambio se evita repetir la consulta sesenta veces por minuto y por nodo.
 */
public final class OwnerAccessCache {

    private static final long TTL_MS = 5000L;

    private record Entrada(boolean permitido, long momento) {
    }

    private static final Map<String, Entrada> CACHE = new ConcurrentHashMap<>();

    private OwnerAccessCache() {
    }

    /** Si el dueno indicado puede interactuar con el bloque, reutilizando la ultima respuesta. */
    public static boolean canInteract(@Nonnull UUID owner, @Nonnull Block block) {
        final Location location = block.getLocation();
        final String clave = owner + "@" + location.getWorld().getName()
                + ':' + location.getBlockX() + ':' + location.getBlockY() + ':' + location.getBlockZ();

        final long ahora = System.currentTimeMillis();
        final Entrada previa = CACHE.get(clave);
        if (previa != null && ahora - previa.momento() < TTL_MS) {
            return previa.permitido();
        }

        final OfflinePlayer jugador = Bukkit.getOfflinePlayer(owner);
        final boolean permitido =
                Slimefun.getProtectionManager().hasPermission(jugador, block, Interaction.INTERACT_BLOCK);
        CACHE.put(clave, new Entrada(permitido, ahora));
        return permitido;
    }

    /**
     * Descarta lo cacheado.
     *
     * Sin esto el mapa crece con cada par de dueno y ubicacion que haya existido, incluidos nodos
     * ya retirados. Se llama al recargar el plugin y al apagar el servidor.
     */
    public static void clear() {
        CACHE.clear();
    }
}
