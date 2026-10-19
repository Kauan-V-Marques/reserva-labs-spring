-- Garantia final no PostgreSQL: duas reservas confirmadas do mesmo laboratório
-- nunca se sobrepõem, mesmo com requisições simultâneas.
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
    ADD CONSTRAINT ex_reservations_no_overlap
    EXCLUDE USING gist (laboratory_id WITH =, tsrange(starts_at, ends_at) WITH &&)
    WHERE (status = 'CONFIRMED');
