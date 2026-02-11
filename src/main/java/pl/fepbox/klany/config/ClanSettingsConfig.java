package pl.fepbox.klany.config;

import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ClanSettingsConfig {

    private final boolean defaultPvpEnabled;
    private final UpgradeSettings memberUpgrade;
    private final UpgradeSettings allyUpgrade;
    private final String upgradesGuiTitle;
    private final ClanInfoConfig info;
    private final ItemStack currencyItem;

    public ClanSettingsConfig(boolean defaultPvpEnabled,
                              UpgradeSettings memberUpgrade,
                              UpgradeSettings allyUpgrade,
                              String upgradesGuiTitle,
                              ClanInfoConfig info,
                              ItemStack currencyItem) {
        this.defaultPvpEnabled = defaultPvpEnabled;
        this.memberUpgrade = memberUpgrade;
        this.allyUpgrade = allyUpgrade;
        this.upgradesGuiTitle = upgradesGuiTitle;
        this.info = info;
        this.currencyItem = currencyItem;
    }

    public boolean isDefaultPvpEnabled() {
        return defaultPvpEnabled;
    }

    public UpgradeSettings getMemberUpgrade() {
        return memberUpgrade;
    }

    public UpgradeSettings getAllyUpgrade() {
        return allyUpgrade;
    }

    public String getUpgradesGuiTitle() {
        return upgradesGuiTitle;
    }

    public ClanInfoConfig getInfo() {
        return info;
    }

    public ItemStack getCurrencyItem() {
        return currencyItem;
    }

    public static class UpgradeSettings {
        private final int baseLimit;
        private final int step;
        private final int maxLevel;
        private final List<Integer> costs;
        private final String itemName;
        private final List<String> itemLore;

        public UpgradeSettings(int baseLimit, int step, int maxLevel, List<Integer> costs, String itemName, List<String> itemLore) {
            this.baseLimit = baseLimit;
            this.step = step;
            this.maxLevel = maxLevel;
            this.costs = costs;
            this.itemName = itemName;
            this.itemLore = itemLore;
        }

        public int getBaseLimit() {
            return baseLimit;
        }

        public int getStep() {
            return step;
        }

        public int getMaxLevel() {
            return maxLevel;
        }

        public List<Integer> getCosts() {
            return costs;
        }

        public String getItemName() {
            return itemName;
        }

        public List<String> getItemLore() {
            return itemLore;
        }
    }

    public static class ClanInfoConfig {
        private final String title;
        private final List<String> lines;

        public ClanInfoConfig(String title, List<String> lines) {
            this.title = title;
            this.lines = lines;
        }

        public String getTitle() {
            return title;
        }

        public List<String> getLines() {
            return lines;
        }
    }
}
