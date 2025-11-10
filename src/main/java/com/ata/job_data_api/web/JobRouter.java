package com.ata.job_data_api.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class JobRouter {

    @Bean
    public RouterFunction<ServerResponse> jobRoute(JobHandler handler) {
        return route(GET("/api/job_data"), handler::findJobsHandler);
    }
}