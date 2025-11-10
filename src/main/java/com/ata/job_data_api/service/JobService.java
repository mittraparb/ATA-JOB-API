package com.ata.job_data_api.service;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.repository.JobRepository;
import com.ata.job_data_api.service.pipeline.JobFilterer;
import com.ata.job_data_api.service.pipeline.JobProjector;
import com.ata.job_data_api.service.pipeline.JobSorter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.function.Predicate;

@RequiredArgsConstructor
@Service
@Slf4j
public class JobService {
    private final JobRepository repository;
    private final JobFilterer jobFilterer;
    private final JobProjector jobProjector;

    public Flux<RawJobData> getRawJobData() {
        return repository.findAllRaw();
    }

    public Flux<Map<String, Object>> findJobsWithCriteria(MultiValueMap<String, String> queryParams) {
        final Predicate<RawJobData> combinedPredicate = jobFilterer.buildFilterPredicate(queryParams);
        final Comparator<Map<String, Object>> comparator = JobSorter.getSortingComparator(queryParams);
        return repository.findAllRaw()
                .filter(combinedPredicate)
                .transform(jobs -> jobProjector.applySparseFields(jobs, queryParams))
                .transform(flux -> comparator != null ? flux.sort(comparator) : flux);
    }
}
