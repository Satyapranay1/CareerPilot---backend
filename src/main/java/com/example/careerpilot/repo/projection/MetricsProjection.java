package com.example.careerpilot.repo.projection;

import java.math.BigDecimal;

public interface MetricsProjection {

    BigDecimal getAtsScore();

    Long getSolvedQuestions();

    Double getInterviewReadiness();

}