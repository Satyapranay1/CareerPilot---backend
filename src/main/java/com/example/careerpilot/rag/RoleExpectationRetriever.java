package com.example.careerpilot.rag;

import com.example.careerpilot.dto.RoleExpectation;

import java.util.List;

public interface RoleExpectationRetriever {

    List<RoleExpectation> retrieve(
            String company,
            String role
    );
}