package com.ata.job_data_api.service.pipeline;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.service.SalaryCleanupService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Component
public class JobProjector {

    private final SalaryCleanupService salaryCleanupService;
    private static final List<String> ALL_JOB_FIELDS = List.of(
            "timestamp", "employer", "location", "job_title", "years_at_employer",
            "years_of_experience", "salary", "raw_salary", "signing_bonus",
            "annual_bonus", "annual_stock_value_bonus", "gender", "additional_comments"
    );

    public Flux<Map<String, Object>> applySparseFields(Flux<RawJobData> jobs, MultiValueMap<String, String> params) {
        String fieldsStr = params.getFirst("fields");

        if (StringUtils.isBlank(fieldsStr)) {
            return jobs.map(this::mapAllFieldsToMap);
        }

        List<String> requestedFields = Arrays.stream(fieldsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return jobs.map(job -> mapFieldsToMap(job, requestedFields));
    }

    private Object getFieldValue(RawJobData job, String fieldName) {
        return switch (fieldName.toLowerCase()) {
            case "timestamp" -> job.getTimestamp();
            case "employer" -> job.getEmployer();
            case "location" -> job.getLocation();
            case "job_title" -> job.getJobTitle();
            case "years_at_employer" -> job.getYearsAtEmployer();
            case "years_of_experience" -> job.getYearsOfExperience();
            case "salary" -> salaryCleanupService.cleanseRawSalary(job.getSalary());
            case "raw_salary" -> job.getSalary();
            case "signing_bonus" -> job.getSigningBonus();
            case "annual_bonus" -> job.getAnnualBonus();
            case "annual_stock_value_bonus", "annual_stock_value/bonus" -> job.getAnnualStockValueBonus();
            case "gender" -> job.getGender();
            case "additional_comments" -> job.getAdditionalComments();
            default -> null;
        };
    }

    private Stream<Map.Entry<String, Object>> streamJobEntries(RawJobData job, List<String> fields) {
        return fields.stream()
                .map(field -> {
                    Object value = getFieldValue(job, field);
                    return value != null ? Map.entry(field, value) : null;
                })
                .filter(Objects::nonNull);
    }

    public Map<String, Object> mapFieldsToMap(RawJobData job, List<String> fields) {
        return streamJobEntries(job, fields)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> newVal,
                        HashMap::new
                ));
    }

    private Map<String, Object> mapAllFieldsToMap(RawJobData job) {
        return streamJobEntries(job, ALL_JOB_FIELDS)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}