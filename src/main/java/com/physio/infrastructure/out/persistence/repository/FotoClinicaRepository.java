package com.physio.infrastructure.out.persistence.repository;
import com.physio.infrastructure.in.web.dto.FotoClinicaDTO;
import com.physio.infrastructure.out.persistence.entity.FotoClinicaEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface FotoClinicaRepository extends JpaRepository<FotoClinicaEntity,Long> {
    Optional<FotoClinicaEntity> findByIdAndPacienteId(Long id,Integer pacienteId);
    // A galeria carrega somente metadados; os bytes saem apenas pelo endpoint privado da imagem.
    @Query("select new com.physio.infrastructure.in.web.dto.FotoClinicaDTO(f.id,f.pacienteId,f.dataCaptura,f.regiao,f.observacao,f.criadoEm,f.autor) from FotoClinicaEntity f where f.pacienteId=:pacienteId order by f.dataCaptura desc,f.id desc")
    List<FotoClinicaDTO> listarMetadados(@Param("pacienteId") Integer pacienteId);
}
