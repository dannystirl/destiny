# Ignores class items and sunset armor, as well as items already tagged for removal in DIM.

# Can be converted to executable using pyinstaller --onefile --console pythonScriptName.py

from asyncio.windows_events import NULL
from cmath import log
import copy
from msilib.schema import File
import csv
import os
import sys
# Config options
keepRaid = False
skipMob = False
skipRes = False
skipRec = False
skipDis = False
skipInt = False
skipStr = False

testClasses = {"Warlock", "Hunter", "Titan"}

csvColumns = {}

class armorPiece:
    def __init__(self, info):
        # Set variables
        try:
            self.name = info[csvColumns.index('Name')]
            self.hash = info[csvColumns.index('Hash')]
            self.id = info[csvColumns.index('Id')]
            self.tag = info[csvColumns.index('Tag')]
            self.artifice = (info[csvColumns.index('Seasonal Mod')] == 'artifice')
            self.modslot = info[csvColumns.index('Seasonal Mod')]
            self.tier = info[csvColumns.index('Tier')]
            self.type = info[csvColumns.index('Type')]
            self.equippable = info[csvColumns.index('Equippable')]
            self.power = int(info[csvColumns.index('Power')])
            self.powerLimit = (info[csvColumns.index('Power Limit')] != "")
            #! Armor 1.0 exotics have no masterwork so this is required.
            try:
                self.masterworkTier = int(info[csvColumns.index('Energy Capacity')])
            except Exception as e:
                self.masterworkTier = 0

            self.mob = int(info[csvColumns.index('Mobility (Base)')])
            self.res = int(info[csvColumns.index('Resilience (Base)')])
            self.rec = int(info[csvColumns.index('Recovery (Base)')])
            self.dis = int(info[csvColumns.index('Discipline (Base)')])
            self.int = int(info[csvColumns.index('Intellect (Base)')])
            self.str = int(info[csvColumns.index('Strength (Base)')])
            self.total = int(info[csvColumns.index('Total (Base)')])
        except Exception as e:
            print(e)
            print("Something went wrong with " + self.name)

    # Determine if stats of two pieces are identical
    def identicalStats(self, test):
        return all(getattr(self, stat) == getattr(test, stat) for stat in ['mob', 'res', 'rec', 'dis', 'int', 'str'])

    # Determine if stats of a piece are better than stats of another piece
    def isBetter(self, test):
        # Skip different armor tiers
        if self == test or self.tier != test.tier:
            return False
        # Only compare an exotic item to itself
        if self.tier == "Exotic" and test.tier == "Exotic" and self.name != test.name:
            return False
        # Check classes and slot are the same
        if self.equippable != test.equippable or self.type != test.type or self.equippable not in testClasses:
            return False
        # Skip class items
        if self.type in ["Warlock Bond", "Hunter Cloak", "Titan Mark"]:
            return self.modslot != '' and test.modslot == ''
        # Keep raid items?
        if keepRaid and test.modslot != '' and not test.artifice and self.modslot != test.modslot:
            return False
        #! Skip if stats are completely identical
        if self.identicalStats(test):
            return True
        # Check if all stats are equal to or better than test piece, respecting config options
        statNames = ['mob', 'res', 'rec', 'dis', 'int', 'str']
        # Remove unwanted stats
        for statName in statNames:
            if globals()["skip{}".format(statName.capitalize())]:
                statNames.remove(statName)
        # Test if current armor is better
        checkList = []
        for otherStatName in statNames:
            checkList.append(getattr(self, otherStatName) >= getattr(test, otherStatName))

        # Test artifice armor stat boosts
        if self.artifice:
            for statName in statNames:
                checkList = []
                checkList.append(getattr(self, statName) + 3 >= getattr(test, statName))
                for otherStatName in [x for x in statNames if x != statName]:
                    checkList.append(getattr(self, otherStatName) >= getattr(test, otherStatName))
                if all(ele == True for ele in checkList):
                    return True
        elif test.artifice:
            for statName in statNames:
                checkList = []
                checkList.append(getattr(self, statName) >= getattr(test, statName) + 3)
                for otherStatName in [x for x in statNames if x != statName]:
                    checkList.append(getattr(self, otherStatName) >= getattr(test, otherStatName))
                if any(ele == False for ele in checkList):
                    return False
        return all(ele == True for ele in checkList)

    # Simpler way to print armor piece
    def shortStr(self):
        return str(self.name) + "," + str(self.equippable) + "," + str(self.type) + "," + str(self.power) + "," + str(self.total) + "," + str(self.masterworkTier) + "," + str(self.id)


