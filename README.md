# Summer 2026 Internship API

A REST API that aggregates Summer 2026 tech internship listings from the [vanshb03/Summer2026-Internships](https://github.com/vanshb03/Summer2026-Internships) GitHub repository. The API automatically polls for new listings hourly and provides clean JSON endpoints for integration into web apps, mobile apps, and other services.

## What It Does

The API provides access to tech internship listings posted within the last 30 days, filtering out older postings to keep data relevant and fresh.

**Key Features:**
- Automatic hourly polling of GitHub repository
- Filters to only internships posted in last 30 days
- RESTful JSON endpoints for easy integration
- Deduplication to prevent duplicate entries
- Scheduled cleanup of old data

## Requirements

- Java 17 or higher
- Maven 3.6+

## Installation

### Clone the Repository

```bash
git clone https://github.com/bobbramillan/internship-api.git
cd internship-api
```

### Build and Run

```bash
./mvnw spring-boot:run
```

The API will start on `http://localhost:8080`

## Usage

### Quick Start

Once the application is running, you can make HTTP requests to the API endpoints:

```bash
# Get all internships from the last 30 days
curl http://localhost:8080/api/internships

# Get total count
curl http://localhost:8080/api/internships/count

# Search by company
curl http://localhost:8080/api/internships/search?company=Google
```

### API Endpoints

#### Get All Internships

Returns all internships posted within the last 30 days.

```
GET /api/internships
```

**Response:**
```json
[
  {
    "id": 1,
    "company": "Microsoft AI",
    "role": "Software Engineer: AI/ML Intern",
    "location": "Mountain View CA, Redmond WA",
    "applicationLink": "https://apply.careers.microsoft.com/...",
    "datePosted": "2024-12-20",
    "createdAt": "2024-12-29"
  }
]
```

#### Get Internship Count

Returns the total number of internships in the database.

```
GET /api/internships/count
```

**Response:**
```
20
```

#### Get Specific Internship by ID

Returns a single internship by its database ID.

```
GET /api/internships/{id}
```

**Response:**
```json
{
  "id": 1,
  "company": "Microsoft AI",
  "role": "Software Engineer: AI/ML Intern",
  "location": "Mountain View CA, Redmond WA",
  "applicationLink": "https://apply.careers.microsoft.com/...",
  "datePosted": "2024-12-20",
  "createdAt": "2024-12-29"
}
```

#### Search by Company Name

Search for internships by company name (case-insensitive, partial match).

```
GET /api/internships/search?company={companyName}
```

**Example:**
```bash
curl http://localhost:8080/api/internships/search?company=Microsoft
```

#### Get Recent Internships After Date

Get internships posted after a specific date.

```
GET /api/internships/recent?since={date}
```

**Example:**
```bash
curl http://localhost:8080/api/internships/recent?since=2024-12-01
```

#### Manually Refresh from GitHub

Trigger a manual refresh of internship data from GitHub.

```
POST /api/internships/refresh
```

**Response:**
```
Refreshed! Added 5 new internships (skipped 1100 older than 30 days)
```

## How It Works

### Architecture

The application is built using Spring Boot and consists of five main components:

1. **Internship Entity** (`Internship.java`)
    - JPA entity representing an internship record
    - Fields: company, role, location, application link, date posted

2. **Internship Repository** (`InternshipRepository.java`)
    - Spring Data JPA repository for database operations
    - Custom queries for filtering by date and checking duplicates

3. **GitHub Service** (`GitHubService.java`)
    - Fetches README from GitHub API
    - Parses markdown table to extract internship data
    - Uses ETags for conditional requests (saves bandwidth and respects rate limits)

4. **Internship Scheduler** (`InternshipScheduler.java`)
    - Polls GitHub every hour for updates
    - Filters out internships older than 30 days
    - Runs daily cleanup job at 2 AM to remove stale data

5. **Internship Controller** (`InternshipController.java`)
    - REST API endpoints
    - All endpoints filtered to return only last 30 days

### Data Flow

```
GitHub Repo → Scheduler (every hour) → GitHub Service → Parser → Database → REST API → Client
```

1. **Scheduler** triggers every hour (5 seconds after startup for first run)
2. **GitHub Service** fetches README using GitHub API
3. **Parser** extracts internship data from markdown table
4. **Deduplication** checks if internship already exists (by company + role + date)
5. **Filtering** only saves internships from last 30 days
6. **REST API** serves data to clients

### Database

The application uses H2 database (file-based) for data persistence. The database file (`internships.mv.db`) is created automatically in the application directory.

**Schema:**
```sql
CREATE TABLE internships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    location VARCHAR(1000),
    application_link VARCHAR(1000),
    date_posted DATE,
    created_at DATE
);
```

### Automatic Jobs

**Hourly Poll:**
- Runs every 3,600,000 milliseconds (1 hour)
- Initial delay of 5 seconds after startup
- Only saves internships from last 30 days

**Daily Cleanup:**
- Runs at 2:00 AM every day
- Removes internships older than 90 days (safety margin beyond the 30-day filter)

## Configuration

Configuration is managed in `src/main/resources/application.properties`:

```properties
# H2 Database
spring.datasource.url=jdbc:h2:file:./internships
spring.datasource.driver-class-name=org.h2.Driver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Server
server.port=8080
```

### Changing the GitHub Repository

To point the API at a different repository (e.g., Summer 2027), update `GitHubService.java`:

```java
private static final String GITHUB_API_URL = "https://api.github.com/repos/vanshb03/Summer2027-Internships/readme";
```

## Use Cases

### Web Applications

```javascript
// Fetch internships and display
fetch('http://localhost:8080/api/internships')
  .then(response => response.json())
  .then(data => {
    data.forEach(internship => {
      console.log(`${internship.company} - ${internship.role}`);
    });
  });
```

### Mobile Apps (iOS Example)

```swift
struct Internship: Codable {
    let id: Int
    let company: String
    let role: String
    let location: String
    let applicationLink: String
    let datePosted: String
}

func fetchInternships() {
    guard let url = URL(string: "http://localhost:8080/api/internships") else { return }
    
    URLSession.shared.dataTask(with: url) { data, response, error in
        guard let data = data else { return }
        let internships = try? JSONDecoder().decode([Internship].self, from: data)
        // Update UI with internships
    }.resume()
}
```

### Chrome Extension

Use the API to build a browser extension that notifies users of new internship postings.

### Discord Bot

Integrate with Discord to send notifications when new internships matching certain criteria are posted.

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/bav/internshipapi/
│   │   ├── InternshipApiApplication.java    # Main application
│   │   ├── Internship.java                  # Entity model
│   │   ├── InternshipRepository.java        # Database interface
│   │   ├── GitHubService.java               # GitHub integration
│   │   ├── InternshipScheduler.java         # Background jobs
│   │   └── InternshipController.java        # REST endpoints
│   └── resources/
│       └── application.properties            # Configuration
└── test/
    └── java/com/bav/internshipapi/
```

### Running Tests

```bash
./mvnw test
```

### Building JAR

```bash
./mvnw clean package
java -jar target/internship-api-0.0.1-SNAPSHOT.jar
```

## Deployment

The application can be deployed to:
- **Render.com** (free tier available)
- **Railway.app** (free tier available)
- **AWS Elastic Beanstalk**
- **Google Cloud Run**
- **Heroku**

## Limitations

- **30-Day Window:** Only stores and returns internships from the last 30 days
- **GitHub Rate Limits:** Subject to GitHub API rate limits (60 requests/hour unauthenticated)
- **Local Database:** Uses H2 file-based database (suitable for small-scale deployments)
- **No Authentication:** API endpoints are publicly accessible

## Future Enhancements

- [ ] Add pagination for large result sets
- [ ] Implement filtering by location, role type, sponsorship requirements
- [ ] Add full-text search capabilities
- [ ] Rate limiting for public API
- [ ] User authentication and API keys
- [ ] Email/Discord notifications for new postings
- [ ] PostgreSQL support for production deployments

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - feel free to use this project for your own purposes.

## Acknowledgments

- Data sourced from [vanshb03/Summer2026-Internships](https://github.com/vanshb03/Summer2026-Internships)
- Built with Spring Boot, Spring Data JPA, and H2 Database