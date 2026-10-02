package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.TelecomPlan;

import java.util.List;

public interface TelecomPlanDAO {

    TelecomPlan findById(Long planId);

    TelecomPlan findByPlanCode(String planCode);

    boolean existsByPlanCode(String planCode);

    void save(TelecomPlan plan);

    void update(TelecomPlan plan);

    void deleteById(Long planId);

    List<TelecomPlan> findAllActive();
}
