package com.lazar.napo.dto;

import java.time.OffsetDateTime;

public record ProcessDueRemindersRequest(
        OffsetDateTime processUntil
) {
}
