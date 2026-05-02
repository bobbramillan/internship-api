package com.bav.internshipapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @Autowired
    private JobRepository repository;

    @GetMapping("/")
    public String home() {
        long count = repository.countByIsActiveTrue();
        return "New Grad Jobs API — " + count + " active listings\n\n" +
                "Endpoints:\n" +
                "  GET /api/jobs\n" +
                "  GET /api/jobs/search?company=Google\n" +
                "  GET /api/jobs/sponsorship?type=Sponsors\n" +
                "  GET /api/jobs/count\n" +
                "  GET /api/jobs/{id}\n\n" +
                "Docs: https://github.com/bobbramillan/internship-api";
    }
}