CREATE TABLE starred_repos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    repo_full_name VARCHAR(255) NOT NULL,
    repo_name VARCHAR(255) NOT NULL,
    language VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(user_id, repo_full_name)
);

CREATE INDEX idx_starred_repos_user_id ON starred_repos(user_id);
