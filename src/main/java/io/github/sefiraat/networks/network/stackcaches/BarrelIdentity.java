package io.github.sefiraat.networks.network.stackcaches;

import io.github.sefiraat.networks.network.barrel.BarrelCore;
import io.github.sefiraat.networks.network.barrel.BarrelType;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

public abstract class BarrelIdentity extends ItemStackCache implements BarrelCore {

    private final Location location;
    private volatile int amount;
    private final BarrelType type;

    @ParametersAreNonnullByDefault
    protected BarrelIdentity(Location location, ItemStack itemStack, int amount, BarrelType type) {
        super(itemStack);
        this.location = location;
        this.amount = amount;
        this.type = type;
    }

    public Location getLocation() {
        return this.location;
    }

    public int getAmount() {
        return this.amount;
    }

    protected void setAmount(int amount) {
        this.amount = amount;
    }

    public BarrelType getType() {
        return this.type;
    }
}
