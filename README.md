# New Grad Jobs API

A free, public REST API serving new grad SWE, PM, and quant job listings for 2025 & 2026 graduates. Data is sourced from [SimplifyJobs/New-Grad-Positions](https://github.com/SimplifyJobs/New-Grad-Positions) and syncs automatically every 30 minutes.

**Base URL:** `https://internship-api-production-521e.up.railway.app`

---

## Quick Start

```bash
# Get all active listings
curl https://internship-api-production-521e.up.railway.app/api/jobs

# Search by company
curl "https://internship-api-production-521e.up.railway.app/api/jobs/search?company=Google"

# Get count of active listings
curl https://internship-api-production-521e.up.railway.app/api/jobs/count
```

---

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/jobs` | All active listings |
| GET | `/api/jobs/all` | All listings (active + inactive) |
| GET | `/api/jobs/{id}` | Single listing by ID |
| GET | `/api/jobs/search?company=Google` | Filter active by company (partial, case-insensitive) |
| GET | `/api/jobs/sponsorship?type=Sponsors` | Filter by sponsorship status |
| GET | `/api/jobs/count` | Count of active listings |
| POST | `/api/jobs/refresh` | Manually trigger a sync |

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
  "isActive": true,
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
| `datePosted` | string | Date posted (YYYY-MM-DD) |
| `sponsorship` | string | Sponsorship status (e.g. "Sponsors", "Other") |
| `isActive` | boolean | Whether the listing is still open |
| `createdAt` | string | When the record was added to this API |
| `updatedAt` | string | When the record was last updated |

---

## Usage Examples

### JavaScript / Fetch

```javascript
fetch('https://internship-api-production-521e.up.railway.app/api/jobs')
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
    let isActive: Bool
}

func fetchJobs() {
    let url = URL(string: "https://internship-api-production-521e.up.railway.app/api/jobs")!
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

response = requests.get('https://internship-api-production-521e.up.railway.app/api/jobs')
jobs = response.json()

for job in jobs:
    print(f"{job['company']} — {job['title']}")
    print(f"Locations: {', '.join(job['locations'])}")
    print(f"Apply: {job['applicationUrl']}\n")
```

---

## Notes

- **No authentication required** — the API is fully public and free to use
- **CORS enabled** — can be called directly from any web app or browser
- **Auto-syncs every 30 minutes** from SimplifyJobs
- **Active listings only** by default — use `/api/jobs/all` to include closed roles
- Data covers SWE, PM, and quant roles in the US, Canada, and remote

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
3. Add a PostgreSQL plugin
4. Set these environment variables:

| Variable | Value |
|----------|-------|
| `DATABASE_URL` | `jdbc:postgresql://<host>:5432/<db>` |
| `DB_DRIVER` | `org.postgresql.Driver` |
| `DB_USERNAME` | from Railway PostgreSQL plugin |
| `DB_PASSWORD` | from Railway PostgreSQL plugin |
| `H2_CONSOLE` | `false` |

---

## Data Source

All job data is sourced from [SimplifyJobs/New-Grad-Positions](https://github.com/SimplifyJobs/New-Grad-Positions), maintained by [Coder Quad](https://github.com/coderquad) and [Simplify](https://simplify.jobs). This API is an unofficial wrapper — if you find it useful, star their repo too.
