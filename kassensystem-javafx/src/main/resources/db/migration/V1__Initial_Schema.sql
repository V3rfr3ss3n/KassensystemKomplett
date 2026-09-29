CREATE TABLE IF NOT EXISTS produkte (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    preis REAL NOT NULL,
    lagerbestand REAL NOT NULL,
    bildPfad TEXT,
    einheit TEXT NOT NULL DEFAULT 'STUECK',
    steuerSatz REAL NOT NULL DEFAULT 19.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bons (
    bonnummer INTEGER PRIMARY KEY AUTOINCREMENT,
    datumUhrzeit TIMESTAMP NOT NULL,
    gesamtpreis REAL NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bon_positionen (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bonnummer INTEGER NOT NULL,
    produkt_id INTEGER NOT NULL,
    produkt_name TEXT,
    einheit TEXT,
    menge REAL NOT NULL,
    einzelpreis REAL NOT NULL,
    steuerSatz REAL NOT NULL DEFAULT 19.0,
    gesamtpreis REAL NOT NULL,
    FOREIGN KEY(bonnummer) REFERENCES bons(bonnummer),
    FOREIGN KEY(produkt_id) REFERENCES produkte(id)
);
