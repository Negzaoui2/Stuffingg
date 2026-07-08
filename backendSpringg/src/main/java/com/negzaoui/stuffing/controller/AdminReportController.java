package com.negzaoui.stuffing.controller;

import com.negzaoui.stuffing.dto.admin.AbsenteeismResponse;
import com.negzaoui.stuffing.dto.admin.LeaveStatsResponse;
import com.negzaoui.stuffing.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/leaves/stats")
    public ResponseEntity<LeaveStatsResponse> getLeaveStats() {
        return ResponseEntity.ok(adminReportService.getLeaveStats());
    }

    @GetMapping("/leaves")
    public ResponseEntity<Map<String, Object>> getAllLeaves(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminReportService.getAllLeaves(status, type, search, page, size));
    }

    @GetMapping("/absenteeism")
    public ResponseEntity<AbsenteeismResponse> getAbsenteeismReport() {
        return ResponseEntity.ok(adminReportService.getAbsenteeismReport());
    }
}

