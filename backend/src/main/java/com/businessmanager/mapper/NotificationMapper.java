package com.businessmanager.mapper;

import com.businessmanager.dto.notification.NotificationDtos.NotificationResponse;
import com.businessmanager.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
