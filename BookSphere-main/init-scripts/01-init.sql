-- =============================================
-- BookSphere - Database Initialization
-- =============================================

-- Create extension for UUID generation (optional)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE library_db TO library_admin;

-- Note: Tables will be created automatically by Spring Boot JPA
-- This script is for any initial setup or extensions needed

-- You can add initial data here if needed
-- Example:
-- INSERT INTO ... VALUES ...;

-- Log successful initialization
DO $$
BEGIN
    RAISE NOTICE 'BookSphere database initialized successfully!';
END $$;
