package com.physio.infrastructure.out.persistence.mapper;

import com.physio.domain.model.Atendimento;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AtendimentoMapper {
    Atendimento toDomain(AtendimentoEntity entity);
    AtendimentoEntity toEntity(Atendimento domain);
}