def run():
    global keepRaid, skipMob, skipRec, skipRes, skipDis, skipInt, skipStr, testClasses, csvColumns
    yes = ['Y','YES']
    #Prompting and config
    print("Setup: Decide what parameters to use. Press Y for yes, any other key for no.")
    classes = input("Classes to check? W,H,T (Default: All)\n")
    keepRaid = input("Keep one of each raid mod slot? Y/N (Default: No)\n").upper() in yes
    skipMob = input("Ignore Mobility? Y/N (Default: No)\n").upper() in yes
    skipRes = input("Ignore Resilience? Y/N (Default: No)\n").upper() in yes
    skipRec = input("Ignore Recovery? Y/N (Default: No)\n").upper() in yes
    skipDis = input("Ignore Discipline? Y/N (Default: No)\n").upper() in yes
    skipInt = input("Ignore Intellect? Y/N (Default: No)\n").upper() in yes
    skipStr = input("Ignore Strength? Y/N (Default: No)\n").upper() in yes

    if len(classes) > 0:
        if "W" not in str(classes).upper():
            testClasses.remove("Warlock")
        if "H" not in str(classes).upper():
            testClasses.remove("Hunter")
        if "T" not in str(classes).upper():
            testClasses.remove("Titan")

    # Open CSV from DIM
    with open(os.path.abspath('input/destinyArmor.csv'), newline='', errors="ignore") as f:
        reader = csv.reader(f)
        rawArmorList = list(reader)
        armorList = []
        
        csvColumns = rawArmorList[0]
        # List of all pieces
        for currentArmor in rawArmorList[1:]:
            if armorPiece(currentArmor).tag not in {"junk", "infuse", "archive"}:
                armorList.append(armorPiece(currentArmor))

        # Create list of comparisons
        simpleSuperiorityList = {}
        idToItemList = {}

        # For each armor piece
        for currentArmor in armorList:
            # Create a list to store the testArmor IDs with the same currentArmor
            testArmorList = []

            # Compare it to every other armor piece
            for testArmor in armorList:
                # If the piece is better and its powerLimit is False, add its ID to the testArmorList
                if currentArmor.isBetter(testArmor) and not testArmor.powerLimit:
                    testArmorList.append(testArmor.id)

            # Add an entry to idToItemMap with the currentArmor ID as the key and the item as the value
            idToItemList[currentArmor.id] = currentArmor

            # Store the testArmorList as the value for the currentArmor ID key in the simpleSuperiorityList
            if testArmorList: # If it is not empty
                simpleSuperiorityList[currentArmor.id] = testArmorList
                
        # Iterate over the keys and values in the simpleSuperiorityList
        for key1, value1 in simpleSuperiorityList.items():
            for key2 in value1:
                # Check if key2 is in the simpleSuperiorityList and if key1 is in the corresponding list
                if key2 in simpleSuperiorityList and key1 in simpleSuperiorityList[key2]:
                    # Remove key2 from key1's list
                    value1.remove(key2)
                    # Remove key1 from key2's list
                    simpleSuperiorityList[key2].remove(key1)
            
        uniqueValues = list(set(item for sublist in simpleSuperiorityList.values() for item in sublist))

        # Display
        original_stdout = sys.stdout
        with open('output/ArmorExamined.txt', 'w') as f:
            sys.stdout = f
            for betterArmorPieceId in simpleSuperiorityList:
                if idToItemList[betterArmorPieceId].type != "Warlock Bond" and idToItemList[betterArmorPieceId].type != "Hunter Cloak" and idToItemList[betterArmorPieceId].type != "Titan Mark":
                    for worseArmorPiece in simpleSuperiorityList[betterArmorPieceId]:
                        print(
                            f'id:{idToItemList[betterArmorPieceId].id} or id:{idToItemList[worseArmorPiece].id}')
                        printformatted(
                            idToItemList[betterArmorPieceId], idToItemList[worseArmorPiece])
            
            print("Vault Spaces Saveable: " + str(len(uniqueValues)), end="\n\n")
            armorSet = set()
            printstr = "DIM string for items to delete:      \n"
            for betterArmorPieceId in simpleSuperiorityList:
                for worseArmorPieceId in simpleSuperiorityList[betterArmorPieceId]:
                    if worseArmorPieceId not in armorSet:
                        armorSet.add(worseArmorPieceId)
                        printstr += f'id:{worseArmorPieceId} or '
            print(printstr[0:len(printstr)-4])

        sys.stdout = original_stdout  #? Reset the standard output to its original value


def printformatted(better, worse):
    print("%s : %s (%s %s at %s power) > \n%s : %s (%s %s at %s power)\n" %
          (better.id, better.name, better.equippable, better.type, better.power, worse.id, worse.name, worse.equippable, worse.type, worse.power))


if __name__ == "__main__":
    run()
