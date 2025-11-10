package com.ata.job_data_api.service;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
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
        return jobTitlePredicate;
    }

    private Predicate<RawJobData> createJobTitlePredicate(MultiValueMap<String, String> params) {
        var jobTitle = params.getFirst("job_title");
        if (jobTitle != null) {
            return job -> job.getJobTitle() != null && job.getJobTitle().equalsIgnoreCase(jobTitle.trim());
        }
        return job -> true;
    }
}
