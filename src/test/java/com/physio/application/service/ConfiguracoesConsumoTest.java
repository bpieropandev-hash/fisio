package com.physio.application.service;

import com.physio.domain.model.*;
import com.physio.domain.ports.out.*;
import com.physio.infrastructure.in.web.dto.*;
import com.physio.infrastructure.out.persistence.entity.*;
import com.physio.infrastructure.out.persistence.repository.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracoesConsumoTest {
    final ConfiguracoesTestFixture config = new ConfiguracoesTestFixture();
    @Test void capacidadePadraoSomenteNovosServicosSemCapacidade() {
        var repo=mock(ServicoRepositoryPort.class);
        when(repo.salvar(any())).thenAnswer(i->i.getArgument(0));
        var service=new CriarServicoService(repo,config.service);
        config.alterar("capacidadePilates",8); config.alterar("capacidadeFisioterapia",3);
        var pilates=ServicoConfig.builder().nome("Pilates").tipo(TipoServico.PILATES).build();
        assertThat(service.criarServico(pilates).getCapacidade()).isEqualTo(8);
        var fisio=ServicoConfig.builder().nome("Fisio").tipo(TipoServico.FISIOTERAPIA).build();
        assertThat(service.criarServico(fisio).getCapacidade()).isEqualTo(3);
        var explicita=ServicoConfig.builder().nome("Personalizado").tipo(TipoServico.PILATES).capacidade(2).build();
        assertThat(service.criarServico(explicita).getCapacidade()).isEqualTo(2);
        config.alterar("capacidadePilates",4); assertThat(pilates.getCapacidade()).isEqualTo(8);
    }
    @Test void fotoRespeitaLimiteConfiguradoNoTamanhoDeclaradoENosBytes() {
        var fotos=mock(FotoClinicaRepository.class); var pacientes=mock(PacienteJpaRepository.class);
        when(pacientes.existsById(1)).thenReturn(true);
        var service=new FotoClinicaService(fotos,pacientes,config.service); config.alterar("fotoLimiteMiB",1);
        var imagem=new MockMultipartFile("imagem","f.png","image/png",new byte[1024*1024+1]);
        assertThatThrownBy(()->service.criar(1,imagem,LocalDate.now(),"Coluna","","p")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1 MiB");
        var tamanhoFalso=new MockMultipartFile("imagem","f.png","image/png",new byte[1024*1024+1]) { @Override public long getSize() { return 1; } };
        assertThatThrownBy(()->service.criar(1,tamanhoFalso,LocalDate.now(),"Coluna","","p")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("1 MiB");
        verify(fotos,never()).save(any());
    }
    @Test void novoReciboUsaEmitenteAtualESnapshotContinuaAposAlteracao() {
        var atendimentos=mock(AtendimentoRepositoryPort.class); var cobrancas=mock(CobrancaMensalRepositoryPort.class);
        var recibos=mock(ReciboJpaRepository.class); var lock=mock(AgendaTransacaoLock.class);
        var service=new ReciboService(atendimentos,cobrancas,recibos,lock,config.service);
        var data=LocalDate.of(2026,10,26);
        when(atendimentos.buscarPorId(1L)).thenReturn(Atendimento.builder().id(1).paciente(Paciente.builder().nome("Paciente").build())
                .servicoBase(ServicoConfig.builder().nome("Fisio").build()).valorCobrado(BigDecimal.TEN).statusPagamento(StatusPagamento.PAID)
                .dataPagamento(data).modalidadeCobranca(ModalidadeCobranca.AVULSO).build());
        var saved=new ArrayList<ReciboEntity>();
        when(recibos.save(any())).thenAnswer(i->{ReciboEntity e=i.getArgument(0); saved.add(e); return e;});
        config.alterar("emitenteNome","Novo emitente");
        var request=new ReciboRequestDTO(1,null,data,"Pagador","123");
        assertThat(service.emitir(request).emitenteNome()).isEqualTo("Novo emitente");
        config.alterar("emitenteNome","Outro emitente");
        when(recibos.findByAtendimentoId(1)).thenReturn(Optional.of(saved.getFirst()));
        assertThat(service.emitir(request).emitenteNome()).isEqualTo("Novo emitente");
    }
    final EntityManager em=mock(EntityManager.class);
    final EvolucaoClinicaRepository evolucoes=mock(EvolucaoClinicaRepository.class);
    final List<EvolucaoClinicaEntity> revisoes=new ArrayList<>();
    EvolucaoClinicaService evolucao() {
        when(em.find(AtendimentoEntity.class,7,LockModeType.PESSIMISTIC_WRITE)).thenReturn(AtendimentoEntity.builder().id(7).build());
        when(em.find(AtendimentoEntity.class,7)).thenReturn(AtendimentoEntity.builder().id(7).build());
        when(evolucoes.findFirstByAtendimentoIdOrderByVersaoDesc(7)).thenAnswer(i->revisoes.stream().max(Comparator.comparing(EvolucaoClinicaEntity::getVersao)));
        when(evolucoes.findByAtendimentoIdOrderByVersaoDesc(7)).thenAnswer(i->revisoes.stream().sorted(Comparator.comparing(EvolucaoClinicaEntity::getVersao).reversed()).toList());
        when(evolucoes.save(any())).thenAnswer(i->{EvolucaoClinicaEntity e=i.getArgument(0);revisoes.add(e);return e;});
        return new EvolucaoClinicaService(evolucoes,em,config.service);
    }
    void procedimento(String codigo,String nome,boolean ativo) {
        var items=new ArrayList<>(config.service.buscar().procedimentos());
        items.removeIf(i->i.codigo().equals(codigo)); items.add(new ConfiguracoesDTO.Item(codigo,nome,ativo));
        config.alterar("procedimentos",items);
    }
    EvolucaoClinicaRequestDTO req(int versao,List<String> procedimentos,Integer dor) {
        return new EvolucaoClinicaRequestDTO(versao,procedimentos,List.of(),null,null,dor,"");
    }
    @Test void evolucaoUsaCatalogoOrdemENomeAtualPreservaResumoAnterior() {
        var service=evolucao(); procedimento("NOVO","Novo procedimento",true);
        var primeira=service.salvar(7,req(0,List.of("NOVO","PILATES"),3),"p");
        assertThat(primeira.resumo()).isEqualTo("Procedimentos: Pilates, Novo procedimento. Dor: 3/10.");
        procedimento("NOVO","Nome atualizado",true);
        assertThat(service.salvar(7,req(1,List.of("NOVO"),3),"p").resumo()).contains("Nome atualizado");
        assertThat(service.historico(7).get(1).resumo()).isEqualTo(primeira.resumo());
    }
    @Test void inativoSoPodePermanecerNaRevisaoSeguinteSeJaEstavaMarcado() {
        var service=evolucao(); service.salvar(7,req(0,List.of("PILATES"),null),"p");
        procedimento("PILATES","Pilates",false); procedimento("ALONGAMENTO","Alongamento",false);
        assertThatThrownBy(()->service.salvar(7,req(1,List.of("PILATES","ALONGAMENTO"),null),"p")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("inativo");
        assertThat(service.salvar(7,req(1,List.of("PILATES"),null),"p").procedimentos()).containsExactly("PILATES");
        service.salvar(7,req(2,List.of(),null),"p");
        assertThatThrownBy(()->service.salvar(7,req(3,List.of("PILATES"),null),"p")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void dorOcultaPreservaAnteriorSemAceitarNovoValor() {
        var service=evolucao(); service.salvar(7,req(0,List.of(),4),"p"); config.alterar("exibirDor",false);
        assertThatThrownBy(()->service.salvar(7,req(1,List.of(),5),"p")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("dor");
        assertThat(service.salvar(7,req(1,List.of(),null),"p").dor()).isEqualTo(4);
        assertThat(service.salvar(7,req(2,List.of(),4),"p").dor()).isEqualTo(4);
    }
    @Test void dorOcultaNaoPodeSerCriadaEmFichaSemAnterior() {
        var service=evolucao(); config.alterar("exibirDor",false);
        assertThatThrownBy(()->service.salvar(7,req(0,List.of(),3),"p")).isInstanceOf(IllegalArgumentException.class);
    }
}
