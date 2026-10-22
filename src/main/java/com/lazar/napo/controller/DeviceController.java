package com.lazar.napo.controller;

import com.lazar.napo.dto.DeviceResponse;
import com.lazar.napo.dto.RegisterDeviceRequest;
import com.lazar.napo.service.DeviceRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceRegistrationService deviceRegistrationService;

    @PostMapping
    public ResponseEntity<DeviceResponse> register(
            Authentication authentication,
            @Valid @RequestBody RegisterDeviceRequest request
    ) {
        DeviceResponse response = deviceRegistrationService.register(authentication.getName(), request);
        return ResponseEntity.created(URI.create("/api/devices/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> findAll(Authentication authentication) {
        return ResponseEntity.ok(deviceRegistrationService.findAll(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unregister(Authentication authentication, @PathVariable Long id) {
        deviceRegistrationService.unregister(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
