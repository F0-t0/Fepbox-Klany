package pl.fepbox.klany.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.fepbox.klany.clan.Clan;
import pl.fepbox.klany.clan.ClanPermission;
import pl.fepbox.klany.clan.ClanRank;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.util.ColorUtil;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RankPermissionMenu implements Listener {

    private final ClanService clanService;
    private final Map<ClanPermission, Material> permIcons = new EnumMap<>(ClanPermission.class);

    public RankPermissionMenu(ClanService clanService) {
        this.clanService = clanService;
        permIcons.put(ClanPermission.INVITE, Material.LIME_DYE);
        permIcons.put(ClanPermission.KICK, Material.RED_DYE);
        permIcons.put(ClanPermission.COLOR, Material.ORANGE_DYE);
        permIcons.put(ClanPermission.ALLY, Material.LIGHT_BLUE_DYE);
        permIcons.put(ClanPermission.UPGRADE, Material.PURPLE_DYE);
        permIcons.put(ClanPermission.PVP, Material.GRAY_DYE);
        permIcons.put(ClanPermission.MANAGE_RANKS, Material.YELLOW_DYE);
    }

    public void open(Player player, Clan clan, ClanRank rank) {
        Inventory inv = Bukkit.createInventory(new RankHolder(clan, rank), 27, ColorUtil.colorize("<GOLD>Permisje: <YELLOW>" + rank.getName()));
        int slot = 10;
        for (ClanPermission perm : ClanPermission.values()) {
            inv.setItem(slot, buildItem(perm, rank.has(perm)));
            slot++;
            if (slot == 17) slot = 19;
        }
        player.openInventory(inv);
    }

    private ItemStack buildItem(ClanPermission perm, boolean enabled) {
        ItemStack item = new ItemStack(permIcons.getOrDefault(perm, Material.GRAY_DYE));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize((enabled ? "<GREEN>" : "<RED>") + perm.name()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize(enabled ? "<GREEN>Wlaczona" : "<RED>Wylaczona"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RankHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int raw = event.getRawSlot();
        Clan clan = holder.clan();
        ClanRank rank = holder.rank();
        if (clan == null || rank == null) {
            player.closeInventory();
            return;
        }
        if (raw < 0 || raw >= event.getInventory().getSize()) return;

        // map slots to permissions (same iteration order)
        int idx = (raw <= 16) ? raw - 10 : raw - 19 + 7; // positions used
        if (idx < 0 || idx >= ClanPermission.values().length) return;
        ClanPermission perm = ClanPermission.values()[idx];
        boolean toggled = clanService.togglePermission(clan, rank.getName(), perm);
        if (toggled) {
            player.sendMessage(ColorUtil.colorize("<YELLOW>Przelaczono permisje <WHITE>" + perm.name() + " <YELLOW>dla rangi <WHITE>" + rank.getName()));
            open(player, clan, clan.getRank(rank.getName()));
        } else {
            player.sendMessage(ColorUtil.colorize("<RED>Nie mozna zmienic tej permisji."));
        }
    }

    private record RankHolder(Clan clan, ClanRank rank) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
