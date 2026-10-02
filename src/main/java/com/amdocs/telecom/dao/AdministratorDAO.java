package com.amdocs.telecom.dao;

import com.amdocs.telecom.model.Administrator;

public interface AdministratorDAO {

    Administrator findById(Long adminId);

    Administrator findByUsername(String username);

    Administrator findByEmail(String email);

    void save(Administrator administrator);

    void update(Administrator administrator);

    void deleteById(Long adminId);
}
