package com.ata.job_data_api.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SalaryCleanupService {

    private static final long ANNUAL_WORKING_HOURS = 2080L; // 40 hours/week * 52 weeks
    private static final double USD_TO_THB_RATE = 32.34;
    private static final double NOK_TO_THB_RATE = 3.20;
    private static final double EUR_TO_THB_RATE = 37.43;
    private static final double GBP_TO_THB_RATE = 42.57;

    public Long cleanseRawSalary(String rawSalary) {
        log.debug("Cleaning raw salary: {}", rawSalary);
        if (StringUtils.isBlank(rawSalary)) {
            return 0L;
        }

        //Initial Cleaning and Currency Rate
        var conversionRate = getConversionRate(rawSalary);
        var cleaned = rawSalary.replaceAll("[€$£,\\s]", "").toLowerCase();
        cleaned = StringUtils.replace(cleaned, "nok", "");
        cleaned = StringUtils.deleteWhitespace(cleaned);
        log.debug("Cleaned raw salary: {}, conversion rate {}", cleaned, conversionRate);

        var annualSalaryInBaseCurrency = 0.0;

        //convert k to 1000
        if (cleaned.endsWith("k")) {
            try {
                annualSalaryInBaseCurrency = Double.parseDouble(cleaned.substring(0, cleaned.length() - 1)) * 1000;
            } catch (NumberFormatException ignored) {}
        }
        //Hourly rate
        else if (cleaned.contains("/hr") || cleaned.contains("/h")) {
            try {
                var rate = cleaned.replace("/hr", "").replace("/h", "");
                annualSalaryInBaseCurrency = Double.parseDouble(rate) * ANNUAL_WORKING_HOURS;
            } catch (NumberFormatException ignored) {}
        }
        // other case
        else {
            try {
                annualSalaryInBaseCurrency = Double.parseDouble(cleaned);
            } catch (NumberFormatException ignored) {
                log.info("cannot convert raw salary : {}", rawSalary);
            }
        }
        return Math.round(annualSalaryInBaseCurrency * conversionRate);
    }

    private double getConversionRate(String rawSalary) {
        var conversionRate = 1.0;
        if (rawSalary.contains("€")) {
            conversionRate = EUR_TO_THB_RATE;
        }
        else if (rawSalary.contains("$")) {
            conversionRate = USD_TO_THB_RATE;
        } else if (rawSalary.contains("£")) {
            conversionRate = GBP_TO_THB_RATE;
        } else if (rawSalary.contains("NOK")) {
            conversionRate = NOK_TO_THB_RATE;
        }
        return conversionRate;
    }
}
