package com.remoteprint.document;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.TreeSet;

@Service
public class PageRangeService {

    public Set<Integer> parse(String pageRange, int totalPages) {

        if (pageRange == null
                || pageRange.isBlank()
                || pageRange.equalsIgnoreCase("ALL")) {

            Set<Integer> allPages = new TreeSet<>();

            for (int page = 1; page <= totalPages; page++) {
                allPages.add(page);
            }

            return allPages;
        }

        Set<Integer> pages = new TreeSet<>();

        String[] parts = pageRange.split(",");

        for (String part : parts) {

            part = part.trim();

            if (part.contains("-")) {
                parseRange(part, totalPages, pages);
            } else {
                parseSinglePage(part, totalPages, pages);
            }
        }

        return pages;
    }

    private void parseSinglePage(
            String value,
            int totalPages,
            Set<Integer> pages
    ) {

        int page = Integer.parseInt(value);

        validatePage(page, totalPages);

        pages.add(page);
    }

    private void parseRange(
            String value,
            int totalPages,
            Set<Integer> pages
    ) {

        String[] bounds = value.split("-");

        if (bounds.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid page range: " + value
            );
        }

        int start = Integer.parseInt(bounds[0].trim());
        int end = Integer.parseInt(bounds[1].trim());

        if (start > end) {
            throw new IllegalArgumentException(
                    "Range start cannot be greater than range end"
            );
        }

        validatePage(start, totalPages);
        validatePage(end, totalPages);

        for (int page = start; page <= end; page++) {
            pages.add(page);
        }
    }

    private void validatePage(int page, int totalPages) {

        if (page < 1 || page > totalPages) {
            throw new IllegalArgumentException(
                    "Page " + page
                            + " is outside document range 1-"
                            + totalPages
            );
        }
    }
}