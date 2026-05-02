package com.bav.internshipapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class SimplifyService {

    private static final Logger logger = LoggerFactory.getLogger(SimplifyService.class);

    // listings.json is on the dev branch and updated every 30 minutes by Simplify's bot
    private static final String LISTINGS_URL =
            "https://raw.githubusercontent.com/SimplifyJobs/New-Grad-Positions/dev/.github/scripts/listings.json";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private String etag = null;

    public SimplifyService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<Job> fetchJobs() {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (etag != null) {
                headers.set("If-None-Match", etag);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    LISTINGS_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
                logger.info("listings.json not modified (304), skipping parse");
                return new ArrayList<>();
            }

            if (response.getHeaders().getETag() != null) {
                etag = response.getHeaders().getETag();
            }

            return parseListings(response.getBody());

        } catch (Exception e) {
            logger.error("Error fetching listings.json: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<Job> parseListings(String json) {
        List<Job> jobs = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                logger.error("Expected JSON array at root of listings.json");
                return jobs;
            }

            for (JsonNode node : root) {
                try {
                    Job job = parseJob(node);
                    if (job != null) {
                        jobs.add(job);
                    }
                } catch (Exception e) {
                    logger.debug("Skipping malformed listing: {}", e.getMessage());
                }
            }

            logger.info("Parsed {} jobs from listings.json", jobs.size());
        } catch (Exception e) {
            logger.error("Failed to parse listings.json: {}", e.getMessage(), e);
        }
        return jobs;
    }

    private Job parseJob(JsonNode node) {
        String simplifyId = node.path("id").asText(null);
        if (simplifyId == null || simplifyId.isBlank()) return null;

        String company = node.path("company_name").asText("Unknown");
        String title = node.path("title").asText("Unknown");
        boolean isActive = node.path("active").asBoolean(true);
        String sponsorship = node.path("sponsorship").asText(null);

        // application url — try url first, then urls array
        String applicationUrl = node.path("url").asText(null);
        if (applicationUrl == null || applicationUrl.isBlank()) {
            JsonNode urlsNode = node.path("urls");
            if (urlsNode.isArray() && urlsNode.size() > 0) {
                applicationUrl = urlsNode.get(0).asText(null);
            }
        }

        // date_posted is a Unix timestamp (seconds)
        LocalDate datePosted = null;
        JsonNode dateNode = node.path("date_posted");
        if (!dateNode.isMissingNode() && !dateNode.isNull()) {
            try {
                long epochSeconds = dateNode.asLong();
                datePosted = Instant.ofEpochSecond(epochSeconds)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate();
            } catch (Exception e) {
                logger.debug("Could not parse date_posted for {}: {}", simplifyId, e.getMessage());
            }
        }

        // locations array of strings
        List<String> locations = new ArrayList<>();
        JsonNode locsNode = node.path("locations");
        if (locsNode.isArray()) {
            for (JsonNode loc : locsNode) {
                String locStr = loc.asText("").trim();
                if (!locStr.isBlank()) locations.add(locStr);
            }
        }

        Job job = new Job();
        job.setSimplifyId(simplifyId);
        job.setCompany(company);
        job.setTitle(title);
        job.setLocations(locations);
        job.setApplicationUrl(applicationUrl);
        job.setDatePosted(datePosted);
        job.setActive(isActive);
        job.setSponsorship(sponsorship);
        return job;
    }
}
