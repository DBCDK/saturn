ALTER TABLE httpharvester ADD COLUMN name TEXT UNIQUE NOT NULL;
ALTER TABLE ftpharvester ADD COLUMN name TEXT UNIQUE NOT NULL;