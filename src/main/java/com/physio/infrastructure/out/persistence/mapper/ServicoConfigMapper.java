package com.physio.infrastructure.out.persistence.mapper;

import com.physio.domain.model.ServicoConfig;
import com.physio.infrastructure.out.persistence.entity.ServicoConfigEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ServicoConfigMapper {
    ServicoConfig toDomain(ServicoConfigEntity entity);
    ServicoConfigEntity toEntity(ServicoConfig domain);
}

