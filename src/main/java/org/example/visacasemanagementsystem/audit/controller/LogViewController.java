package org.example.visacasemanagementsystem.audit.controller;

import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.service.UserLogService;
import org.example.visacasemanagementsystem.audit.service.VisaLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * SYSADMIN-only audit-log pages.
 */
@PreAuthorize("hasRole('SYSADMIN')")
@Controller
@RequestMapping("/log")
public class LogViewController {

    // Hard cap on page size for pagination.
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final VisaLogService visaLogService;
    private final UserLogService userLogService;

    public LogViewController(VisaLogService visaLogService, UserLogService userLogService) {
        this.visaLogService = visaLogService;
        this.userLogService = userLogService;
    }

    @GetMapping("/visa")
    public String visaLog(
            @RequestParam(value = "eventType", required = false) VisaEventType eventType,
            @RequestParam(value = "from", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            Model model) {
        return renderLogPage(eventType, from, to, page, size,
                visaLogService::findFiltered,
                VisaEventType.values(), "Visa Log", "log/visa", model);
    }

    @GetMapping("/user")
    public String userLog(
            @RequestParam(value = "eventType", required = false) UserEventType eventType,
            @RequestParam(value = "from", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
            Model model) {
        return renderLogPage(eventType, from, to, page, size,
                userLogService::findFiltered,
                UserEventType.values(), "User Log", "log/user", model);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

    /** Four-arity fetch function: (eventType, from, to, pageable) → Page. */
    @FunctionalInterface
    private interface LogFetcher<E extends Enum<E>, D> {
        Page<D> fetch(E eventType, LocalDateTime from, LocalDateTime to, Pageable pageable);
    }

    /**
     * Populates the model and returns the view name for any log page.
     * Date-to-LocalDateTime conversion and pagination clamping are applied here
     * so each handler only needs to supply its service reference and enum type.
     */
    private <E extends Enum<E>, D> String renderLogPage(
            E eventType, LocalDate from, LocalDate to, int page, int size,
            LogFetcher<E, D> fetchFn,
            E[] allEventTypes, String pageTitle, String view, Model model) {

        Page<D> logs = fetchFn.fetch(
                eventType,
                startOfDayOrNull(from),
                endOfDayOrNull(to),
                buildPageable(page, size));

        model.addAttribute("logs", logs);
        model.addAttribute("eventTypes", allEventTypes);
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("pageTitle", pageTitle);
        return view;
    }

    private static LocalDateTime startOfDayOrNull(LocalDate d) {
        return d == null ? null : d.atStartOfDay();
    }

    private static LocalDateTime endOfDayOrNull(LocalDate d) {
        return d == null ? null : d.atTime(LocalTime.MAX);
    }

    private static PageRequest buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.clamp(size, 1, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "timeStamp"));
    }
}
