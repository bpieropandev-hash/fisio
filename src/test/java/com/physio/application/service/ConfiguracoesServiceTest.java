package com.physio.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.physio.infrastructure.in.web.dto.ConfiguracoesDTO;
import com.physio.infrastructure.out.persistence.entity.ConfiguracoesEntity;
import com.physio.infrastructure.out.persistence.repository.ConfiguracoesRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracoesServiceTest {
    final ConfiguracoesRepository repository = mock(ConfiguracoesRepository.class);
    final ObjectMapper mapper = new ObjectMapper();
    final ConfiguracoesEntity row = new ConfiguracoesEntity(1, 0L, null);
    final ConfiguracoesService service = new ConfiguracoesService(repository, mapper, "ENV Clínica", "123");
    ConfiguracoesServiceTest() {
        when(repository.findById(1)).thenReturn(Optional.of(row));
        when(repository.bloquear()).thenReturn(Optional.of(row));
    }
    ConfiguracoesDTO patch(ConfiguracoesDTO base, String key, Object value) {
        Map<String,Object> values = mapper.convertValue(base, Map.class);
        values.put(key,value); return mapper.convertValue(values,ConfiguracoesDTO.class);
    }
    @Test void defaultsUsamEnvAtePrimeiroSaveEBrancosPersistidosNaoRetornamEnv() {
        var initial = service.buscar();
        assertThat(initial.versao()).isZero(); assertThat(initial.emitenteNome()).isEqualTo("ENV Clínica");
        assertThat(initial.capacidadePilates()).isEqualTo(15); assertThat(initial.fotoLimiteMiB()).isEqualTo(5);
        var blank = patch(patch(initial,"emitenteNome",""),"emitenteDocumento","");
        assertThat(service.salvar(blank).versao()).isEqualTo(1);
        assertThat(service.buscar().emitenteNome()).isEmpty(); assertThat(service.buscar().emitenteDocumento()).isEmpty();
    }
    @Test void versaoObsoletaNaoAlteraConfiguracao() {
        var initial=service.buscar(); service.salvar(patch(initial,"capacidadePilates",8));
        assertThatThrownBy(()->service.salvar(initial)).isInstanceOf(ConfiguracoesVersaoConflitanteException.class);
        assertThat(service.buscar().capacidadePilates()).isEqualTo(8);
    }
    @Test void validaLimitesCamposEParDoEmitente() {
        var initial=service.buscar();
        Map<String,Object> invalidos=Map.of("mensalidadeValorPadrao","1.001","mensalidadeDiaVencimento",29,"capacidadeFisioterapia",0,
                "capacidadePilates",101,"fotoLimiteMiB",6,"emitenteNome","", "emitenteDocumento","x".repeat(31));
        invalidos.forEach((key,value)->assertThatThrownBy(()->service.salvar(patch(initial,key,value))).isInstanceOf(IllegalArgumentException.class));
        assertThatThrownBy(()->service.salvar(patch(initial,"agruparAgenda",null))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void normalizaTextosECatalogosPreservandoCodigos() {
        var initial=service.buscar();
        var items=new ArrayList<>(initial.procedimentos());
        items.set(0,new ConfiguracoesDTO.Item("PRESSAO_ARTERIAL"," Pressão aferida ",false));
        items.add(new ConfiguracoesDTO.Item("NOVO"," Novo procedimento ",true));
        var result=service.salvar(patch(patch(initial,"emitenteNome"," Nome "),"procedimentos",items));
        assertThat(result.emitenteNome()).isEqualTo("Nome"); assertThat(result.procedimentos().getFirst().nome()).isEqualTo("Pressão aferida");
        assertThat(result.procedimentos().getLast().codigo()).isEqualTo("NOVO");
        assertThatThrownBy(()->service.salvar(patch(result,"procedimentos",initial.procedimentos()))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejeitaRemocaoCodigosExistentesDuplicatasECodigoInvalido() {
        var initial=service.buscar();
        var semPressao=initial.procedimentos().subList(1,initial.procedimentos().size());
        assertThatThrownBy(()->service.salvar(patch(initial,"procedimentos",semPressao))).isInstanceOf(IllegalArgumentException.class);
        for(var item:List.of(new ConfiguracoesDTO.Item("minusculo","Nome",true),new ConfiguracoesDTO.Item("NOVO","pilates",true),new ConfiguracoesDTO.Item("NOVO"," ",true),new ConfiguracoesDTO.Item("PILATES","Outro",true))) {
            var items=new ArrayList<>(initial.procedimentos()); items.add(item);
            assertThatThrownBy(()->service.salvar(patch(initial,"procedimentos",items))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(()->service.salvar(patch(initial,"equipamentos",List.of()))).isInstanceOf(IllegalArgumentException.class);
    }
}
