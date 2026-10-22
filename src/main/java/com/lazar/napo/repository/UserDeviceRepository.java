package com.lazar.napo.repository;

import com.lazar.napo.entity.DevicePlatform;
import com.lazar.napo.entity.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserEmailIgnoreCaseAndPlatformAndPushToken(
            String email,
            DevicePlatform platform,
            String pushToken
    );

    List<UserDevice> findByUserEmailIgnoreCaseOrderByLastSeenAtDesc(String email);

    Optional<UserDevice> findByIdAndUserEmailIgnoreCase(Long id, String email);
}
