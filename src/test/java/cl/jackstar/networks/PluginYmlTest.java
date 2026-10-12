package cl.jackstar.networks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * La identidad del plugin en produccion no se negocia: el nombre decide la carpeta de datos
 * (plugins/NetworksV6-Drake: quantum storage y config) y el orden de carga respecto a los addons Drake.
 * Esta prueba rompe el build si alguien vuelve a pegar el plugin.yml del upstream (name: Networks).
 */
class PluginYmlTest {

    private static Map<String, Object> yml;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void load() throws Exception {
        try (InputStream in = PluginYmlTest.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in, "plugin.yml debe estar en el classpath");
            yml = new Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(String key) {
        Object value = yml.get(key);
        return value == null ? List.of() : (List<String>) value;
    }

    @Test
    void keepsProductionIdentity() {
        assertEquals("NetworksV6-Drake", yml.get("name"));
        assertEquals("io.github.sefiraat.networks.Networks", yml.get("main"));
        assertEquals("1.21", String.valueOf(yml.get("api-version")));
    }

    @Test
    void requiresSlimefun() {
        assertTrue(list("depend").contains("Slimefun"));
    }

    @Test
    void loadsAfterTheDrakeAddonsItIntegratesWith() {
        List<String> soft = list("softdepend");
        for (String addon : List.of("InfinityExpansion", "Supreme", "Cultivation", "GeneticChickengineering", "DynaTech",
            "FluffyMachines", "FoxyMachines", "SlimefunWarfare", "AlchimiaVitae", "SlimeHUD")) {
            assertTrue(soft.contains(addon), "falta en softdepend: " + addon);
        }
    }

    @Test
    void declaresSqliteDriverAsLibrary() {
        assertTrue(list("libraries").stream().anyMatch(l -> l.startsWith("org.xerial:sqlite-jdbc")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminPermissionAndCommandAreProtected() {
        Map<String, Object> permissions = (Map<String, Object>) yml.get("permissions");
        assertTrue(permissions.containsKey("networks.admin"));
        Map<String, Object> networks = (Map<String, Object>) ((Map<String, Object>) yml.get("commands")).get("networks");
        assertEquals("networks.admin", networks.get("permission"));
    }
}
