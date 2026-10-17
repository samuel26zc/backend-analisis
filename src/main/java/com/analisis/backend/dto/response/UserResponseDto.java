package com.analisis.backend.dto.response;

import com.analisis.backend.model.enums.UserStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserResponseDto() {
    }

    public UserResponseDto(Long id, String firstName, String lastName, String email, UserStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponseDtoBuilder builder() {
        return new UserResponseDtoBuilder();
    }

    public static class UserResponseDtoBuilder {
        private Long id;
        private String firstName;
        private String lastName;
        private String email;
        private UserStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        UserResponseDtoBuilder() {
        }

        public UserResponseDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserResponseDtoBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserResponseDtoBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserResponseDtoBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserResponseDtoBuilder status(UserStatus status) {
            this.status = status;
            return this;
        }

        public UserResponseDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserResponseDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserResponseDto build() {
            return new UserResponseDto(this.id, this.firstName, this.lastName, this.email, this.status, this.createdAt, this.updatedAt);
        }
    }
}
