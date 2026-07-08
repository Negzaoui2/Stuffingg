package com.negzaoui.stuffing.service;

import com.negzaoui.stuffing.dto.admin.*;
import com.negzaoui.stuffing.entity.*;
import com.negzaoui.stuffing.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final EmployeeProfileRepository employeeProfileRepository;

    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Transactional(readOnly = true)
    public LeaveStatsResponse getLeaveStats() {
        List<LeaveRequest> allLeaves = leaveRequestRepository.findAll();

        long totalRequests = allLeaves.size();
        long pending = allLeaves.stream().filter(l -> l.getStatus() == LeaveStatus.PENDING).count();
        long approved = allLeaves.stream().filter(l -> l.getStatus() == LeaveStatus.APPROVED).count();
        long rejected = allLeaves.stream().filter(l -> l.getStatus() == LeaveStatus.REJECTED).count();

        List<LeaveTypeStatDto> byType = Arrays.stream(LeaveType.values()).map(type -> {
            List<LeaveRequest> ofType = allLeaves.stream().filter(l -> l.getType() == type).toList();
            long count = ofType.size();
            long totalDays = ofType.stream().mapToLong(this::calcDays).sum();
            return LeaveTypeStatDto.builder()
                    .type(type.name())
                    .label(labelForType(type))
                    .count(count)
                    .totalDays(totalDays)
                    .build();
        }).toList();

        LocalDate now = LocalDate.now();
        LocalDate yearAgo = now.minusMonths(12).withDayOfMonth(1);
        List<MonthlyDistributionDto> monthly = allLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> !l.getStartDate().isBefore(yearAgo))
                .collect(Collectors.groupingBy(l -> l.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM"))))
                .entrySet().stream()
                .map(e -> MonthlyDistributionDto.builder()
                        .month(e.getKey())
                        .requestCount(e.getValue().size())
                        .totalDays(e.getValue().stream().mapToLong(this::calcDays).sum())
                        .build())
                .sorted(Comparator.comparing(MonthlyDistributionDto::getMonth))
                .toList();

        double absenteeismRate = calculateGlobalAbsenteeismRate(allLeaves);

        return LeaveStatsResponse.builder()
                .totalRequests(totalRequests)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .byType(byType)
                .absenteeismRate(absenteeismRate)
                .monthlyDistribution(monthly)
                .build();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAllLeaves(String status, String type, String search, int page, int size) {
        Specification<LeaveRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), LeaveStatus.valueOf(status)));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), LeaveType.valueOf(type)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("user").get("firstName")), pattern),
                        cb.like(cb.lower(root.get("user").get("lastName")), pattern),
                        cb.like(cb.lower(root.get("user").get("email")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LeaveRequest> pageResult = leaveRequestRepository.findAll(spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<LeaveDetailAdminDto> items = pageResult.getContent().stream().map(l -> {
            String dept = employeeProfileRepository.findByUserId(l.getUser().getId())
                    .map(EmployeeProfile::getDepartment).orElse(null);
            String reviewedByName = l.getReviewedBy() != null
                    ? l.getReviewedBy().getFirstName() + " " + l.getReviewedBy().getLastName() : null;

            return LeaveDetailAdminDto.builder()
                    .id(l.getId())
                    .collaboratorId(l.getUser().getId())
                    .collaboratorName(l.getUser().getFirstName() + " " + l.getUser().getLastName())
                    .collaboratorEmail(l.getUser().getEmail())
                    .department(dept)
                    .type(l.getType().name())
                    .startDate(l.getStartDate().format(D_FMT))
                    .endDate(l.getEndDate().format(D_FMT))
                    .days((int) calcDays(l))
                    .reason(l.getReason())
                    .status(l.getStatus().name())
                    .createdAt(l.getCreatedAt() != null ? l.getCreatedAt().toString() : null)
                    .reviewedBy(reviewedByName)
                    .reviewedAt(l.getReviewedAt() != null ? l.getReviewedAt().toString() : null)
                    .build();
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("total", pageResult.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Transactional(readOnly = true)
    public AbsenteeismResponse getAbsenteeismReport() {
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfMonth(1).withMonth(1);
        String period = startOfYear.format(D_FMT) + "/" + now.format(D_FMT);

        List<LeaveRequest> approvedThisYear = leaveRequestRepository.findAll().stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> !l.getStartDate().isBefore(startOfYear))
                .toList();

        long totalCollaborators = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.COLLABORATEUR && u.isActive()).count();

        long workingDays = calculateWorkingDays(startOfYear, now);
        long totalDaysAbsent = approvedThisYear.stream().mapToLong(this::calcDays).sum();

        double globalRate = (totalCollaborators > 0 && workingDays > 0)
                ? Math.round(((double) totalDaysAbsent / (totalCollaborators * workingDays)) * 100 * 10) / 10.0
                : 0.0;

        List<DepartmentAbsenteeismDto> byDept = employeeProfileRepository.findAll().stream()
                .filter(ep -> ep.getDepartment() != null && !ep.getDepartment().isBlank())
                .collect(Collectors.groupingBy(EmployeeProfile::getDepartment))
                .entrySet().stream()
                .map(entry -> {
                    String dept = entry.getKey();
                    List<Long> userIds = entry.getValue().stream()
                            .map(ep -> ep.getUser().getId()).toList();
                    long deptAbsent = approvedThisYear.stream()
                            .filter(l -> userIds.contains(l.getUser().getId()))
                            .mapToLong(this::calcDays).sum();
                    long collabCount = userIds.size();
                    double rate = (collabCount > 0 && workingDays > 0)
                            ? Math.round(((double) deptAbsent / (collabCount * workingDays)) * 100 * 10) / 10.0
                            : 0.0;
                    return DepartmentAbsenteeismDto.builder()
                            .department(dept)
                            .rate(rate)
                            .totalDaysAbsent(deptAbsent)
                            .collaboratorCount(collabCount)
                            .build();
                }).toList();

        return AbsenteeismResponse.builder()
                .globalRate(globalRate)
                .period(period)
                .byDepartment(byDept)
                .build();
    }

    private long calcDays(LeaveRequest l) {
        return ChronoUnit.DAYS.between(l.getStartDate(), l.getEndDate()) + 1;
    }

    private String labelForType(LeaveType type) {
        return switch (type) {
            case PAID_LEAVE -> "Congé payé";
            case RTT -> "RTT";
            case SICK_LEAVE -> "Arrêt maladie";
            case UNPAID_LEAVE -> "Congé sans solde";
        };
    }

    private double calculateGlobalAbsenteeismRate(List<LeaveRequest> allLeaves) {
        LocalDate now = LocalDate.now();
        LocalDate startOfYear = now.withDayOfMonth(1).withMonth(1);
        long totalCollaborators = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.COLLABORATEUR && u.isActive()).count();
        long workingDays = calculateWorkingDays(startOfYear, now);
        long totalDaysAbsent = allLeaves.stream()
                .filter(l -> l.getStatus() == LeaveStatus.APPROVED)
                .filter(l -> !l.getStartDate().isBefore(startOfYear))
                .mapToLong(this::calcDays).sum();
        return (totalCollaborators > 0 && workingDays > 0)
                ? Math.round(((double) totalDaysAbsent / (totalCollaborators * workingDays)) * 100 * 10) / 10.0
                : 0.0;
    }

    private long calculateWorkingDays(LocalDate start, LocalDate end) {
        long days = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            if (d.getDayOfWeek().getValue() <= 5) days++;
            d = d.plusDays(1);
        }
        return days;
    }
}

