package io.github.sefiraat.networks;

import com.github.drakescraft_labs.slimefun4.implementation.Slimefun;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * MockBukkit + Slimefun Drake para tests que usan ItemStack / StackUtils.
 */
public abstract class BukkitTestSupport {

    @BeforeAll
    static void startMockBukkit() {
        if (!MockBukkit.isMocked()) {
            MockBukkit.mock();
            MockBukkit.load(Slimefun.class);
        }
    }

    @AfterAll
    static void stopMockBukkit() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }
}
