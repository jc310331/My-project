package com.jacob.companion;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
/** World-owned identity survives player respawns and unloaded entity chunks. */
public class CompanionRegistry extends SavedData {
    private UUID worldId=UUID.randomUUID();
    public UUID worldId() {setDirty();return worldId;}
    private CompoundTag entries=new CompoundTag();
    public static CompanionRegistry load(CompoundTag tag) { var d=new CompanionRegistry();d.entries=tag.getCompound("Entries");if(tag.hasUUID("WorldId"))d.worldId=tag.getUUID("WorldId");return d; }
    public boolean contains(UUID owner) { return entries.contains(owner.toString()); }
    public String location(UUID owner) { return entries.getCompound(owner.toString()).getString("Spawn"); }
    public void remove(UUID owner) { entries.remove(owner.toString());setDirty(); }
    public void put(UUID owner,UUID entity,String location) { var t=new CompoundTag();t.putUUID("Entity",entity);t.putString("Spawn",location);entries.put(owner.toString(),t);setDirty(); }
    @Override public CompoundTag save(CompoundTag tag) {tag.putUUID("WorldId",worldId);tag.put("Entries",entries);return tag;}
}
