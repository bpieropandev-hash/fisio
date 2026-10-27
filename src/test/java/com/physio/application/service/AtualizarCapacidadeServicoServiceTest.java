package com.physio.application.service;

import com.physio.infrastructure.out.persistence.repository.ServicoConfigJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AtualizarCapacidadeServicoServiceTest {
    final ServicoConfigJpaRepository repository=mock(ServicoConfigJpaRepository.class);
    final AtualizarCapacidadeServicoService service=new AtualizarCapacidadeServicoService(repository);

    @Test void rejeitaCapacidadeAusenteOuForaDoIntervaloSemAlterarServico() {
        for(Integer valor:new Integer[]{null,0,101,-1})
            assertThatThrownBy(()->service.atualizar(1,valor)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1 e 100");
        verifyNoInteractions(repository);
    }
    @Test void atualizaCapacidadeNosLimitesPermitidos() {
        when(repository.atualizarCapacidade(7,1)).thenReturn(1);
        when(repository.atualizarCapacidade(7,100)).thenReturn(1);
        assertThatCode(()->service.atualizar(7,1)).doesNotThrowAnyException();
        assertThatCode(()->service.atualizar(7,100)).doesNotThrowAnyException();
        verify(repository).atualizarCapacidade(7,1);
        verify(repository).atualizarCapacidade(7,100);
        verifyNoMoreInteractions(repository);
    }
    @Test void servicoInexistenteRetornaNaoEncontrado() {
        assertThatThrownBy(()->service.atualizar(99,8)).isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }
}
