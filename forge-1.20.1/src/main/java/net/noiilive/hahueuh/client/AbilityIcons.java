package net.noiilive.hahueuh.client;

import net.minecraft.client.Minecraft;
import net.noiilive.hahueuh.api.Ability;

public final class AbilityIcons {
    private AbilityIcons() {}

    public static boolean hasIcon(Ability ability) {
        return Minecraft.getInstance().getResourceManager().getResource(ability.iconLocation()).isPresent();
    }
}
