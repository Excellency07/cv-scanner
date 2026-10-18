package com.backend.cv_scanner.model.mapper;

import com.backend.cv_scanner.dao.entity.Account;
import com.backend.cv_scanner.model.dto.account.AccountResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponseDto toDto(Account account);
}
