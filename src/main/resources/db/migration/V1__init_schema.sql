CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE businesses (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            category VARCHAR(100) NOT NULL,
                            address TEXT NOT NULL,
                            latitude DOUBLE PRECISION NOT NULL,
                            longitude DOUBLE PRECISION NOT NULL,
                            location GEOGRAPHY(Point, 4326) NOT NULL,
                            num_ratings INT NOT NULL DEFAULT 0,
                            sum_ratings BIGINT NOT NULL DEFAULT 0,
                            avg_rating NUMERIC(3, 2) GENERATED ALWAYS AS (
                                CASE
                                    WHEN num_ratings > 0 THEN ROUND(sum_ratings::numeric / num_ratings, 2)
                                    ELSE 0.00
                                    END
                                ) STORED,
                            created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                            updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);