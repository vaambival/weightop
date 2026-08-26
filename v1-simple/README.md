# weightop
Comment system with different deploy variants: from simple app to high-load app

> **Languages:** [English](README.md) | [Русский](README.ru.md)

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 25 | Programming language |
| Spring Boot | 4.1.1 | Web framework |
| Gradle | 8.14+ | Build system |
| OpenAPI Generator | 7.23.0 | Code generation from specification |
| springdoc-openapi | 3.1.0 | Swagger UI for API browsing |
| Jakarta Validation | — | Input data validation |

## Architecture

The project follows the **API-first** principle:
1. Single source of truth — `openapi.yaml` (in `src/main/resources/`).
2. The following are automatically generated from it:
    - **DTOs** (request/response models) — package `com.weightop.model`
    - **API interfaces** — package `com.weightop.api`
3. Developers manually write only controller implementations.

```
openapi.yaml → [openapi-generator] → DTO + API interfaces → [compileJava]
```

## API Methods

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/comments` | Create a comment |
| GET | `/api/v1/comments/{commentId}` | Get comment by ID |
| PUT | `/api/v1/comments/{commentId}` | Update comment text |
| DELETE | `/api/v1/comments/{commentId}` | Delete a comment |
| POST | `/api/v1/comments/{commentId}/like` | Increment like count by 1 |
| DELETE | `/api/v1/comments/{commentId}/like` | Decrement like count by 1 |
| GET | `/api/v1/posts/{postId}/comments` | Get comments for a post (paginated) |

## Database Schema (MVP)

### `comments` Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PRIMARY KEY, NOT NULL | Unique comment identifier |
| `author` | BIGINT | NOT NULL | Author identifier |
| `post_id` | BIGINT | NOT NULL | Post/article identifier |
| `text` | TEXT | NOT NULL | Comment text (up to 1000 characters) |
| `likes` | INTEGER | NOT NULL, DEFAULT 0, CHECK (likes >= 0) | Like count |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT now() | Creation time (UTC) |
| `updated_at` | TIMESTAMPTZ | NULL | Last text update time |

### Indexes

| Index | Type | Columns | Purpose |
|-------|------|---------|---------|
| `comments_pkey` | B-tree | `id` | Primary key, fast lookup by ID |
| `idx_post_created` | B-tree | `post_id ASC, created_at DESC` | Main index for fetching post comments by date (newest first) |
| `idx_post_likes` | B-tree | `post_id ASC, likes DESC` | Optional index for sorting by popularity (top comments) |

### Pagination Rules

- **Method**: `LIMIT / OFFSET` based on `idx_post_created` index.
- **Parameters**:
    - `limit` — records per page (default 20, max 100).
    - `offset` — offset (default 0, **max 1000**).
- **Default sorting**: by `created_at DESC` (newest first).
- **Popularity sorting**: by `likes DESC, created_at DESC` with `sort=popular` flag.
- **Restriction**: `offset > 1000` is forbidden (protection against deep pagination).
- **Response format**: array of `Comment` objects without wrapper.

### Intentionally Omitted (for MVP)

- Partitioning (comments are not archived, posts have unlimited lifetime).
- Replies (flat model).
- Soft delete (physical deletion).
- Materialized views.
- Full-text search.

### Growth Estimate (at 50 RPS, 12 comments/sec)

| Period | Records | Data size (with indexes) |
|--------|---------|---------------------------|
| Day | ~1,036,800 | ~265 MB |
| Week | ~7,257,600 | ~1.9 GB |
| Month | ~31,000,000 | ~7.9 GB |
| 3 months | ~93,000,000 | ~23.8 GB |

A 50 GB disk will fill in ~6 months, 250 GB — in ~2.5 years under constant maximum load.

## Running the Project

### Requirements
- JDK 25
- Gradle 8.14+ (or use wrapper after first run)

### Build
```bash
gradle build
```

### Run
```bash
gradle bootRun
```

### Swagger UI
After startup, documentation is available at:
```
http://localhost:8080/swagger-ui.html
```

## MVP Limitations

- **Likes**: increment/decrement by 1 only, no atomic race protection (sufficient for current load).
- **Pagination**: `LIMIT/OFFSET` — acceptable for MVP, but may slow down with offset > 10000.
- **Identifiers**: UUID for post and comment. Author is a string identifier.
- **Update**: only comment text can be updated, other fields are immutable.
- **Soft delete**: not implemented, physical deletion is used.

## Code Generation

Generation runs automatically before compilation:
```bash
gradle compileJava        # generates code from openapi.yaml and compiles
gradle openApiGenerate    # generation only, without compilation
```

Generated code is located in `build/generated/src/main/java` and **must not** be edited manually.