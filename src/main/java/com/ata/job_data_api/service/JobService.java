package com.ata.job_data_api.service;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@Service
public class JobService {
    private final JobRepository repository;

    public Flux<RawJobData> getRawJobData() {
        return repository.findAllRaw();
    }
}
