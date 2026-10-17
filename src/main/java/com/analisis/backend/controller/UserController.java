package com.analisis.backend.controller;

import com.analisis.backend.dto.request.UserRequestDto;
import com.analisis.backend.dto.response.ApiResponse;
import com.analisis.backend.dto.response.UserResponseDto;
import com.analisis.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponseDto>> create(@Valid @RequestBody UserRequestDto request) {
        UserResponseDto createdUser = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUser, "Usuario creado exitosamente"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> getById(@PathVariable Long id) {
        UserResponseDto user = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAll() {
        List<UserResponseDto> users = userService.getAll();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<UserResponseDto>>> getPaginated(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        Page<UserResponseDto> users = userService.getPaginated(pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponseDto>> update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto request) {
        UserResponseDto updatedUser = userService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Usuario actualizado exitosamente"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Usuario eliminado exitosamente"));
    }
}
