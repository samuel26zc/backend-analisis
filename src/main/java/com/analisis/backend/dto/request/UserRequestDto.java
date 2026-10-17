package com.analisis.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRequestDto {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 100, message = "El apellido debe tener entre 2 y 100 caracteres")
    private String lastName;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    private String email;

    public UserRequestDto() {
    }

    public UserRequestDto(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public static UserRequestDtoBuilder builder() {
        return new UserRequestDtoBuilder();
    }

    public static class UserRequestDtoBuilder {
        private String firstName;
        private String lastName;
        private String email;

        UserRequestDtoBuilder() {
        }

        public UserRequestDtoBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserRequestDtoBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserRequestDtoBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserRequestDto build() {
            return new UserRequestDto(this.firstName, this.lastName, this.email);
        }
    }
}
