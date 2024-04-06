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
        /*
         * TODO - Would love to add a second sort here to organize by notes again (happens to be how it's sorted without the above sorting method) to reduce output file size.
         * Ideally by size of note so the ones with more information (generally the ones that lists had originally) would be at the top of the list, and therefor easier to see in dim.
         * This would also put anything without notes (usually just collections of perks) at the bottom.
         *
         * However, you can't sort by note list here because notes aren't unique entries so there's no way to map them back to the original list
         *
         * Alternatively, I could also sort inversely by the number of perk-sets under each note to achieve a similar affect.
         * Would need to see this in action BUT I'm not even sure I need to do this since dim already does this.
         * It would really just be for a minor file size reduction.
         */
        for (Map.Entry<Long, Item> item : itemMap.entrySet()) {
            try {
                item.getValue().getRollList().sort((Roll roll1, Roll roll2) -> {
                    // First, attempt to sort by notes to lower the file size
                    int noteComparison = String.join("", roll1.noteList).compareTo(String.join("", roll2.noteList));
                    if (noteComparison != 0) {
                        return noteComparison;
                    } else { // If notes are the same, sort by perks in reverse order
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
                    }
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
