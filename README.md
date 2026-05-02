# New Grad Jobs API

A REST API serving new grad SWE, PM, and quant job listings sourced from [SimplifyJobs/New-Grad-Positions](https://github.com/SimplifyJobs/New-Grad-Positions). Syncs automatically every 30 minutes.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/jobs` | All active listings |
| GET | `/api/jobs/all` | All listings (active + inactive) |
| GET | `/api/jobs/{id}` | Single listing by DB id |
| GET | `/api/jobs/search?company=Google` | Filter active by company (partial, case-insensitive) |
| GET | `/api/jobs/sponsorship?type=Sponsors` | Filter by sponsorship field |
| GET | `/api/jobs/count` | Count of active listings |
| POST | `/api/jobs/refresh` | Manually trigger a sync |

## Response shape

```json
{
  "id": 1,
  "simplifyId": "abc123",
  "company": "Google",
  "title": "Software Engineer, New Grad",
  "locations": ["Mountain View, CA", "New York, NY"],
  "applicationUrl": "https://careers.google.com/...",
  "datePosted": "2025-08-01",
  "isActive": true,
  "sponsorship": "Sponsors",
  "createdAt": "2025-08-02",
  "updatedAt": "2025-08-02"
}
```

## Running locally

```bash
./mvnw spring-boot:run
# API at http://localhost:8080
# H2 console at http://localhost:8080/h2-console
```

## Deploying to Railway

1. Push to GitHub
2. Create new Railway project → Deploy from GitHub repo
3. Add a PostgreSQL plugin (Railway provisions it automatically)
4. Set these environment variables in Railway:

| Variable | Value |
|----------|-------|
| `DATABASE_URL` | `jdbc:postgresql://<host>/<db>` (Railway provides this) |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DB_PLATFORM` | `org.hibernate.dialect.PostgreSQLDialect` |
| `DB_USERNAME` | from Railway PostgreSQL plugin |
| `DB_PASSWORD` | from Railway PostgreSQL plugin |
| `H2_CONSOLE` | `false` |

Railway auto-detects the Maven project and runs `./mvnw package` + `java -jar`.

## Data source

Data is from [SimplifyJobs/New-Grad-Positions](https://github.com/SimplifyJobs/New-Grad-Positions) (`dev` branch, `listings.json`). Updated by Simplify's bot every 30 minutes; this API syncs on the same cadence.
