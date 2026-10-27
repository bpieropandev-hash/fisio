package com.physio.application.service;
import com.physio.infrastructure.in.web.dto.EvolucaoClinicaRequestDTO;
import com.physio.infrastructure.out.persistence.entity.*;
import com.physio.infrastructure.out.persistence.repository.EvolucaoClinicaRepository;
import jakarta.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
class EvolucaoClinicaServiceTest {
    final EntityManager em=mock(EntityManager.class);
    final EvolucaoClinicaRepository repository=mock(EvolucaoClinicaRepository.class);
    final EvolucaoClinicaService service=new EvolucaoClinicaService(repository,em);
    final List<EvolucaoClinicaEntity> revisoes=new ArrayList<>();
    final AtendimentoEntity atendimento=AtendimentoEntity.builder().id(7).evolucao("Registro legado").build();
    @BeforeEach void setup() {
        lenient().when(em.find(AtendimentoEntity.class,7)).thenReturn(atendimento);
        lenient().when(em.find(AtendimentoEntity.class,7,LockModeType.PESSIMISTIC_WRITE)).thenReturn(atendimento);
        lenient().when(repository.findFirstByAtendimentoIdOrderByVersaoDesc(7)).thenAnswer(i->revisoes.stream().max(Comparator.comparing(EvolucaoClinicaEntity::getVersao)));
        lenient().when(repository.findByAtendimentoIdOrderByVersaoDesc(7)).thenAnswer(i->revisoes.stream().sorted(Comparator.comparing(EvolucaoClinicaEntity::getVersao).reversed()).toList());
        lenient().when(repository.save(any())).thenAnswer(i->{ EvolucaoClinicaEntity e=i.getArgument(0); revisoes.add(e); return e; });
    }
    @Test void leituraInicialPreservaLegadoSemPreSelecao() {
        var dto=service.buscar(7); assertThat(dto.versao()).isZero(); assertThat(dto.resumo()).isEqualTo("Registro legado");
        assertThat(dto.procedimentos()).isEmpty(); assertThat(dto.observacoes()).isEmpty();
    }
    @Test void pressaoAferidaExigeDuasMedidas() {
        assertThatThrownBy(()->service.salvar(7,req(0,List.of("PRESSAO_ARTERIAL"),List.of(),120,null,null,""),"p")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejeitaPressaoSemAfericao() {
        assertThatThrownBy(()->service.salvar(7,req(0,List.of(),List.of(),120,80,null,""),"p")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rejeitaPressaoInvalida() {
        for(var p:List.of(new int[]{39,20},new int[]{301,80},new int[]{120,201},new int[]{80,100}))
            assertThatThrownBy(()->service.salvar(7,req(0,List.of("PRESSAO_ARTERIAL"),List.of(),p[0],p[1],null,""),"p")).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void resumoDeterministicoPreservaLegadoEAutoria() {
        var dto=service.salvar(7,req(0,List.of("PILATES","PRESSAO_ARTERIAL"),List.of("REFORMER","SOLO"),120,80,3,"Melhora."),"profissional");
        assertThat(dto.versao()).isEqualTo(1);
        assertThat(dto.resumo()).isEqualTo("Procedimentos: Pressão arterial, Pilates. Equipamentos: Reformer, Solo. Pressão arterial: 120/80 mmHg. Dor: 3/10. Observações: Melhora.\nRegistro anterior: Registro legado");
        assertThat(atendimento.getEvolucao()).isEqualTo(dto.resumo()); assertThat(dto.autor()).isEqualTo("profissional");
    }
    @Test void versaoObsoletaNaoSobrescreveRevisao() {
        service.salvar(7,req(0,List.of("PILATES"),List.of(),null,null,null,"Primeira"),"p");
        assertThatThrownBy(()->service.salvar(7,req(0,List.of(),List.of(),null,null,null,"Antiga"),"q")).isInstanceOf(EvolucaoVersaoConflitanteException.class);
        assertThat(service.buscar(7).observacoes()).isEqualTo("Primeira"); assertThat(service.historico(7)).hasSize(1);
    }
    @Test void novaRevisaoMantemHistoricoELegadoSemDuplicacao() {
        service.salvar(7,req(0,List.of("PILATES"),List.of(),null,null,null,"Primeira"),"p");
        var dto=service.salvar(7,req(1,List.of("ALONGAMENTO"),List.of(),null,null,null,"Segunda"),"q");
        assertThat(service.historico(7)).extracting(e->e.versao()).containsExactly(2,1);
        assertThat(service.historico(7).get(1).observacoes()).isEqualTo("Primeira");
        assertThat(dto.resumo()).containsOnlyOnce("Registro legado").doesNotContain("Primeira");
    }
    @Test void rejeitaCodigosDesconhecidosDuplicadosDorETextoInvalidos() {
        var invalidos=List.of(req(0,List.of("DESCONHECIDO"),List.of(),null,null,null,""),req(0,List.of("PILATES","PILATES"),List.of(),null,null,null,""),
            req(0,List.of(),List.of("ERRADO"),null,null,null,""),req(0,List.of(),List.of(),null,null,11,""),req(0,List.of(),List.of(),null,null,null,"x".repeat(5001)));
        for(var r:invalidos) assertThatThrownBy(()->service.salvar(7,r,"p")).isInstanceOf(IllegalArgumentException.class);
    }
    private EvolucaoClinicaRequestDTO req(int v,List<String> p,List<String> e,Integer s,Integer d,Integer dor,String o) {return new EvolucaoClinicaRequestDTO(v,p,e,s,d,dor,o);}
}
