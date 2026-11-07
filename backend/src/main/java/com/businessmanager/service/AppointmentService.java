package com.businessmanager.service;

import com.businessmanager.dto.appointment.AppointmentDtos.AppointmentRequest;
import com.businessmanager.dto.appointment.AppointmentDtos.AppointmentResponse;
import com.businessmanager.entity.Appointment;
import com.businessmanager.entity.Customer;
import com.businessmanager.exception.BadRequestException;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.AppointmentMapper;
import com.businessmanager.repository.AppointmentRepository;
import com.businessmanager.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentMapper appointmentMapper;

    public List<AppointmentResponse> findBetween(LocalDate from, LocalDate to) {
        return appointmentRepository
                .findByStartAtBetweenOrderByStartAtAsc(from.atStartOfDay(), to.plusDays(1).atStartOfDay())
                .stream().map(appointmentMapper::toResponse).toList();
    }

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        Appointment appointment = new Appointment();
        apply(appointment, request);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse update(UUID id, AppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rendez-vous introuvable : " + id));
        apply(appointment, request);
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public void delete(UUID id) {
        if (!appointmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rendez-vous introuvable : " + id);
        }
        appointmentRepository.deleteById(id);
    }

    private void apply(Appointment appointment, AppointmentRequest request) {
        if (request.endAt() != null && request.endAt().isBefore(request.startAt())) {
            throw new BadRequestException("La fin du rendez-vous doit être postérieure à son début");
        }
        Customer customer = null;
        if (request.customerId() != null) {
            customer = customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + request.customerId()));
        }
        appointment.setTitle(request.title().trim());
        appointment.setStartAt(request.startAt());
        appointment.setEndAt(request.endAt());
        appointment.setLocation(request.location());
        appointment.setNotes(request.notes());
        appointment.setCustomer(customer);
    }
}
