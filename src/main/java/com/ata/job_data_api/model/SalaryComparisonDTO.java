package com.ata.job_data_api.model;

//Use this for testing cleanup salary logic
public record SalaryComparisonDTO(
        String rawSalary,
        Long cleanedSalaryTHB,
        String jobTitle,
        String location
) {}
