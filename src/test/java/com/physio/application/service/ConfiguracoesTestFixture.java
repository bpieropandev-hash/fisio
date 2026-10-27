package com.physio.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.physio.infrastructure.in.web.dto.ConfiguracoesDTO;
import com.physio.infrastructure.out.persistence.entity.ConfiguracoesEntity;
import com.physio.infrastructure.out.persistence.repository.ConfiguracoesRepository;
import java.util.*;
import static org.mockito.Mockito.*;

final class ConfiguracoesTestFixture {
    final ObjectMapper mapper = new ObjectMapper();
    final ConfiguracoesRepository repository = mock(ConfiguracoesRepository.class);
    final ConfiguracoesEntity row = new ConfiguracoesEntity(1,0L,null);
    final ConfiguracoesService service = new ConfiguracoesService(repository,mapper,"Clínica Teste","12345678901");
    ConfiguracoesTestFixture() {
        when(repository.findById(1)).thenReturn(Optional.of(row));
        when(repository.bloquear()).thenReturn(Optional.of(row));
    }
    void alterar(String key,Object value) {
        Map<String,Object> map = mapper.convertValue(service.buscar(),new TypeReference<Map<String,Object>>() {});
        map.put(key,value); service.salvar(mapper.convertValue(map,ConfiguracoesDTO.class));
    }
}
