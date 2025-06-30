package com.jgy36.PoliticalApp.controller;

import com.jgy36.PoliticalApp.dto.SearchResultDTO;
import com.jgy36.PoliticalApp.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "http://localhost:3000")
public class SearchController {

    private static final Logger logger = Logger.getLogger(SearchController.class.getName());

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<List<SearchResultDTO>> searchAll(
            @RequestParam String query,
            @RequestParam(required = false) String type) {

        logger.info("Search request received: query=" + query + ", type=" + type);

        try {
            // Handle empty queries gracefully
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<SearchResultDTO> results;

            if (type != null && !type.isEmpty()) {
                // Search for specific type
                results = searchService.searchByType(query, type);
            } else {
                // Search across all types
                results = searchService.searchAll(query);
            }

            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.severe("Error in search: " + e.getMessage());
            // Return empty results instead of error
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}
