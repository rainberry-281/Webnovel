package com.bn.berrynovel.domain.dto;

import com.bn.berrynovel.service.Validator.RegisterChecked;
import com.bn.berrynovel.service.Validator.StrongPassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@RegisterChecked
public class RegisterDTO {
    public String fullName;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    public String username;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    public String email;

    public String phoneNumber;

    @StrongPassword
    public String password;

    public String confirmPassword;

    public RegisterDTO() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
