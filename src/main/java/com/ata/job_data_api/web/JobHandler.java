package com.ata.job_data_api.web;

import com.ata.job_data_api.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class JobHandler {

    private final JobService jobService;

    public Mono<ServerResponse> findJobsHandler(ServerRequest request) {
        var queryParams = request.queryParams();
        var jobDataFlux = jobService.findJobsWithCriteria(queryParams);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(jobDataFlux, Map.class)
                .switchIfEmpty(ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Collections.emptyList()))
                .onErrorResume(throwable -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("error", "Internal Server Error on API findJobs")));
    }
}