# Most of this code was written by u/ParasiticUniverse and u/MrFlood360, but has been formatted and sipmlified. Ignores class items and sunset armor, as well as items already tagged for removal in DIM. 

from asyncio.windows_events import NULL
from msilib.schema import File
import csv
import sys
# Config options
skipMob = True
skipRec = False
skipRes = False
skipDis = False
skipInt = False
skipStr = False

testClasses = {"Warlock", "Hunter", "Titan"}

class armorPiece:
    def __init__(self, info):
        # Set variables
        try:
            self.name = info[0]
            self.hash = info[1]
            self.id = info[2]
            self.tag = info[3]
            self.tier = info[4]
            self.type = info[5]
            self.equippable = info[7]
            self.power = int(info[8])
            if info[9] == "":
                self.powerLimit = False
            else:
                self.powerLimit = True

            # Armor 1.0 exotics have no masterwork so this is required.
            try:
                self.master = info[10].split()[0]
                self.masterTier = int(info[11])

            except Exception as e:
                self.master = 'None'
                self.masterTier = 0

            self.mob = int(info[27])
            self.res = int(info[28])
            self.rec = int(info[29])
            self.dis = int(info[30])
            self.int = int(info[31])
            self.str = int(info[32])
            self.total = int(info[33])

        except Exception as e:
            print(e)
            print("Something went wrong with " + self.name)

    # Determine if stats of two pieces are identical
    def identicalStats(self, test):
        return self.mob == test.mob and self.res == test.res and self.rec == test.rec and self.dis == test.dis and self.int == test.int and self.str == test.str

    # Determine if stats of a piece are better than stats of another piece
    def isBetter(self, test):
        # Skip comparing piece to self and exotics
        if self.type == "Warlock Bond" or self.type == "Hunter Cloak" or self.type == "Titan Mark":
            return False
        if self == test or self.tier == "Exotic" or test.tier == "Exotic":
            return False
        # Check classes and slot are the same
        if self.equippable == test.equippable and self.type == test.type and self.equippable in testClasses:
            # Skip if stats are completely identical
            if self.identicalStats(test):
                return False
            # Check if all stats are equal to or better than test piece, respecting config options
            return (self.mob >= test.mob or skipMob) and (
                self.res >= test.res
                or skipRes) and (self.rec >= test.rec or skipRec) and (
                    self.dis >= test.dis
                    or skipDis) and (self.int >= test.int or skipInt) and (
                        self.str >= test.str
                        or skipStr)
        return False

    # Simpler way to print armor piece
    def shortStr(self):
        return str(self.name) + "," + str(self.equippable) + "," + str(self.type) + "," + str(self.power) + "," + str(self.total) + "," + self.master + "," + str(self.masterTier) + "," + str(self.id)


def run():
    yes = "[Y', 'YES']"
    #Prompting and config
    print("Setup: Decide what parameters to use. Press Y for yes, any other key for no.")
    global skipMob, skipRec, skipRes, skipDis, skipInt, skipStr
    """ skipMob = input("Ignore Mobility? Y/N (Default: No)\n").upper() in yes
    skipRec = input("Ignore Recovery? Y/N (Default: No)\n").upper() in yes
    skipRes = input("Ignore Resilience? Y/N (Default: No)\n").upper() in yes
    skipDis = input("Ignore Discipline? Y/N (Default: No)\n").upper() in yes
    skipInt = input("Ignore Intellect? Y/N (Default: No)\n").upper() in yes
    skipStr = input("Ignore Strength? Y/N (Default: No)\n").upper() in yes """
    
    classes = input("Classes to check? W,H,T (Default: All)\n")

    if "W" not in str(classes).upper():
        testClasses.remove("Warlock")
    if "H" not in str(classes).upper():
        testClasses.remove("Hunter")
    if "T" not in str(classes).upper():
        testClasses.remove("Titan")

    # Open CSV from DIM
    with open('src/main/data/destiny/destinyArmor.csv', newline='') as f:
        reader = csv.reader(f)
        rawArmorList = list(reader)
        armorList = []

        # List of all pieces
        for currentArmor in rawArmorList[2:]:
            if (armorPiece(currentArmor).tag != "junk" and armorPiece(currentArmor).tag != "infuse" and armorPiece(currentArmor).tier == "Legendary"):
                armorList.append(armorPiece(currentArmor))

        # Create list of comparisons
        superiorityList = []
        for currentArmor in armorList:
            for testArmor in armorList:
                if currentArmor.isBetter(testArmor) and testArmor.powerLimit == False:
                    superiorityList.append(
                        (currentArmor.shortStr(), testArmor.shortStr()))

        # Lists of armor to keep and shard
        bestArmor = list(set([armor[0] for armor in superiorityList]))
        worstArmor = list(set([armor[1] for armor in superiorityList]))

        simpleSuperiorityList = []

        # Formatting the list so that each piece to keep is listed next to which pieces it supersedes
        for currentArmor in bestArmor:
            badArmorList = [
                armor[1] for armor in superiorityList
                if armor[0] == currentArmor
            ]
            simpleSuperiorityList.append([currentArmor, badArmorList])

        # Display
        original_stdout = sys.stdout
        with open('output/ArmorExamined.txt', 'w') as f:
            sys.stdout = f
            for element in simpleSuperiorityList:
                print("id:" + element[0].split(',')[7].replace("\"", "") + " or id:" + element[1][0].split(',')[7].replace("\"", ""))
                printformatted(element[0], element[1][0])
                
            print("Vault Spaces Saveable: " + str(len(worstArmor)),end="\n\n")
            
            armorSet = set()
            printstr = "DIM string for items to delete:      \n"
            for element in simpleSuperiorityList:
                if element[1][0] not in armorSet:
                    armorSet.add(element[1][0])
                    printstr += "id:" + element[1][0].split(',')[7].replace("\"", "") + " or "
            print(printstr[0:len(printstr)-4])
            
        sys.stdout = original_stdout  # Reset the standard output to its original value


def printformatted(better, worse):
    print("%s : %s (%s %s %s at %s power) > \n%s : %s (%s %s %s at %s power)\n" %
          (better.split(',')[7], better.split(',')[0], better.split(',')[5], better.split(',')[1], better.split(',')[2], better.split(',')[3], worse.split(',')[7], worse.split(',')[0], worse.split(',')[5], worse.split(',')[1], worse.split(',')[2], worse.split(',')[3]))


if __name__ == "__main__":
    run()
