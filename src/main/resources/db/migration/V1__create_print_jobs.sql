CREATE TABLE print_jobs (
    id UUID PRIMARY KEY,

    original_file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),

    original_file_path TEXT,
    printable_file_path TEXT,

    status VARCHAR(50) NOT NULL,

    page_range VARCHAR(255),
    copies INTEGER NOT NULL DEFAULT 1,

    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,

    error_message TEXT
);