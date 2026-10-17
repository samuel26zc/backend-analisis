package com.analisis.backend.service;

import com.analisis.backend.dto.request.UserRequestDto;
import com.analisis.backend.dto.response.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponseDto create(UserRequestDto request);

    UserResponseDto getById(Long id);

    List<UserResponseDto> getAll();

    Page<UserResponseDto> getPaginated(Pageable pageable);

    UserResponseDto update(Long id, UserRequestDto request);

    void delete(Long id);
}
