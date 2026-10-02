package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.SIMCard;

public interface SIMCardDAO {

    SIMCard findById(Long simId);

    SIMCard findBySimNumber(String simNumber);

    boolean existsBySimNumber(String simNumber);

    void save(SIMCard simCard);

    void update(SIMCard simCard);

    void deleteById(Long simId);
}
