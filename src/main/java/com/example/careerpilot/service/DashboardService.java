package com.example.careerpilot.service;

import com.example.careerpilot.dto.dashboard.DashboardResponse;
import org.springframework.transaction.annotation.Transactional;


public interface DashboardService {
    @Transactional(readOnly = true)
    DashboardResponse getDashboard(Long userId);

}