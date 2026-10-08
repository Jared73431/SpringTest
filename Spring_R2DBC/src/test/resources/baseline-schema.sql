-- Baseline only: the project has no schema file, so this recreates the table the developer created by hand.
-- @Column("ID") etc. are not quoted by Spring Data R2DBC, so PostgreSQL folds them to lower case.
CREATE TABLE product (
    id          SERIAL PRIMARY KEY,
    description VARCHAR(255),
    price       DOUBLE PRECISION
);
