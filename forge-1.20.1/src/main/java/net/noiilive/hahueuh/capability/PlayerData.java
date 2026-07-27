package net.noiilive.hahueuh.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.noiilive.hahueuh.network.GateStatus;
import net.noiilive.hahueuh.network.PlayerRace;

public class PlayerData {
    public static final int DEFAULT_AGE = 16;

    private PlayerRace race = PlayerRace.HUMAN;
    private int age = DEFAULT_AGE;
    private int ageLastDay = -1;
    private GateStatus gateStatus = GateStatus.OPEN;
    private int gateDefectiveVariant = -1;
    private int gateOutput = -1;
    private int gateEfficiency = -1;
    private int gateStrain;
    private int manaCurrent;
    private int odCurrent = 100;
    private int lifespan = -1;
    private int reputation;
    private int magicSchools;
    private boolean odDepleted;
    private boolean hasTrappedEntities;
    private String storedSpell = "";
    private int spellHeat;
    private int heatLastResetDay = -1;

    public PlayerRace getRace() { return race; }
    public void setRace(PlayerRace race) { this.race = race; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public int getAgeLastDay() { return ageLastDay; }
    public void setAgeLastDay(int day) { this.ageLastDay = day; }

    public GateStatus getGateStatus() { return gateStatus; }
    public void setGateStatus(GateStatus status) { this.gateStatus = status; }

    public int getGateDefectiveVariant() { return gateDefectiveVariant; }
    public void setGateDefectiveVariant(int variant) { this.gateDefectiveVariant = variant; }

    public int getGateOutput() { return gateOutput; }
    public void setGateOutput(int output) { this.gateOutput = output; }

    public int getGateEfficiency() { return gateEfficiency; }
    public void setGateEfficiency(int efficiency) { this.gateEfficiency = efficiency; }

    public int getGateStrain() { return gateStrain; }
    public void setGateStrain(int strain) { this.gateStrain = strain; }

    public int getManaCurrent() { return manaCurrent; }
    public void setManaCurrent(int mana) { this.manaCurrent = mana; }

    public int getOdCurrent() { return odCurrent; }
    public void setOdCurrent(int od) { this.odCurrent = od; }

    public int getLifespan() { return lifespan; }
    public void setLifespan(int lifespan) { this.lifespan = lifespan; }

    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }

    public int getMagicSchools() { return magicSchools; }
    public void setMagicSchools(int schools) { this.magicSchools = schools; }

    public boolean isOdDepleted() { return odDepleted; }
    public void setOdDepleted(boolean depleted) { this.odDepleted = depleted; }

    public boolean hasTrappedEntities() { return hasTrappedEntities; }
    public void setHasTrappedEntities(boolean value) { this.hasTrappedEntities = value; }

    public String getStoredSpell() { return storedSpell; }
    public void setStoredSpell(String id) { this.storedSpell = id == null ? "" : id; }

    public int getSpellHeat() { return spellHeat; }
    public void setSpellHeat(int heat) { this.spellHeat = heat; }

    public int getHeatLastResetDay() { return heatLastResetDay; }
    public void setHeatLastResetDay(int day) { this.heatLastResetDay = day; }

    public void copyFrom(PlayerData other) {
        this.race = other.race;
        this.age = other.age;
        this.ageLastDay = other.ageLastDay;
        this.gateStatus = other.gateStatus;
        this.gateDefectiveVariant = other.gateDefectiveVariant;
        this.gateOutput = other.gateOutput;
        this.gateEfficiency = other.gateEfficiency;
        this.gateStrain = other.gateStrain;
        this.manaCurrent = other.manaCurrent;
        this.odCurrent = other.odCurrent;
        this.lifespan = other.lifespan;
        this.reputation = other.reputation;
        this.magicSchools = other.magicSchools;
        this.odDepleted = other.odDepleted;
        this.hasTrappedEntities = other.hasTrappedEntities;
        this.storedSpell = other.storedSpell;
        this.spellHeat = other.spellHeat;
        this.heatLastResetDay = other.heatLastResetDay;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Race", race.id);
        tag.putInt("Age", age);
        tag.putInt("AgeLastDay", ageLastDay);
        tag.putString("GateStatus", gateStatus.id);
        tag.putInt("GateDefectiveVariant", gateDefectiveVariant);
        tag.putInt("GateOutput", gateOutput);
        tag.putInt("GateEfficiency", gateEfficiency);
        tag.putInt("GateStrain", gateStrain);
        tag.putInt("ManaCurrent", manaCurrent);
        tag.putInt("OdCurrent", odCurrent);
        tag.putInt("Lifespan", lifespan);
        tag.putInt("Reputation", reputation);
        tag.putInt("MagicSchools", magicSchools);
        tag.putBoolean("OdDepleted", odDepleted);
        tag.putBoolean("HasTrappedEntities", hasTrappedEntities);
        tag.putString("StoredSpell", storedSpell);
        tag.putInt("SpellHeat", spellHeat);
        tag.putInt("HeatLastResetDay", heatLastResetDay);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        race = PlayerRace.byId(tag.getString("Race"));
        age = tag.contains("Age") ? tag.getInt("Age") : DEFAULT_AGE;
        ageLastDay = tag.contains("AgeLastDay") ? tag.getInt("AgeLastDay") : -1;
        gateStatus = GateStatus.byId(tag.getString("GateStatus"));
        gateDefectiveVariant = tag.contains("GateDefectiveVariant") ? tag.getInt("GateDefectiveVariant") : -1;
        gateOutput = tag.contains("GateOutput") ? tag.getInt("GateOutput") : -1;
        gateEfficiency = tag.contains("GateEfficiency") ? tag.getInt("GateEfficiency") : -1;
        gateStrain = tag.getInt("GateStrain");
        manaCurrent = tag.getInt("ManaCurrent");
        odCurrent = tag.contains("OdCurrent") ? tag.getInt("OdCurrent") : 100;
        lifespan = tag.contains("Lifespan") ? tag.getInt("Lifespan") : -1;
        reputation = tag.getInt("Reputation");
        magicSchools = tag.getInt("MagicSchools");
        odDepleted = tag.getBoolean("OdDepleted");
        hasTrappedEntities = tag.getBoolean("HasTrappedEntities");
        storedSpell = tag.getString("StoredSpell");
        spellHeat = tag.getInt("SpellHeat");
        heatLastResetDay = tag.contains("HeatLastResetDay") ? tag.getInt("HeatLastResetDay") : -1;
    }

    public static PlayerData get(Player player) {
        return player.getCapability(ModCapabilities.PLAYER_DATA)
                .orElseThrow(() -> new IllegalStateException("Player has no HahUeuh player data capability"));
    }

    public static PlayerData getOrDefault(Player player) {
        return player.getCapability(ModCapabilities.PLAYER_DATA).orElseGet(PlayerData::new);
    }
}
