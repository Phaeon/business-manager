package com.businessmanager.mapper;

import com.businessmanager.dto.appointment.AppointmentDtos.AppointmentResponse;
import com.businessmanager.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppointmentMapper {

    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.name", target = "customerName")
    AppointmentResponse toResponse(Appointment appointment);
}
