package com.physio.application.service;

import com.physio.domain.model.ServicoConfig;
import com.physio.domain.ports.out.ServicoRepositoryPort;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicoSplitTest {
    private final ServicoRepositoryPort repository = mock(ServicoRepositoryPort.class);
    private final ConfiguracoesService configuracoes = mock(ConfiguracoesService.class);

    private ServicoConfig servico(String clinica, String profissional) {
        return ServicoConfig.builder().id(1).nome("Pilates").capacidade(10)
                .pctClinica(clinica == null ? null : new BigDecimal(clinica))
                .pctProfissional(profissional == null ? null : new BigDecimal(profissional)).build();
    }

    @Test void criacaoRejeitaSomaInvalidaAusentesENegativosSemSalvar() {
        var service = new CriarServicoService(repository, configuracoes);
        for (var invalido : new ServicoConfig[]{servico("20", "70"), servico(null, "100"),
                servico("-1", "101"), servico("101", "-1")}) {
            assertThatThrownBy(() -> service.criarServico(invalido))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("percentuais");
        }
        verify(repository, never()).salvar(any());
    }

    @Test void atualizacaoParcialValidaEstadoFinalAntesDeModificarExistente() {
        var existente = servico("20", "80");
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(existente));
        var parcial = ServicoConfig.builder().nome("Outro nome").pctClinica(new BigDecimal("30")).build();
        assertThatThrownBy(() -> new AtualizarServicoService(repository).atualizar(1L, parcial))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("100");
        assertThat(existente.getPctClinica()).isEqualByComparingTo("20");
        assertThat(existente.getNome()).isEqualTo("Pilates");
        verify(repository, never()).salvar(any());
    }

    @Test void aceitaDecimaisLimitesEAtualizacaoSemAlterarSplit() {
        when(repository.salvar(any())).thenAnswer(i -> i.getArgument(0));
        var criar = new CriarServicoService(repository, configuracoes);
        assertThat(criar.criarServico(servico("33.33", "66.67")).getPctClinica()).isEqualByComparingTo("33.33");
        assertThatCode(() -> criar.criarServico(servico("0", "100"))).doesNotThrowAnyException();
        var existente = servico("20.0", "80.00");
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(existente));
        var atualizado = new AtualizarServicoService(repository).atualizar(1L, ServicoConfig.builder().valorBase(BigDecimal.TEN).build());
        assertThat(atualizado.getPctProfissional()).isEqualByComparingTo("80");
        assertThat(atualizado.getValorBase()).isEqualByComparingTo("10");
    }
}
