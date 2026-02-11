package pl.fepbox.klany.clan;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
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
    private boolean pvpEnabled;
    private int memberLevel;
    private int allyLevel;
    private final Map<String, ClanRank> ranks = new ConcurrentHashMap<>();
    private final Map<UUID, String> memberRanks = new ConcurrentHashMap<>();

    public Clan(UUID uuid, String tag, String name, String color, UUID ownerUuid, Instant createdAt) {
        this.uuid = uuid;
        this.tag = tag;
        this.name = name;
        this.color = color;
        this.ownerUuid = ownerUuid;
        this.createdAt = createdAt;
        this.pvpEnabled = true;
        this.memberLevel = 0;
        this.allyLevel = 0;
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

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public int getMemberLevel() {
        return memberLevel;
    }

    public void setMemberLevel(int memberLevel) {
        this.memberLevel = memberLevel;
    }

    public int getAllyLevel() {
        return allyLevel;
    }

    public void setAllyLevel(int allyLevel) {
        this.allyLevel = allyLevel;
    }

    public Map<String, ClanRank> getRanks() { return Collections.unmodifiableMap(ranks); }

    public void setRank(ClanRank rank) { ranks.put(rank.getName().toLowerCase(Locale.ROOT), rank); }

    public ClanRank getRank(String name) { return ranks.get(name.toLowerCase(Locale.ROOT)); }

    public void removeRank(String name) { ranks.remove(name.toLowerCase(Locale.ROOT)); }

    public void setMemberRank(UUID uuid, String rankName) { memberRanks.put(uuid, rankName); }

    public String getMemberRank(UUID uuid) { return memberRanks.get(uuid); }

    public Map<UUID, String> getMemberRanks() { return Collections.unmodifiableMap(memberRanks); }
}
