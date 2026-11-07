package com.businessmanager.controller;

import com.businessmanager.dto.stats.StatsDtos.SearchResult;
import com.businessmanager.security.SecurityUtils;
import com.businessmanager.service.SearchService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Recherche globale")
@SecurityRequirement(name = "bearerAuth")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public List<SearchResult> search(@RequestParam("q") String query, Authentication authentication) {
        return searchService.search(query,
                SecurityUtils.canSeeFinance(authentication),
                SecurityUtils.canSeeProjects(authentication));
    }
}
