package com.amdocs.telecom.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Customer {

    private Long customerId;
    private String customerNumber;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String email;
    private String mobileNumber;
    private String address;
    private String city;
    private String country;
    private String username;
    private String passwordHash;
    private LocalDateTime registrationDate;
    private String accountStatus;

    public Customer() {
    }

    public Customer(String customerNumber, String firstName, String lastName, LocalDate dateOfBirth,
                     String email, String mobileNumber, String username, String passwordHash) {
        this.customerNumber = customerNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", customerNumber='" + customerNumber + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", email='" + email + '\'' +
                ", mobileNumber='" + mobileNumber + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", country='" + country + '\'' +
                ", username='" + username + '\'' +
                ", registrationDate=" + registrationDate +
                ", accountStatus='" + accountStatus + '\'' +
                '}';
    }
}
