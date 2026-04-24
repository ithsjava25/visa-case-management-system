package org.example.visacasemanagementsystem.audit.controller;

import org.example.visacasemanagementsystem.audit.UserEventType;
import org.example.visacasemanagementsystem.audit.VisaEventType;
import org.example.visacasemanagementsystem.audit.dto.UserLogDTO;
import org.example.visacasemanagementsystem.audit.dto.VisaLogDTO;
import org.example.visacasemanagementsystem.audit.service.UserLogService;
import org.example.visacasemanagementsystem.audit.service.VisaLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

        Page<VisaLogDTO> logs = visaLogService.findFiltered(
                eventType,
                startOfDayOrNull(from),
                endOfDayOrNull(to),
                buildPageable(page, size));

        model.addAttribute("logs", logs);
        model.addAttribute("eventTypes", VisaEventType.values());
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("pageTitle", "Visa Log");
        return "log/visa";
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

        Page<UserLogDTO> logs = userLogService.findFiltered(
                eventType,
                startOfDayOrNull(from),
                endOfDayOrNull(to),
                buildPageable(page, size));

        model.addAttribute("logs", logs);
        model.addAttribute("eventTypes", UserEventType.values());
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("pageTitle", "User Log");
        return "log/user";
    }

    // ─── Helpers ──────────────────────────────────────────────────────────

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
