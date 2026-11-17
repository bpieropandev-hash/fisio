package com.physio.infrastructure.out.persistence.repository;
import com.physio.infrastructure.in.web.dto.AvaliacaoFisicaDTO;
import com.physio.infrastructure.out.persistence.entity.AvaliacaoFisicaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface AvaliacaoFisicaRepository extends JpaRepository<AvaliacaoFisicaEntity,Long> {
    Optional<AvaliacaoFisicaEntity> findByIdAndPacienteId(Long id,Integer pacienteId);
    // A lista carrega só metadados; os bytes saem apenas pelo endpoint privado do arquivo.
    @Query("select new com.physio.infrastructure.in.web.dto.AvaliacaoFisicaDTO(a.id,a.pacienteId,a.dataAvaliacao,a.titulo,a.observacao,a.criadoEm,a.autor,a.nomeArquivo,a.tipoConteudo) from AvaliacaoFisicaEntity a where a.pacienteId=:pacienteId order by a.dataAvaliacao desc,a.id desc")
    List<AvaliacaoFisicaDTO> listarMetadados(@Param("pacienteId") Integer pacienteId);
}
