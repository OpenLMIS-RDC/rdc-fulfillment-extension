-- WHEN COMMITTING OR REVIEWING THIS FILE: Make sure that the timestamp in the file name (that serves as a version) is the latest timestamp, and that no new migration have been added in the meanwhile.
-- Adding migrations out of order may cause this migration to never execute or behave in an unexpected way.
-- Migrations should NOT BE EDITED. Add a new migration to apply changes.

-- Table to track order sequence per supplying facility & program, reset every month
CREATE TABLE fulfillment.order_number_sequence (
    supplyingfacilityid UUID NOT NULL,
    programid UUID NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    lastsequencevalue INTEGER NOT NULL,
    CONSTRAINT order_number_sequence_pkey
            PRIMARY KEY (supplyingfacilityid, programid, year, month)
);

-- Turn off core order no. wrapping
UPDATE fulfillment.order_number_configurations
SET includeordernumberprefix = false, includeprogramcode = false, includetypesuffix = false;
