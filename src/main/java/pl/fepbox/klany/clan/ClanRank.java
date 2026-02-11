package pl.fepbox.klany.clan;

import java.util.EnumSet;
import java.util.Set;

public class ClanRank {
    private final String name;
    private final int weight;
    private final Set<ClanPermission> permissions;

    public ClanRank(String name, int weight, Set<ClanPermission> permissions) {
        this.name = name;
        this.weight = weight;
        this.permissions = EnumSet.copyOf(permissions);
    }

    public String getName() {
        return name;
    }

    public int getWeight() {
        return weight;
    }

    public Set<ClanPermission> getPermissions() {
        return permissions;
    }

    public boolean has(ClanPermission perm) {
        return permissions.contains(perm);
    }

    public void toggle(ClanPermission perm) {
        if (permissions.contains(perm)) {
            permissions.remove(perm);
        } else {
            permissions.add(perm);
        }
    }
}
