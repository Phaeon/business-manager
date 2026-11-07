package com.businessmanager.mapper;

import com.businessmanager.dto.customer.CustomerRequest;
import com.businessmanager.dto.customer.CustomerResponse;
import com.businessmanager.entity.Customer;
import org.mapstruct.Builder;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true))
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    Customer toEntity(CustomerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(CustomerRequest request, @MappingTarget Customer customer);
}
