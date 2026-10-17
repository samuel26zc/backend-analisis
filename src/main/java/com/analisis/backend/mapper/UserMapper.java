package com.analisis.backend.mapper;

import com.analisis.backend.dto.request.UserRequestDto;
import com.analisis.backend.dto.response.UserResponseDto;
import com.analisis.backend.model.entity.User;
import com.analisis.backend.model.enums.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserMapper() {
    }

    public User toEntity(UserRequestDto dto) {
        if (dto == null) {
            return null;
        }

        return User.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .status(UserStatus.ACTIVE)
                .build();
    }

    public UserResponseDto toResponseDto(User entity) {
        if (entity == null) {
            return null;
        }

        return UserResponseDto.builder()
                .id(entity.getId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public void updateEntityFromDto(UserRequestDto dto, User entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setEmail(dto.getEmail());
    }
}
