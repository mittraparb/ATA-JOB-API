package com.ata.job_data_api.service.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MultiValueMap;

import java.util.Comparator;
import java.util.Map;

public class JobSorter {
    public static Comparator<Map<String, Object>> getSortingComparator(MultiValueMap<String, String> params) {
        var sortField = params.getFirst("sort");
        if (StringUtils.isBlank(sortField)) {
            return null;
        }

        String sortType = params.getFirst("sort_type");
        var isDescending = "DESC".equalsIgnoreCase(sortType);
        final String finalSortField = sortField;

        Comparator<Map<String, Object>> comparator = (map1, map2) -> {
            Object val1 = map1.get(finalSortField);
            Object val2 = map2.get(finalSortField);

            if (val1 == null && val2 == null) return 0;
            if (val1 == null) return -1;
            if (val2 == null) return 1;

            if (val1 instanceof Long) {
                return ((Long) val1).compareTo((Long) val2);
            } else if (val1 instanceof String) {
                return ((String) val1).compareToIgnoreCase((String) val2);
            }
            return val1.toString().compareToIgnoreCase(val2.toString());
        };
        return isDescending ? comparator.reversed() : comparator;
    }
}
