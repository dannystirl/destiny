DROP TABLE IF EXISTS  source;
CREATE TABLE IF NOT EXISTS source (
    sourceID      int,
    title       varchar(16),
    description varchar(1024),

    PRIMARY KEY (sourceID)
);

CREATE TABLE IF NOT EXISTS items (
    sourceID      int,
    itemID      bigint,

    PRIMARY KEY (itemID),
    CONSTRAINT itemsFK1
        FOREIGN KEY(sourceID) REFERENCES source(sourceID)
        ON UPDATE CASCADE ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS itemPerkList (
    itemID      bigint,
    itemNumber  int,        -- which unique count of itemID is this perkList
    perk1       bigint,
    perk2       bigint,
    perk3       bigint,
    perk4       bigint,

    PRIMARY KEY (itemID, itemNumber),
    CONSTRAINT itemPerkListC1 UNIQUE (itemID, itemNumber, perk1, perk2, perk3, perk4),
    CONSTRAINT itemPerkListFK1
        FOREIGN KEY(itemID) REFERENCES items(itemID)
        ON UPDATE CASCADE ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS itemTagSet (
    itemID      bigint,
    itemNumber  int,        -- which unique count of itemID is this perkList
    tag         varchar(8),


    PRIMARY KEY (itemID, itemNumber, tag),
    CONSTRAINT itemTagSetFK1
        FOREIGN KEY(itemID, itemNumber) REFERENCES itemPerkList(itemID, itemNumber)
        ON UPDATE CASCADE ON DELETE NO ACTION
);

CREATE TABLE IF NOT EXISTS itemNoteSet (
    itemID      bigint,
    itemNumber  int,        -- which unique count of itemID is this perkList
    note        varchar(1042),


    PRIMARY KEY (itemID, itemNumber, note),
    CONSTRAINT itemNoteSetFK1
        FOREIGN KEY(itemID, itemNumber) REFERENCES itemPerkList(itemID, itemNumber)
        ON UPDATE CASCADE ON DELETE NO ACTION
);