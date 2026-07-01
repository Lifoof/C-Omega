package com.health.report.common.constant;

public final class AppConstants {
    private AppConstants() {}

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "create_time";

    public static final String MEMBER_NO_PREFIX = "M";
    public static final String COLLECTION_NO_PREFIX = "C";
    public static final String REPORT_NO_PREFIX = "R";

    public static final double COMPLETENESS_WARNING_THRESHOLD = 0.7;
}
