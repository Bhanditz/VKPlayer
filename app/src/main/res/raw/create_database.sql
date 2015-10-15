CREATE TABLE audios
    (_id INTEGER PRIMARY KEY,
    owner_id INTEGER,
    artist TEXT,
    title TEXT,
    duration INTEGER,
    url TEXT,
    cache_path TEXT,
    lyrics_id INTEGER,
    album_id INTEGER,
    genre INTEGER,
    access_key TEXT);
