package com.businessmanager.service;

import com.businessmanager.dto.customer.CustomerRequest;
import com.businessmanager.dto.customer.CustomerResponse;
import com.businessmanager.entity.Customer;
import com.businessmanager.entity.NotificationType;
import com.businessmanager.exception.ResourceNotFoundException;
import com.businessmanager.mapper.CustomerMapper;
import com.businessmanager.repository.CustomerRepository;
import com.businessmanager.repository.InvoiceRepository;
import com.businessmanager.repository.ProjectRepository;
import com.businessmanager.repository.QuoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private NotificationService notificationService;
    @Mock private QuoteRepository quoteRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ProjectRepository projectRepository;
    @Spy private CustomerMapper customerMapper = Mappers.getMapper(CustomerMapper.class);

    @InjectMocks private CustomerService customerService;

    @Test
    void create_persistsCustomerAndNotifiesTeam() {
        CustomerRequest request = new CustomerRequest("Jean Dupont", "ACME", "jean@acme.com", null, null, null);
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse response = customerService.create(request);

        assertThat(response.name()).isEqualTo("Jean Dupont");
        assertThat(response.archived()).isFalse();
        verify(notificationService).notify(eq(NotificationType.NEW_CUSTOMER), anyString(), anyString());
    }

    @Test
    void findById_throwsWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void setArchived_updatesFlag() {
        UUID id = UUID.randomUUID();
        Customer customer = Customer.builder().id(id).name("Jean").email("j@x.fr").build();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        customerService.setArchived(id, true);

        assertThat(customer.isArchived()).isTrue();
        verify(customerRepository).save(customer);
    }
}
