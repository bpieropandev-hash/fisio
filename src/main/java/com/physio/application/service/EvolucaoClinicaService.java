package com.physio.application.service;

import com.physio.infrastructure.in.web.dto.EvolucaoClinicaDTO;
import com.physio.infrastructure.in.web.dto.EvolucaoClinicaRequestDTO;
import com.physio.infrastructure.out.persistence.entity.AtendimentoEntity;
import com.physio.infrastructure.out.persistence.entity.EvolucaoClinicaEntity;
import com.physio.infrastructure.out.persistence.repository.EvolucaoClinicaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EvolucaoClinicaService {
    private static final List<String> PROCEDIMENTOS = List.of("PRESSAO_ARTERIAL", "ALONGAMENTO", "FORTALECIMENTO",
            "MOBILIZACAO", "TERAPIA_MANUAL", "EXERCICIO_RESPIRATORIO", "PILATES");
    private static final List<String> EQUIPAMENTOS = List.of("REFORMER", "CADILLAC", "CHAIR", "BARREL", "SOLO", "ACESSORIOS");
    private static final Map<String, String> NOMES = Map.ofEntries(
            Map.entry("PRESSAO_ARTERIAL", "Pressão arterial"), Map.entry("ALONGAMENTO", "Alongamento"),
            Map.entry("FORTALECIMENTO", "Fortalecimento"), Map.entry("MOBILIZACAO", "Mobilização"),
            Map.entry("TERAPIA_MANUAL", "Terapia manual"), Map.entry("EXERCICIO_RESPIRATORIO", "Exercício respiratório"),
            Map.entry("PILATES", "Pilates"), Map.entry("REFORMER", "Reformer"), Map.entry("CADILLAC", "Cadillac"),
            Map.entry("CHAIR", "Chair"), Map.entry("BARREL", "Barrel"), Map.entry("SOLO", "Solo"), Map.entry("ACESSORIOS", "Acessórios"));
    private final EvolucaoClinicaRepository repository;
    private final EntityManager em;

    @Transactional(readOnly = true)
    public EvolucaoClinicaDTO buscar(Integer id) {
        var atendimento = exigirAtendimento(id, false);
        return repository.findFirstByAtendimentoIdOrderByVersaoDesc(id).map(this::dto)
                .orElseGet(() -> new EvolucaoClinicaDTO(0, List.of(), List.of(), null, null, null,
                        "", texto(atendimento.getEvolucao()), "", null));
    }

    @Transactional(readOnly = true)
    public List<EvolucaoClinicaDTO> historico(Integer id) {
        exigirAtendimento(id, false);
        return repository.findByAtendimentoIdOrderByVersaoDesc(id).stream().map(this::dto).toList();
    }

    @Transactional
    public EvolucaoClinicaDTO salvar(Integer id, EvolucaoClinicaRequestDTO request, String autor) {
        validar(request, autor);
        // O lock no atendimento também serializa a criação da primeira revisão, quando ainda não há ficha.
        var atendimento = exigirAtendimento(id, true);
        var anterior = repository.findFirstByAtendimentoIdOrderByVersaoDesc(id);
        int versaoAtual = anterior.map(EvolucaoClinicaEntity::getVersao).orElse(0);
        if (request.versao() != versaoAtual) throw new EvolucaoVersaoConflitanteException();
        String legado = anterior.map(EvolucaoClinicaEntity::getTextoLegado).orElse(texto(atendimento.getEvolucao()));
        var procedimentos = ordenar(request.procedimentos(), PROCEDIMENTOS);
        var equipamentos = ordenar(request.equipamentos(), EQUIPAMENTOS);
        String observacoes = texto(request.observacoes()).trim();
        String resumo = resumir(procedimentos, equipamentos, request, observacoes, legado);
        var revisao = repository.save(EvolucaoClinicaEntity.builder().atendimentoId(id).versao(versaoAtual + 1)
                .procedimentos(String.join(",", procedimentos)).equipamentos(String.join(",", equipamentos))
                .pressaoSistolica(request.pressaoSistolica()).pressaoDiastolica(request.pressaoDiastolica())
                .dor(request.dor()).observacoes(observacoes).resumo(resumo).textoLegado(legado)
                .autor(autor).atualizadoEm(LocalDateTime.now()).build());
        atendimento.setEvolucao(resumo);
        return dto(revisao);
    }

    private AtendimentoEntity exigirAtendimento(Integer id, boolean bloquear) {
        AtendimentoEntity atendimento = bloquear ? em.find(AtendimentoEntity.class, id, LockModeType.PESSIMISTIC_WRITE)
                : em.find(AtendimentoEntity.class, id);
        if (atendimento == null) throw new EntityNotFoundException("Atendimento não encontrado.");
        return atendimento;
    }

    private void validar(EvolucaoClinicaRequestDTO r, String autor) {
        if (r == null || r.versao() == null || r.versao() < 0) throw new IllegalArgumentException("Informe a versão atual da ficha.");
        if (autor == null || autor.isBlank() || autor.length() > 255) throw new IllegalArgumentException("Autor inválido.");
        validarCodigos(r.procedimentos(), PROCEDIMENTOS);
        validarCodigos(r.equipamentos(), EQUIPAMENTOS);
        if (texto(r.observacoes()).length() > 5000) throw new IllegalArgumentException("Observações devem ter até 5000 caracteres.");
        if (r.dor() != null && (r.dor() < 0 || r.dor() > 10)) throw new IllegalArgumentException("Dor deve estar entre 0 e 10.");
        if (r.procedimentos().contains("PRESSAO_ARTERIAL")) {
            if (r.pressaoSistolica() == null || r.pressaoDiastolica() == null || r.pressaoSistolica() < 40
                    || r.pressaoSistolica() > 300 || r.pressaoDiastolica() < 20 || r.pressaoDiastolica() > 200
                    || r.pressaoSistolica() <= r.pressaoDiastolica())
                throw new IllegalArgumentException("Informe pressão sistólica (40–300) e diastólica (20–200), com sistólica maior que diastólica.");
        } else if (r.pressaoSistolica() != null || r.pressaoDiastolica() != null) {
            throw new IllegalArgumentException("Marque pressão arterial para registrar as medidas.");
        }
    }

    private void validarCodigos(List<String> valores, List<String> catalogo) {
        if (valores == null || valores.size() > catalogo.size() || valores.stream().anyMatch(v -> v == null || !catalogo.contains(v))
                || new HashSet<>(valores).size() != valores.size())
            throw new IllegalArgumentException("Seleção contém códigos inválidos ou repetidos.");
    }

    private List<String> ordenar(List<String> valores, List<String> catalogo) {
        return catalogo.stream().filter(valores::contains).toList();
    }

    private String resumir(List<String> procedimentos, List<String> equipamentos, EvolucaoClinicaRequestDTO r,
                           String observacoes, String legado) {
        var partes = new ArrayList<String>();
        if (!procedimentos.isEmpty()) partes.add("Procedimentos: " + nomes(procedimentos) + ".");
        if (!equipamentos.isEmpty()) partes.add("Equipamentos: " + nomes(equipamentos) + ".");
        if (r.pressaoSistolica() != null) partes.add("Pressão arterial: " + r.pressaoSistolica() + "/" + r.pressaoDiastolica() + " mmHg.");
        if (r.dor() != null) partes.add("Dor: " + r.dor() + "/10.");
        if (!observacoes.isBlank()) partes.add("Observações: " + observacoes);
        String resumo = String.join(" ", partes);
        if (!legado.isBlank()) resumo += (resumo.isEmpty() ? "" : "\n") + "Registro anterior: " + legado;
        return resumo;
    }

    private String nomes(List<String> codigos) { return String.join(", ", codigos.stream().map(NOMES::get).toList()); }
    private String texto(String valor) { return valor == null ? "" : valor; }
    private List<String> codigos(String valor) { return valor.isEmpty() ? List.of() : List.of(valor.split(",")); }
    private EvolucaoClinicaDTO dto(EvolucaoClinicaEntity e) {
        return new EvolucaoClinicaDTO(e.getVersao(), codigos(e.getProcedimentos()), codigos(e.getEquipamentos()),
                e.getPressaoSistolica(), e.getPressaoDiastolica(), e.getDor(), e.getObservacoes(), e.getResumo(), e.getAutor(), e.getAtualizadoEm());
    }
}
