package com.physio.infrastructure.out.persistence.mapper;

import com.physio.domain.model.Assinatura;
import com.physio.infrastructure.out.persistence.entity.AssinaturaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {PacienteMapper.class, ServicoConfigMapper.class})
public interface AssinaturaMapper {
    Assinatura toDomain(AssinaturaEntity entity);
    AssinaturaEntity toEntity(Assinatura domain);
}

