package com.example.hahueuh.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Ability {
    private final ResourceLocation id;
    private final ResourceLocation authorityId;
    private final boolean holdBased;
    private final Supplier<String> translationKey;
    private final Supplier<String> shortLabel;
    private final Supplier<ResourceLocation> iconLocation;
    private final AbilityBehavior.Tap onActivate;
    private final AbilityBehavior.Held onHeldTick;

    private Ability(Builder builder) {
        this.id = builder.id;
        this.authorityId = builder.authorityId;
        this.holdBased = builder.holdBased;
        this.translationKey = builder.translationKey;
        this.shortLabel = builder.shortLabel;
        this.iconLocation = builder.iconLocation;
        this.onActivate = builder.onActivate;
        this.onHeldTick = builder.onHeldTick;
    }

    public ResourceLocation id() { return id; }
    public ResourceLocation authorityId() { return authorityId; }
    public boolean holdBased() { return holdBased; }
    public String translationKey() { return translationKey.get(); }
    public String shortLabel() { return shortLabel.get(); }
    public ResourceLocation iconLocation() { return iconLocation.get(); }

    public void onActivate(AbilityContext ctx) {
        if (onActivate != null) onActivate.onActivate(ctx);
    }

    public void onHeldTick(AbilityContext ctx, boolean down) {
        if (onHeldTick != null) onHeldTick.onHeldTick(ctx, down);
    }

    public int cooldownSecondsRemaining() {
        return AbilityCooldowns.secondsRemaining(id);
    }

    public static Builder builder(ResourceLocation id, ResourceLocation authorityId) {
        return new Builder(id, authorityId);
    }

    public static Ability simple(ResourceLocation id, ResourceLocation authorityId, String translationKey,
                                  Consumer<AbilityContext> onActivate) {
        return builder(id, authorityId)
                .translationKey(translationKey)
                .onActivate(ctx -> onActivate.accept(ctx))
                .build();
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final ResourceLocation authorityId;
        private boolean holdBased;
        private Supplier<String> translationKey;
        private Supplier<String> shortLabel;
        private Supplier<ResourceLocation> iconLocation;
        private AbilityBehavior.Tap onActivate;
        private AbilityBehavior.Held onHeldTick;

        private Builder(ResourceLocation id, ResourceLocation authorityId) {
            this.id = Objects.requireNonNull(id, "id");
            this.authorityId = Objects.requireNonNull(authorityId, "authorityId");
            this.shortLabel = () -> id.getPath().substring(0, Math.min(3, id.getPath().length())).toUpperCase(Locale.ROOT);
            this.iconLocation = () -> ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/gui/icons/" + id.getPath() + ".png");
        }

        public Builder holdBased() {
            this.holdBased = true;
            return this;
        }

        public Builder translationKey(Supplier<String> translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        public Builder translationKey(String translationKey) {
            return translationKey(() -> translationKey);
        }

        public Builder shortLabel(Supplier<String> shortLabel) {
            this.shortLabel = shortLabel;
            return this;
        }

        public Builder icon(Supplier<ResourceLocation> iconLocation) {
            this.iconLocation = iconLocation;
            return this;
        }

        public Builder icon(ResourceLocation iconLocation) {
            return icon(() -> iconLocation);
        }

        public Builder onActivate(AbilityBehavior.Tap onActivate) {
            this.onActivate = onActivate;
            return this;
        }

        public Builder onHeldTick(AbilityBehavior.Held onHeldTick) {
            this.onHeldTick = onHeldTick;
            return this;
        }

        public Ability build() {
            if (translationKey == null) {
                throw new IllegalArgumentException("Ability " + id + " has no translationKey");
            }
            if (holdBased && onHeldTick == null) {
                throw new IllegalArgumentException("Ability " + id + " is holdBased but has no onHeldTick behavior");
            }
            if (!holdBased && onActivate == null) {
                throw new IllegalArgumentException("Ability " + id + " is tap-based but has no onActivate behavior");
            }
            if (holdBased && onActivate != null) {
                throw new IllegalArgumentException("Ability " + id + " is holdBased and must not have an onActivate behavior");
            }
            if (!holdBased && onHeldTick != null) {
                throw new IllegalArgumentException("Ability " + id + " is tap-based and must not have an onHeldTick behavior");
            }
            return new Ability(this);
        }
    }

    @Override
    public String toString() {
        return "Ability[" + id + "]";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Ability other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
