package io.github.sefiraat.networks.slimefun.network.grid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresion del IndexOutOfBoundsException "fromIndex = -48" reportado por NetworksV6-Drake el
 * 2026-09-14 en AbstractGrid#updateDisplay: con la rejilla vacia el maximo de paginas valia -1,
 * el boton de pagina siguiente dejaba la cache en la pagina -1 y, al reaparecer items en la red,
 * el subList de la ventana visible arrancaba en un indice negativo en cada tick.
 */
class GridPagingTest {

    /** Mismo tamano que NetworkGrid.DISPLAY_SLOTS, el que produjo el indice -48. */
    private static final int DISPLAY_SLOTS = 48;

    private static int maxPagesFor(int entries) {
        // Replica del calculo de AbstractGrid#updateDisplay, ya saneado.
        return Math.max(0, (int) Math.ceil(entries / (double) DISPLAY_SLOTS) - 1);
    }

    @Test
    @DisplayName("la rejilla vacia no publica un maximo de paginas negativo")
    void emptyGridNeverPublishesNegativeMaxPages() {
        assertEquals(0, maxPagesFor(0));
    }

    @Test
    @DisplayName("pagina siguiente sobre una rejilla vacia se queda en la pagina 0")
    void nextPageOnEmptyGridStaysOnFirstPage() {
        assertEquals(0, GridCache.nextPage(0, maxPagesFor(0)));
    }

    @Test
    @DisplayName("pagina siguiente nunca devuelve una pagina negativa aunque llegue un maximo sucio")
    void nextPageIsNeverNegativeEvenWithStaleNegativeMax() {
        assertEquals(0, GridCache.nextPage(0, -1));
        assertEquals(0, GridCache.nextPage(-1, -1));
    }

    @Test
    @DisplayName("el inicio de la ventana visible nunca es negativo tras varios clics")
    void windowStartNeverGoesNegative() {
        final GridCache cache = new GridCache(0, maxPagesFor(0), GridCache.SortOrder.ALPHABETICAL);
        for (int click = 0; click < 5; click++) {
            cache.setPage(GridCache.nextPage(cache.getPage(), cache.getMaxPages()));
        }
        // La red vuelve a tener items: una sola pagina completa.
        cache.setMaxPages(maxPagesFor(DISPLAY_SLOTS));
        assertTrue(cache.getPage() >= 0, "la pagina en cache no puede ser negativa");
        assertTrue(cache.getPage() * DISPLAY_SLOTS >= 0, "fromIndex de subList no puede ser negativo");
    }

    @Test
    @DisplayName("pagina anterior se detiene en la pagina 0")
    void previousPageStopsAtZero() {
        assertEquals(0, GridCache.previousPage(0));
        assertEquals(0, GridCache.previousPage(-3));
        assertEquals(1, GridCache.previousPage(2));
    }

    @Test
    @DisplayName("pagina siguiente avanza hasta el maximo y se detiene ahi")
    void nextPageAdvancesUpToTheMax() {
        final int max = maxPagesFor(DISPLAY_SLOTS * 3);
        assertEquals(2, max);
        assertEquals(1, GridCache.nextPage(0, max));
        assertEquals(2, GridCache.nextPage(1, max));
        assertEquals(2, GridCache.nextPage(2, max));
    }
}
