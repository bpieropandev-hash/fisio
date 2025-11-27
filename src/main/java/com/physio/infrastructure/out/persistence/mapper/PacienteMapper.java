package com.physio.infrastructure.out.persistence.mapper;

import com.physio.domain.model.Paciente;
import com.physio.infrastructure.out.persistence.entity.PacienteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PacienteMapper {
    Paciente toDomain(PacienteEntity entity);
    PacienteEntity toEntity(Paciente domain);
}

