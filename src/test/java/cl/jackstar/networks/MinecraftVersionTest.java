package cl.jackstar.networks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.balugaq.netex.api.enums.MinecraftVersion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Vigila que el plugin reconozca las versiones que se prueban con ./gradlew paperCompat.
 *
 * Desde 2026 Mojang numera por año: despues de 1.21.11 viene 26.1, 26.2... El plugin decide que
 * comprobaciones de items usar con isAtLeast(1.20.5) e isAtLeast(1.21) (StackUtils). Si una 26.x se
 * leyera como "anterior a 1.21", se saltarian comprobaciones de componentes y dos items distintos
 * podrian contarse como iguales en la red.
 */
class MinecraftVersionTest {

    @ParameterizedTest
    @CsvSource({
        "1.21.11, 1, 21, 11",
        "26.1, 26, 1, 0",
        "26.1.2, 26, 1, 2",
        "26.2, 26, 2, 0",
    })
    void leeLasVersionesProbadas(String texto, int major, int minor, int patch) {
        assertEquals(MinecraftVersion.of(major, minor, patch), MinecraftVersion.of(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.21.11", "26.1", "26.1.2", "26.2"})
    void lasVersionesProbadasActivanLasComprobacionesModernas(String texto) {
        MinecraftVersion version = MinecraftVersion.of(texto);
        assertTrue(version.isAtLeast(MinecraftVersion.V1_20_5));
        assertTrue(version.isAtLeast(MinecraftVersion.V1_21));
        assertFalse(version.isBefore(MinecraftVersion.V1_21));
    }

    @ParameterizedTest
    @ValueSource(strings = {"26.1", "26.2"})
    void lasVersionesPorAnioVanDespuesDe1_21_11(String texto) {
        MinecraftVersion version = MinecraftVersion.of(texto);
        assertTrue(version.compareTo(MinecraftVersion.of("1.21.11")) > 0);
        assertTrue(version.isAtLeast("1.21.11"));
    }

    @ParameterizedTest
    @CsvSource({
        "1.21.11-pre5, 1, 21, 11",
        "1.21.11-rc1, 1, 21, 11",
        "26.1-rc-1, 26, 1, 0",
        "26.2-snapshot-3, 26, 2, 0",
    })
    void aceptaPreReleasesYReleaseCandidates(String texto, int major, int minor, int patch) {
        // Antes lanzaba NumberFormatException y el plugin no arrancaba.
        assertEquals(MinecraftVersion.of(major, minor, patch), MinecraftVersion.of(texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"26", "", "x.1", "1.x"})
    void rechazaTextoSinVersion(String texto) {
        assertThrows(IllegalArgumentException.class, () -> MinecraftVersion.of(texto));
    }
}
