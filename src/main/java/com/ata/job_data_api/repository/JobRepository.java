package com.ata.job_data_api.repository;

import com.ata.job_data_api.model.RawJobData;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
@Repository
@Slf4j
public class JobRepository {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    private List<RawJobData> allRawJobs = Collections.emptyList();


    @PostConstruct
    public void loadStaticData() {
        Resource resource = resourceLoader.getResource("classpath:salary_survey-3.json");

        try (InputStream inputStream = resource.getInputStream()) {
            this.allRawJobs = objectMapper.readValue(
                    inputStream,
                    new TypeReference<>() {}
            );;
            log.info("Successfully loaded {} raw job records from JSON file.", allRawJobs.size());

        } catch (IOException e) {
            log.error("FATAL ERROR: Failed to load or parse job data from 'salary_survey-3.json'.");
            throw new RuntimeException("Could not initialize static job data repository.");
        }
    }

    public Flux<RawJobData> findAllRaw() {
        return Flux.fromIterable(this.allRawJobs);
    }
}
