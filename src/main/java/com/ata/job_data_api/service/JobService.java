package com.ata.job_data_api.service;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.model.SalaryComparisonDTO;
import com.ata.job_data_api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class JobService {
    private final JobRepository repository;
    private final SalaryCleanupService salaryCleanupService;

    public Flux<RawJobData> getRawJobData() {
        return repository.findAllRaw();
    }

    public Flux<RawJobData> findJobs(MultiValueMap<String, String> queryParams) {
        var rawJobs = repository.findAllRaw();
        var combinedPredicate = buildFilterPredicate(queryParams);
        rawJobs = rawJobs.filter(combinedPredicate);
        return rawJobs;
    }

    private Predicate<RawJobData> buildFilterPredicate(MultiValueMap<String, String> params) {
        var jobTitlePredicate = createJobTitlePredicate(params);
        var genderPredicate = createGenderPredicate(params);
        var salaryPredicate = createSalaryRangePredicate(params);
        return jobTitlePredicate
                .and(genderPredicate)
                .and(salaryPredicate);
    }

    private Predicate<RawJobData> createJobTitlePredicate(MultiValueMap<String, String> params) {
        var jobTitle = params.getFirst("job_title");
        if (jobTitle != null) {
            return job -> job.getJobTitle() != null && job.getJobTitle().equalsIgnoreCase(jobTitle.trim());
        }
        return job -> true;
    }

    private Predicate<RawJobData> createGenderPredicate(MultiValueMap<String, String> params) {
        var gender = params.getFirst("gender");
        if (gender != null) {
            var genderFilter = gender.trim().toLowerCase();
            if (genderFilter.equals("others")) {
                // Condition: If user specifies gender=others, return all non-Male/non-Female.
                return job -> {
                    String rawGender = job.getGender().trim();
                    if (StringUtils.isBlank(rawGender)) {
                        return false; // Exclude null/empty values from 'others' group
                    }
                    var normalizedGender = rawGender.trim().toLowerCase();
                    // Return TRUE if the gender is NOT "male" AND NOT "female"
                    return !(normalizedGender.equalsIgnoreCase("male") || normalizedGender.equalsIgnoreCase("female"));
                };
            } else {
                // Condition: Filter by the exact specified value e.g., Male, Female, and weird.
                return job -> job.getGender() != null && job.getGender().trim().toLowerCase().equals(genderFilter);
            }
        }
        return job -> true;
    }

    private Predicate<RawJobData> createSalaryRangePredicate(MultiValueMap<String, String> params) {
        Predicate<RawJobData> defaultSalaryPredicate = job -> true; // เริ่มต้นด้วย True
        String[] operators = {"gte", "gt", "lte", "lt", "eq"};
        for (String op : operators) {
            String filterKey = "salary[" + op + "]";
            String salaryValueStr = params.getFirst(filterKey);
            if (salaryValueStr != null) {
                try {
                    Predicate<RawJobData> currentPredicate = getRawJobDataPredicate(op, salaryValueStr);
                    defaultSalaryPredicate = defaultSalaryPredicate.and(currentPredicate);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return defaultSalaryPredicate;
    }

    private Predicate<RawJobData> getRawJobDataPredicate(String op, String salaryValueStr) {
        var comparisonValue = Long.parseLong(salaryValueStr);
        Predicate<RawJobData> currentPredicate = job -> {
            Long actualAnnualSalary = salaryCleanupService.cleanseRawSalary(job.getRawSalary());
            return switch (op) {
                case "gte" -> actualAnnualSalary >= comparisonValue;
                case "gt" -> actualAnnualSalary > comparisonValue;
                case "lte" -> actualAnnualSalary <= comparisonValue;
                case "lt" -> actualAnnualSalary < comparisonValue;
                case "eq" -> actualAnnualSalary == comparisonValue;
                default -> true; // ควรจะไม่เกิดขึ้น
            };
        };
        return currentPredicate;
    }

    public Flux<SalaryComparisonDTO> getSalaryComparisonData() {
        return repository.findAllRaw()
                .map(this::mapToComparisonDTO);
    }

    private SalaryComparisonDTO mapToComparisonDTO(RawJobData rawJob) {
        var cleanedTHB = salaryCleanupService.cleanseRawSalary(rawJob.getRawSalary());
        return new SalaryComparisonDTO(
                rawJob.getRawSalary(),
                cleanedTHB,
                rawJob.getJobTitle(),
                rawJob.getLocation()
        );
    }

    public Flux<Map<String, Object>> findJobsWithSparseFields(MultiValueMap<String, String> queryParams) {
        Flux<RawJobData> rawJobs = repository.findAllRaw();

        // 1. Apply Filtering (already done)
        Predicate<RawJobData> combinedPredicate = buildFilterPredicate(queryParams);
        rawJobs = rawJobs.filter(combinedPredicate);
        return applySparseFields(rawJobs, queryParams);
    }

    private Flux<Map<String, Object>> applySparseFields(Flux<RawJobData> jobs, MultiValueMap<String, String> params) {
        String fieldsStr = params.getFirst("fields");

        if (StringUtils.isBlank(fieldsStr)) {
            return jobs.map(this::mapAllFieldsToMap);
        }

        //list all requesting fields
        List<String> requestedFields = Arrays.stream(fieldsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return jobs.map(job -> mapFieldsToMap(job, requestedFields));
    }

    private Map<String, Object> mapFieldsToMap(RawJobData job, List<String> fields) {
        Map<String, Object> result = new HashMap<>();

        for (String field : fields) {
            // ใช้ field.toLowerCase() ในการตรวจสอบ
            Object value = switch (field.toLowerCase()) {
                case "timestamp" -> job.getTimestamp();
                case "employer" -> job.getEmployer();
                case "location" -> job.getLocation();
                case "job_title" -> job.getJobTitle();
                case "years_at_employer" -> job.getYearsAtEmployer();
                case "years_of_experience" -> job.getYearsOfExperience();

                // Special Cases:
                case "salary" -> salaryCleanupService.cleanseRawSalary(job.getRawSalary()); // Cleaned Salary (THB)
                case "raw_salary" -> job.getRawSalary(); // Raw Salary String

                case "signing_bonus" -> job.getSigningBonus();
                case "annual_bonus" -> job.getAnnualBonus();
                case "annual_stock_value_bonus", "annual_stock_value/bonus" -> job.getAnnualStockValueBonus(); // รองรับ 2 key
                case "gender" -> job.getGender();
                case "additional_comments" -> job.getAdditionalComments();
                default -> null;
            };
            if (value != null) {
                result.put(field, value);
            }
        }
        return result;
    }

    private Map<String, Object> mapAllFieldsToMap(RawJobData job) {
        Map<String, Object> result = new HashMap<>();
        result.put("Timestamp", job.getTimestamp());
        result.put("Employer", job.getEmployer());
        result.put("Location", job.getLocation());
        result.put("Job Title", job.getJobTitle());
        result.put("Years at Employer", job.getYearsAtEmployer());
        result.put("Years of Experience", job.getYearsOfExperience());
        result.put("Salary (Cleaned THB)", salaryCleanupService.cleanseRawSalary(job.getRawSalary()));
        result.put("Raw Salary", job.getRawSalary());
        result.put("Signing Bonus", job.getSigningBonus());
        result.put("Annual Bonus", job.getAnnualBonus());
        result.put("Annual Stock Value/Bonus", job.getAnnualStockValueBonus());
        result.put("Gender", job.getGender());
        result.put("Additional Comments", job.getAdditionalComments());
        return result;
    }
}
