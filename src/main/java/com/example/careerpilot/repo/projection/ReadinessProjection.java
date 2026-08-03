package com.example.careerpilot.repo.projection;

import java.time.LocalDate;

public interface ReadinessProjection {

    LocalDate getDate();

    Double getScore();

}