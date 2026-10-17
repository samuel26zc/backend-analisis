package com.analisis.backend.service.impl;

import com.analisis.backend.dto.request.UserRequestDto;
import com.analisis.backend.dto.response.UserResponseDto;
import com.analisis.backend.exception.BadRequestException;
import com.analisis.backend.exception.ResourceNotFoundException;
import com.analisis.backend.mapper.UserMapper;
import com.analisis.backend.model.entity.User;
import com.analisis.backend.repository.UserRepository;
import com.analisis.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public UserResponseDto create(UserRequestDto request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Ya existe un usuario registrado con el correo: " + request.getEmail());
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        return userMapper.toResponseDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto getById(Long id) {
        User user = findUserById(id);
        return userMapper.toResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDto> getPaginated(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toResponseDto);
    }

    @Override
    public UserResponseDto update(Long id, UserRequestDto request) {
        User existingUser = findUserById(id);

        if (!existingUser.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("El correo " + request.getEmail() + " ya está en uso por otro usuario.");
        }

        userMapper.updateEntityFromDto(request, existingUser);
        User updatedUser = userRepository.save(existingUser);
        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    public void delete(Long id) {
        User existingUser = findUserById(id);
        userRepository.delete(existingUser);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }
}
