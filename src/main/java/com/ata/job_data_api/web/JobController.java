package com.ata.job_data_api.web;

import com.ata.job_data_api.model.RawJobData;
import com.ata.job_data_api.model.SalaryComparisonDTO;
import com.ata.job_data_api.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/job_data")
@Slf4j
public class JobController {

    private final JobService jobService;

    //for testing
    @GetMapping("/all-raw")
    public Flux<RawJobData> getAllRawJobs() {
        return jobService.getRawJobData();
    }

    @GetMapping
    public Flux<Map<String, Object>> getFilteredJobs(@RequestParam MultiValueMap<String, String> params) {
        return jobService.findJobsWithSparseFields(params);
    }

    @GetMapping("/salary_compare")
    public Flux<SalaryComparisonDTO> getSalaryComparison() {
        return jobService.getSalaryComparisonData();
    }
}