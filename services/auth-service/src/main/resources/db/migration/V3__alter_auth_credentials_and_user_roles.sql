-- Step 1: Drop existing foreign key constraint
ALTER TABLE user_roles 
DROP CONSTRAINT IF EXISTS fk_user_roles_user;

-- Step 2: Drop the user_id column from auth_credentials
ALTER TABLE auth_credentials 
DROP COLUMN IF EXISTS user_id;

-- Step 3: Drop the index on user_id (if exists)
DROP INDEX IF EXISTS idx_auth_user_id;

-- Step 4: Add new foreign key constraint referencing id
ALTER TABLE user_roles 
ADD CONSTRAINT fk_user_roles_user 
    FOREIGN KEY (user_id) REFERENCES auth_credentials(id)
    ON DELETE CASCADE;

-- Step 5: Rename user_id column in user_roles to auth_id for clarity (optional)
-- This makes it clear we're referencing auth_credentials.id
ALTER TABLE user_roles RENAME COLUMN user_id TO auth_id;
ALTER TABLE refresh_tokens RENAME COLUMN user_id TO auth_id;