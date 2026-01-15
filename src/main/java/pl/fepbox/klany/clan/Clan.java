package pl.fepbox.klany.clan;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class Clan {

    private final UUID uuid;
    private String tag;
    private String name;
    private String color;
    private final UUID ownerUuid;
    private final Instant createdAt;
    private final Map<UUID, ClanRole> members = new ConcurrentHashMap<>();
    private final Set<UUID> allies = new CopyOnWriteArraySet<>();

    public Clan(UUID uuid, String tag, String name, String color, UUID ownerUuid, Instant createdAt) {
        this.uuid = uuid;
        this.tag = tag;
        this.name = name;
        this.color = color;
        this.ownerUuid = ownerUuid;
        this.createdAt = createdAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<UUID, ClanRole> getMembers() {
        return Collections.unmodifiableMap(members);
    }

    public void setMemberRole(UUID uuid, ClanRole role) {
        members.put(uuid, role);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public Set<UUID> getAllies() {
        return Collections.unmodifiableSet(allies);
    }

    public void addAlly(UUID clanUuid) {
        allies.add(clanUuid);
    }

    public void removeAlly(UUID clanUuid) {
        allies.remove(clanUuid);
    }

    public void clearAllies() {
        allies.clear();
    }
}
