
The root docs page (`/`) is not rate-limited.

---

## Response Format

Each job object looks like this:

```json
{
  "id": 15666,
  "simplifyId": "711e8836-13f2-4b00-873e-10d28b8c9753",
  "company": "Google",
  "title": "Software Engineer Early Career - Infrastructure",
  "locations": ["Seattle, WA", "NYC", "Mountain View, CA"],
  "applicationUrl": "https://www.google.com/about/careers/...",
  "datePosted": "2025-10-31",
  "sponsorship": "Other",
  "active": true,
  "createdAt": "2026-05-02",
  "updatedAt": "2026-05-02"
}
```

### Field reference

| Field | Type | Description |
|-------|------|-------------|
| `id` | number | Database ID |
| `simplifyId` | string | Simplify's unique ID for this listing |
| `company` | string | Company name |
| `title` | string | Job title |
| `locations` | string[] | List of locations (can be multiple) |
| `applicationUrl` | string | Direct link to apply |
| `datePosted` | string | Date posted (YYYY-MM-DD) — listings older than 29 days are excluded from results |
| `sponsorship` | string | Sponsorship status (e.g. "Sponsors", "Other") |
| `active` | boolean | Whether the listing is still open |
| `createdAt` | string | When the record was added to this API |
| `updatedAt` | string | When the record was last updated |

---

## Usage Examples

### JavaScript / Fetch

```javascript
fetch('https://internship-api-production-a9b7.up.railway.app/api/jobs')
  .then(res => res.json())
  .then(jobs => {
    jobs.forEach(job => {
      console.log(`${job.company} — ${job.title}`);
      console.log(`Apply: ${job.applicationUrl}`);
    });
  });
```

### Swift (iOS)

```swift
struct Job: Codable {
    let id: Int
    let company: String
    let title: String
    let locations: [String]
    let applicationUrl: String
    let datePosted: String
    let sponsorship: String
    let active: Bool
}

func fetchJobs() {
    let url = URL(string: "https://internship-api-production-a9b7.up.railway.app/api/jobs")!
    URLSession.shared.dataTask(with: url) { data, _, _ in
        guard let data = data else { return }
        let jobs = try? JSONDecoder().decode([Job].self, from: data)
        // Use jobs
    }.resume()
}
```

### Python

```python
import requests

response = requests.get('https://internship-api-production-a9b7.up.railway.app/api/jobs')
jobs = response.json()

for job in jobs:
    print(f"{job['company']} — {job['title']}")
    print(f"Locations: {', '.join(job['locations'])}")
    print(f"Apply: {job['applicationUrl']}\n")
```

---

## Notes

- **No authentication required** — the API is fully public and free to use
- **Rate limited** — 60 requests/min per IP on `/api/**` (see [Rate Limiting](#rate-limiting))
- **CORS enabled** — can be called directly from any web app or browser
- **Auto-syncs every 30 minutes** from SimplifyJobs
- **29-day filter** — only listings posted within the last 29 days are returned across all endpoints
- **Active listings only** by default — use `/api/jobs/all` to include closed roles
- Data covers Software Engineering internships only (category = "Software" / "Software Engineering") in the US, Canada, and remote

---

## Running Locally

```bash
git clone https://github.com/bobbramillan/internship-api.git
cd internship-api
./mvnw spring-boot:run
# API at http://localhost:8080
# H2 console at http://localhost:8080/h2-console
```

## Deploying to Railway

1. Fork this repo and push to GitHub
2. Create a new Railway project → Deploy from GitHub repo
3. Add a PostgreSQL plugin to the project
4. On your app service, set these environment variables (using Railway's `${{Postgres.VAR}}` reference syntax):

| Variable | Value |
|----------|-------|
| `DATABASE_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DB_PLATFORM` | `org.hibernate.dialect.PostgreSQLDialect` |
| `DB_USERNAME` | `${{Postgres.PGUSER}}` |
| `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
| `H2_CONSOLE` | `false` |

> **Note:** Railway's `DATABASE_URL` variable on the Postgres service itself is a plain connection URI (no `jdbc:` prefix), which the JDBC driver will reject. Construct your app's `DATABASE_URL` manually from the individual `PGHOST`/`PGPORT`/`PGDATABASE` fields as shown above.

5. On the app service → Settings → Networking → Generate Domain, using port `8080`

---

## Data Source

All job data is sourced from [SimplifyJobs/Summer2027-Internships](https://github.com/SimplifyJobs/Summer2027-Internships), maintained by Pitt CSC and [Simplify](https://simplify.jobs). This API is an unofficial wrapper, filtered to Software Engineering roles only — if you find it useful, star their repo too.