package com.physio.infrastructure.out.persistence.repository;
import com.physio.infrastructure.out.persistence.entity.RemarcacaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RemarcacaoRepository extends JpaRepository<RemarcacaoEntity,String> {
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
 @org.springframework.data.jpa.repository.Query("SELECT s FROM ServicoConfigEntity s WHERE s.id = :id")
 com.physio.infrastructure.out.persistence.entity.ServicoConfigEntity bloquearServico(@org.springframework.data.repository.query.Param("id") Long id);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
 @org.springframework.data.jpa.repository.Query("SELECT p FROM PacienteEntity p WHERE p.id = :id")
 com.physio.infrastructure.out.persistence.entity.PacienteEntity bloquearPaciente(@org.springframework.data.repository.query.Param("id") Long id);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_READ)
 @org.springframework.data.jpa.repository.Query("SELECT a FROM AssinaturaEntity a WHERE a.paciente.id = :paciente AND a.servico.id = :servico ORDER BY a.id")
 java.util.List<com.physio.infrastructure.out.persistence.entity.AssinaturaEntity> bloquearAssinaturas(@org.springframework.data.repository.query.Param("paciente") Long paciente,@org.springframework.data.repository.query.Param("servico") Long servico);
}
