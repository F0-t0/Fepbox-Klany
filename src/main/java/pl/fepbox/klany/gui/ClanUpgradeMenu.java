package pl.fepbox.klany.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.fepbox.klany.clan.Clan;
import pl.fepbox.klany.clan.ClanService;
import pl.fepbox.klany.clan.ClanUpgradeType;
import pl.fepbox.klany.config.ClanSettingsConfig;
import pl.fepbox.klany.config.PluginConfig;
import pl.fepbox.klany.util.ColorUtil;

import java.util.List;

public class ClanUpgradeMenu implements Listener {

    private final PluginConfig pluginConfig;
    private final ClanService clanService;
    private final ClanSettingsConfig settings;

    public ClanUpgradeMenu(PluginConfig pluginConfig, ClanService clanService) {
        this.pluginConfig = pluginConfig;
        this.clanService = clanService;
        this.settings = pluginConfig.getClanSettings();
    }

    public void open(Player player, Clan clan) {
        Inventory inv = Bukkit.createInventory(new UpgradeHolder(clan), 27, ColorUtil.colorize(settings.getUpgradesGuiTitle()));
        inv.setItem(11, buildUpgradeItem(clan, ClanUpgradeType.MEMBERS));
        inv.setItem(15, buildUpgradeItem(clan, ClanUpgradeType.ALLIES));
        player.openInventory(inv);
    }

    private ItemStack buildUpgradeItem(Clan clan, ClanUpgradeType type) {
        ClanSettingsConfig.UpgradeSettings cfg = type == ClanUpgradeType.MEMBERS ? settings.getMemberUpgrade() : settings.getAllyUpgrade();
        ItemStack base = new ItemStack(type == ClanUpgradeType.MEMBERS ? Material.PLAYER_HEAD : Material.SHIELD);
        ItemMeta meta = base.getItemMeta();
        int currentLevel = type == ClanUpgradeType.MEMBERS ? clan.getMemberLevel() : clan.getAllyLevel();
        int nextLevel = currentLevel + 1;
        int limit = type == ClanUpgradeType.MEMBERS ? clanService.getMemberLimit(clan) : clanService.getAllyLimit(clan);
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(cfg.getItemName() + " <GRAY>(Lv." + currentLevel + ")"));
            int cost = getCost(cfg, nextLevel);
            List<String> lore = cfg.getItemLore();
            lore = lore.stream().map(ColorUtil::colorize).toList();
            lore = new java.util.ArrayList<>(lore);
            lore.add(ColorUtil.colorize("<YELLOW>Limit: <WHITE>" + limit));
            if (nextLevel <= cfg.getMaxLevel()) {
                lore.add(ColorUtil.colorize("<GREEN>Kolejny poziom koszt: <WHITE>" + cost));
            } else {
                lore.add(ColorUtil.colorize("<GRAY>Osiagnieto maksymalny poziom"));
            }
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            base.setItemMeta(meta);
        }
        return base;
    }

    private int getCost(ClanSettingsConfig.UpgradeSettings cfg, int nextLevel) {
        if (nextLevel - 1 < cfg.getCosts().size()) {
            return cfg.getCosts().get(nextLevel - 1);
        }
        return cfg.getCosts().isEmpty() ? 1 : cfg.getCosts().get(cfg.getCosts().size() - 1);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof UpgradeHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Clan clan = holder.clan();
        if (clan == null) {
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        ClanUpgradeType type = null;
        if (slot == 11) {
            type = ClanUpgradeType.MEMBERS;
        } else if (slot == 15) {
            type = ClanUpgradeType.ALLIES;
        }
        if (type == null) {
            return;
        }
        boolean ok = clanService.tryUpgrade(clan, type, player);
        if (ok) {
            player.sendMessage(ColorUtil.colorize("<GREEN>Ulepszono klan!"));
        } else {
            player.sendMessage(ColorUtil.colorize("<RED>Brak waluty lub osiagnieto limit."));
        }
        open(player, clan);
    }

    private record UpgradeHolder(Clan clan) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}






