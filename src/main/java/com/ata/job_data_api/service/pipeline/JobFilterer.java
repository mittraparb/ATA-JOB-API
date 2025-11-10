package com.ata.job_data_api.service.pipeline;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.service.SalaryCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.function.Predicate;

@RequiredArgsConstructor
@Component
@Slf4j
public class JobFilterer {
    private static final String JOB_TITLE_PARAM_KEY = "job_title";
    private static final String GENDER_PARAM_KEY = "gender";
    private static final String[] SUPPORT_OPERATORS = {"gte", "gt", "lte", "lt", "eq"};
    private final SalaryCleanupService salaryCleanupService;

    public Predicate<RawJobData> buildFilterPredicate(MultiValueMap<String, String> params) {
        var jobTitlePredicate = createJobTitlePredicate(params);
        var genderPredicate = createGenderPredicate(params);
        var salaryPredicate = createSalaryRangePredicate(params);
        return jobTitlePredicate
                .and(genderPredicate)
                .and(salaryPredicate);
    }

    private Predicate<RawJobData> createJobTitlePredicate(MultiValueMap<String, String> params) {
        var jobTitle = params.getFirst(JOB_TITLE_PARAM_KEY);
        if (jobTitle != null) {
            return job -> job.getJobTitle() != null && job.getJobTitle().equalsIgnoreCase(jobTitle.trim());
        }
        return job -> true;
    }

    private Predicate<RawJobData> createGenderPredicate(MultiValueMap<String, String> params) {
        var gender = params.getFirst(GENDER_PARAM_KEY);
        if (gender != null) {
            var genderFilter = gender.strip().toLowerCase();
            if (genderFilter.equals("others")) {
                return job -> {
                    String rawGender = job.getGender().strip();
                    if (StringUtils.isBlank(rawGender)) {
                        return false;
                    }
                    var normalizedGender = rawGender.toLowerCase();
                    return !(normalizedGender.equalsIgnoreCase("male") || normalizedGender.equalsIgnoreCase("female"));
                };
            } else {
                return job -> job.getGender() != null && job.getGender().strip().toLowerCase().equals(genderFilter);
            }
        }
        return job -> true;
    }

    private Predicate<RawJobData> createSalaryRangePredicate(MultiValueMap<String, String> params) {
        Predicate<RawJobData> defaultSalaryPredicate = job -> true;
        for (String op : SUPPORT_OPERATORS) {
            String filterKey = "salary[" + op + "]";
            String salaryValueStr = params.getFirst(filterKey);
            if (salaryValueStr != null) {
                try {
                    defaultSalaryPredicate = defaultSalaryPredicate.and(getRawJobDataPredicate(op, salaryValueStr));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return defaultSalaryPredicate;
    }

    private Predicate<RawJobData> getRawJobDataPredicate(String op, String salaryValueStr) {
        var comparisonValue = Long.parseLong(salaryValueStr);
        return job -> {
            Long actualAnnualSalary = salaryCleanupService.cleanseRawSalary(job.getSalary());
            return switch (op) {
                case "gte" -> actualAnnualSalary >= comparisonValue;
                case "gt" -> actualAnnualSalary > comparisonValue;
                case "lte" -> actualAnnualSalary <= comparisonValue;
                case "lt" -> actualAnnualSalary < comparisonValue;
                case "eq" -> actualAnnualSalary == comparisonValue;
                default -> true;
            };
        };
    }
}
