package com.physio.infrastructure.out.persistence.mapper;

import com.physio.domain.model.CobrancaMensal;
import com.physio.infrastructure.out.persistence.entity.CobrancaMensalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {AssinaturaMapper.class})
public interface CobrancaMensalMapper {
    CobrancaMensal toDomain(CobrancaMensalEntity entity);
    CobrancaMensalEntity toEntity(CobrancaMensal domain);
}

