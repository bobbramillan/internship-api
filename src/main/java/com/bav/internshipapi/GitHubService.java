package com.bav.internshipapi;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    private static final String GITHUB_API_URL = "https://api.github.com/repos/vanshb03/Summer2026-Internships/readme";

    private final RestTemplate restTemplate;
    private String etag = null;

    public GitHubService() {
        this.restTemplate = new RestTemplate();
    }

    public List<Internship> fetchInternships() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", "application/vnd.github.v3.raw");
            if (etag != null) {
                headers.set("If-None-Match", etag);
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    GITHUB_API_URL,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.NOT_MODIFIED) {
                logger.info("GitHub data not modified (304)");
                return new ArrayList<>();
            }

            if (response.getHeaders().getETag() != null) {
                etag = response.getHeaders().getETag();
            }

            return parseMarkdownTable(response.getBody());

        } catch (Exception e) {
            logger.error("Error fetching from GitHub: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<Internship> parseMarkdownTable(String markdown) {
        List<Internship> internships = new ArrayList<>();
        String[] lines = markdown.split("\n");
        boolean inTable = false;

        for (String line : lines) {
            if (line.contains("| Company | Role | Location |")) {
                inTable = true;
                continue;
            }

            if (line.contains("| ---")) {
                continue;
            }

            if (inTable && line.startsWith("|")) {
                try {
                    Internship internship = parseTableRow(line);
                    if (internship != null) {
                        internships.add(internship);
                    }
                } catch (Exception e) {
                    logger.debug("Could not parse line: {}", line.substring(0, Math.min(100, line.length())));
                }
            }
        }

        logger.info("Parsed {} internships from GitHub", internships.size());
        return internships;
    }

    private Internship parseTableRow(String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 6) return null;

        String company = cleanText(parts[1]);
        String role = cleanText(parts[2]);
        String location = cleanText(parts[3]);
        String linkCell = parts[4];
        String dateStr = cleanText(parts[5]);

        if (company.startsWith("↳")) {
            return null;
        }

        String applicationLink = extractLink(linkCell);
        LocalDate datePosted = parseDate(dateStr);

        Internship internship = new Internship();
        internship.setCompany(company);
        internship.setRole(role);
        internship.setLocation(location);
        internship.setApplicationLink(applicationLink);
        internship.setDatePosted(datePosted);

        return internship;
    }

    private String cleanText(String text) {
        if (text == null) return "";

        return text
                .replaceAll("<[^>]+>", "")
                .replaceAll("\\*\\*", "")
                .replaceAll("🛂|🇺🇸|🔒", "")
                .replaceAll("</br>", " ")
                .trim();
    }

    private String extractLink(String linkCell) {
        Pattern hrefPattern = Pattern.compile("href=\"([^\"]+)\"");
        Matcher matcher = hrefPattern.matcher(linkCell);
        if (matcher.find()) {
            return matcher.group(1);
        }

        Pattern markdownPattern = Pattern.compile("\\[.*?\\]\\((.*?)\\)");
        matcher = markdownPattern.matcher(linkCell);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return linkCell.trim();
    }

    private LocalDate parseDate(String dateString) {
        try {
            LocalDate now = LocalDate.now();
            int year = now.getYear();

            String dateWithYear = dateString + " " + year;
            LocalDate parsed = LocalDate.parse(dateWithYear, DateTimeFormatter.ofPattern("MMM dd yyyy"));

            if (parsed.isAfter(now.plusDays(30))) {
                dateWithYear = dateString + " " + (year - 1);
                parsed = LocalDate.parse(dateWithYear, DateTimeFormatter.ofPattern("MMM dd yyyy"));
            }

            return parsed;

        } catch (Exception e) {
            logger.warn("Could not parse date: {}", dateString);
            return LocalDate.now();
        }
    }
}
