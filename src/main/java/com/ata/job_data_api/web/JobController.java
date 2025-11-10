package com.ata.job_data_api.web;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class JobController {

    private final JobService jobService;

    @GetMapping("/all-raw")
    public Flux<RawJobData> getAllRawJobs() {
        return jobService.getRawJobData();
    }
}