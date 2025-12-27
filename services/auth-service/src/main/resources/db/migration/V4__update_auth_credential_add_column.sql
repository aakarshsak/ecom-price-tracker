-- Add new column 'user_id' to 'auth_credentials' table
ALTER TABLE auth_credentials
ADD COLUMN user_id UUID;
