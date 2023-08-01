# Destiny Respository
Repository to store information used in maintaining destiny api apps. Houses custom wish lists, **DIM** info, and various spreadsheets. 

![Destiny: The Moon](https://user-images.githubusercontent.com/77759414/175799964-1eaed344-eccf-43d3-be89-4b5595432e80.png)

## Wishlist Generator
Takes an input of a list of wishlists and outputs an [organized list](https://github.com/dannystirl/destiny/blob/master/output/WishListScripted.txt) of _unique_ items. 
- Allows for generic desireable and undesirable perk combinations
- Removes duplicate items
- Organizes similar items together and then sorts those items
- Removes enhanced perks to fix **DIM** wishlist issues. 

The script can be run using `./run`, and takes item input from [this](https://raw.githubusercontent.com/dannystirl/destiny/master/input/CompleteDestinyWishLIst.txt) text file, as well as a variety of community suggestions. Once completed, your **DIM** wishlist should be set to [this link](https://raw.githubusercontent.com/dannystirl/destiny/master/output/WishListScripted.txt) in your [**DIM** settings](https://app.destinyitemmanager.com/settings). 

If you'd like to add your own items to the wishlist, formatting for **DIM** wishlists can be found [here](https://github.com/DestinyItemManager/DIM/wiki/Wish-Lists).
Alternatively, you can use the [Little Light](https://wishlists.littlelight.club/#/) wishlist generator and paste the results into the [CustomDestinyWishlist.txt](https://wishlists.littlelight.club/#/) file. 

![**DIM** Logo](https://user-images.githubusercontent.com/77759414/175800099-d71fb12d-e03f-44dd-81b5-bfc31746ceac.png)

Any information except the `hashIdentifier` is optional, but most follow the format of `dimwishlist:item=hashIdentifier&perks=long_1,long_2,long_3,long_4#notes:Example_note_text.|tags:example-tag`. 

Perks that can be applied to any item are given the `hashIdentifier` _-69420_ (That's what it is on **DIM**, I don't have the ability to change it) and perks or items that are to be excluded from the wishlist are given a `-` in front of them. 

A list of items and perks can be found on [Destiny Sets Data](https://data.destinysets.com). 

## Armor Examiner
Takes all of the user's armor from input/destinyArmor.csv (exported from **DIM**) and searches for armor that is strictly worse than other armor. Exported as a [text file](https://raw.githubusercontent.com/dannystirl/destiny/master/output/ArmorExamined.txt), which includes a list of armor that should be replaced, as well as the armor that should replace it, and a search string for **DIM** at the end of the file. 

The script can be run using `./runArmorer` or running the `armorExaminer.exe` file. 

### Backup
There is a [backup](https://github.com/dannystirl/destiny/tree/pre_maven_project) of this program that has no connection to an API and doesn't use maven. It takes less time to run, but has larger and less accurate files. 

## **DIM** Data
A backup of dim data can be found [here](https://github.com/dannystirl/destiny/blob/master/src/main/data/destiny/dim-data.json).

If you wish to run a fresh version of the Wishlist Generator, you'll need to add a personal API key to the [DATA file](https://raw.githubusercontent.com/dannystirl/destiny/master/src/main/java/DATA.java)