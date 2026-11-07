package com.businessmanager.mapper;

import com.businessmanager.dto.user.UserDtos.AssigneeDto;
import com.businessmanager.dto.user.UserDtos.UserResponse;
import com.businessmanager.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(source = "fullName", target = "name")
    AssigneeDto toAssignee(User user);
}
