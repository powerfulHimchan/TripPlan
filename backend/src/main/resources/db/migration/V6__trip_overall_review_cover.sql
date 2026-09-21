ALTER TABLE trip_reviews
    ADD COLUMN representative_photo_id UUID REFERENCES review_photos(id) ON DELETE SET NULL;

