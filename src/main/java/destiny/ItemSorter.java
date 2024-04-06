package destiny;

import junit.framework.AssertionFailedError;

import java.util.List;
import java.util.Map;

public class ItemSorter {

    App.RunType runType;

    ItemSorter(App.RunType runType) {
        this.runType = runType;
    }

    /**
     * Sort each item in itemList by the perkList, starting with the final entry in each perkList
     *
     * @param itemMap - Map of ItemId -> Item to sort
     */
    Map<Long, Item> sortItems(Map<Long, Item> itemMap) {
        for (Map.Entry<Long, Item> item : itemMap.entrySet()) {
            try {
                item.getValue().getRollList().sort((Roll roll1, Roll roll2) -> {
                    List<String> roll1PerkList = roll1.getPerkList();
                    List<String> roll2PerkList = roll2.getPerkList();
                    int roll1perkSize = roll1PerkList.size();
                    int roll2perkSize = roll2PerkList.size();
                    for (int i = 0; i < Math.min(roll1perkSize, roll2perkSize); i++) {
                        if (!roll1PerkList.get(roll1perkSize - i - 1).equals(roll2PerkList.get(roll2perkSize - i - 1))) {
                            return roll1PerkList.get(roll1perkSize - i - 1).compareTo(roll2PerkList.get(roll2perkSize - i - 1));
                        }
                    }
                    return roll1PerkList.get(0).compareTo(roll2PerkList.get(0));
                });
            } catch (Exception e) {
                if (runType == App.RunType.NORMAL) {
                    Formatters.errorPrint("Error sorting item " + item.getKey(), e);
                } else {
                    throw new AssertionFailedError("Unable to generate list from item");
                }
            }
        }
        return itemMap;
    }
}
