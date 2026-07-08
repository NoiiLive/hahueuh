package com.example.hahueuh.client;

import com.example.hahueuh.api.Ability;
import net.minecraft.client.Minecraft;

public final class AbilityIcons {
    private AbilityIcons() {}

    public static boolean hasIcon(Ability ability) {
        return Minecraft.getInstance().getResourceManager().getResource(ability.iconLocation()).isPresent();
    }
}
