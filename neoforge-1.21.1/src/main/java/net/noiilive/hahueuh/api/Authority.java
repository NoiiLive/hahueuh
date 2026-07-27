package net.noiilive.hahueuh.api;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public final class Authority {
    private final ResourceLocation id;
    private final String translationKey;
    private final int sortPriority;

    private Authority(Builder builder) {
        this.id = builder.id;
        this.translationKey = builder.translationKey;
        this.sortPriority = builder.sortPriority;
    }

    public ResourceLocation id() { return id; }
    public String translationKey() { return translationKey; }
    public int sortPriority() { return sortPriority; }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private String translationKey;
        private int sortPriority;

        private Builder(ResourceLocation id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder translationKey(String translationKey) {
            this.translationKey = translationKey;
            return this;
        }

        public Builder sortPriority(int sortPriority) {
            this.sortPriority = sortPriority;
            return this;
        }

        public Authority build() {
            if (translationKey == null) {
                throw new IllegalArgumentException("Authority " + id + " has no translationKey");
            }
            return new Authority(this);
        }
    }

    @Override
    public String toString() {
        return "Authority[" + id + "]";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Authority other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
