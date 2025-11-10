package com.ata.job_data_api.service;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.util.function.Predicate;

@RequiredArgsConstructor
@Service
public class JobService {
    private final JobRepository repository;

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
        return jobTitlePredicate
                .and(genderPredicate);
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
}
