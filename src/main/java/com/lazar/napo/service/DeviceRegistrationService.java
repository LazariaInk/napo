package com.lazar.napo.service;

import com.lazar.napo.dto.DeviceResponse;
import com.lazar.napo.dto.RegisterDeviceRequest;
import com.lazar.napo.entity.UserAccount;
import com.lazar.napo.entity.UserDevice;
import com.lazar.napo.exception.ResourceNotFoundException;
import com.lazar.napo.repository.UserAccountRepository;
import com.lazar.napo.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceRegistrationService {

    private final UserDeviceRepository userDeviceRepository;
    private final UserAccountRepository userAccountRepository;

    @Transactional
    public DeviceResponse register(String userEmail, RegisterDeviceRequest request) {
        UserDevice device = userDeviceRepository
                .findByUserEmailIgnoreCaseAndPlatformAndInstallationId(
                        userEmail, request.platform(), request.installationId())
                .orElseGet(() -> registerNewInstallation(userEmail, request));

        device.setInstallationId(request.installationId());
        device.setPushToken(request.pushToken());
        device.setAppVersion(request.appVersion());
        device.setDeviceName(request.deviceName());
        device.setEnabled(true);
        device.setLastSeenAt(OffsetDateTime.now());
        return toResponse(userDeviceRepository.save(device));
    }

    private UserDevice registerNewInstallation(String userEmail, RegisterDeviceRequest request) {
        UserAccount user = getUser(userEmail);
        userDeviceRepository.findByUserEmailIgnoreCaseOrderByLastSeenAtDesc(userEmail)
                .stream()
                .filter(existing -> existing.getPlatform() == request.platform())
                .filter(existing -> !request.installationId().equals(existing.getInstallationId()))
                .forEach(userDeviceRepository::delete);

        UserDevice newDevice = new UserDevice();
        newDevice.setUser(user);
        newDevice.setPlatform(request.platform());
        return newDevice;
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> findAll(String userEmail) {
        return userDeviceRepository.findByUserEmailIgnoreCaseOrderByLastSeenAtDesc(userEmail)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void unregister(String userEmail, Long deviceId) {
        UserDevice device = userDeviceRepository.findByIdAndUserEmailIgnoreCase(deviceId, userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device with id %d was not found".formatted(deviceId)));
        userDeviceRepository.delete(device);
    }

    private UserAccount getUser(String email) {
        return userAccountRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user was not found"));
    }

    private DeviceResponse toResponse(UserDevice device) {
        return new DeviceResponse(
                device.getId(),
                device.getPlatform(),
                device.getInstallationId(),
                device.getAppVersion(),
                device.getDeviceName(),
                device.isEnabled(),
                device.getLastSeenAt(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
