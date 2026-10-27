package com.physio.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.physio.infrastructure.in.web.dto.ConfiguracoesDTO;
import com.physio.infrastructure.in.web.dto.ConfiguracoesDTO.Item;
import com.physio.infrastructure.out.persistence.entity.ConfiguracoesEntity;
import com.physio.infrastructure.out.persistence.repository.ConfiguracoesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ConfiguracoesService {
    private final ConfiguracoesRepository repository;
    private final ObjectMapper mapper;
    private final String emitenteNome, emitenteDocumento;
    public ConfiguracoesService(ConfiguracoesRepository repository, ObjectMapper mapper,
            @Value("${recibo.emitente.nome:}") String emitenteNome,
            @Value("${recibo.emitente.documento:}") String emitenteDocumento) {
        this.repository=repository; this.mapper=mapper;
        this.emitenteNome=emitenteNome; this.emitenteDocumento=emitenteDocumento;
    }
    @Transactional(readOnly=true)
    public ConfiguracoesDTO buscar() { return ler(repository.findById(1).orElseThrow(this::ausente)); }

    @Transactional
    public ConfiguracoesDTO salvar(ConfiguracoesDTO request) {
        if(request==null || request.versao()==null || request.versao()<0)
            throw new IllegalArgumentException("Informe a versão atual das configurações.");
        var row=repository.bloquear().orElseThrow(this::ausente);
        if(!row.getVersao().equals(request.versao())) throw new ConfiguracoesVersaoConflitanteException();
        var atual=ler(row);
        var nome=texto(request.emitenteNome(),255,"Nome do emitente",true);
        var documento=texto(request.emitenteDocumento(),30,"Documento do emitente",true);
        if(nome.isEmpty()!=documento.isEmpty()) throw new IllegalArgumentException("Informe nome e documento do emitente juntos.");
        var valor=request.mensalidadeValorPadrao();
        if(valor==null || valor.signum()<0 || valor.compareTo(new BigDecimal("99999999.99"))>0 || valor.stripTrailingZeros().scale()>2)
            throw new IllegalArgumentException("Valor padrão deve estar entre 0 e 99999999,99, com até duas casas decimais.");
        intervalo(request.mensalidadeDiaVencimento(),1,28,"Dia do vencimento");
        intervalo(request.capacidadeFisioterapia(),1,100,"Capacidade da fisioterapia");
        intervalo(request.capacidadePilates(),1,100,"Capacidade do pilates");
        intervalo(request.fotoLimiteMiB(),1,5,"Limite das fotos (MiB)");
        if(request.agruparAgenda()==null || request.exibirDor()==null) throw new IllegalArgumentException("Informe as opções de agenda e dor.");
        var procedimentos=catalogo(request.procedimentos(),atual.procedimentos(),true);
        var equipamentos=catalogo(request.equipamentos(),atual.equipamentos(),false);
        var nova=new ConfiguracoesDTO(row.getVersao()+1,nome,documento,valor,request.mensalidadeDiaVencimento(),
                request.capacidadeFisioterapia(),request.capacidadePilates(),request.agruparAgenda(),request.fotoLimiteMiB(),
                request.exibirDor(),procedimentos,equipamentos);
        try { row.setConfiguracao(mapper.writeValueAsString(nova)); }
        catch(JsonProcessingException ex) { throw new IllegalStateException("Não foi possível salvar configurações.",ex); }
        row.setVersao(nova.versao()); repository.save(row); return nova;
    }
    private ConfiguracoesDTO ler(ConfiguracoesEntity row) {
        if(row.getConfiguracao()==null) return defaults(row.getVersao());
        try { return mapper.readValue(row.getConfiguracao(),ConfiguracoesDTO.class); }
        catch(JsonProcessingException ex) { throw new IllegalStateException("Configurações persistidas inválidas.",ex); }
    }
    private ConfiguracoesDTO defaults(long versao) {
        return new ConfiguracoesDTO(versao,Objects.toString(emitenteNome,"").trim(),Objects.toString(emitenteDocumento,"").trim(),
                BigDecimal.ZERO,5,1,15,true,5,true,
                List.of(item("PRESSAO_ARTERIAL","Pressão aferida"),item("ALONGAMENTO","Alongamento"),item("FORTALECIMENTO","Fortalecimento"),
                        item("MOBILIZACAO","Mobilização"),item("TERAPIA_MANUAL","Terapia manual"),item("EXERCICIO_RESPIRATORIO","Exercício respiratório"),item("PILATES","Pilates")),
                List.of(item("REFORMER","Reformer"),item("CADILLAC","Cadillac"),item("CHAIR","Chair"),item("BARREL","Barrel"),item("SOLO","Solo"),item("ACESSORIOS","Acessórios")));
    }
    private Item item(String codigo,String nome) { return new Item(codigo,nome,true); }
    private List<Item> catalogo(List<Item> itens,List<Item> anteriores,boolean procedimentos) {
        if(itens==null || itens.size()>50 || procedimentos&&itens.isEmpty()) throw new IllegalArgumentException("Catálogo deve ter até 50 itens; procedimentos exige ao menos um.");
        var codigos=new HashSet<String>(); var nomes=new HashSet<String>(); var result=new ArrayList<Item>();
        for(var item:itens) {
            if(item==null || item.codigo()==null || !item.codigo().matches("[A-Z][A-Z0-9_]{0,49}") || item.ativo()==null)
                throw new IllegalArgumentException("Item do catálogo exige código válido e opção ativo.");
            var nome=texto(item.nome(),100,"Nome do item",false);
            if(!codigos.add(item.codigo()) || !nomes.add(nome.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("Códigos e nomes do catálogo devem ser únicos.");
            result.add(new Item(item.codigo(),nome,item.ativo()));
        }
        if(anteriores.stream().anyMatch(item->!codigos.contains(item.codigo())) || procedimentos&&!codigos.contains("PRESSAO_ARTERIAL"))
            throw new IllegalArgumentException("Códigos existentes não podem ser removidos ou alterados. Desative o item para preservar o histórico.");
        return List.copyOf(result);
    }
    private String texto(String valor,int max,String nome,boolean vazio) {
        if(valor==null) throw new IllegalArgumentException(nome+" é obrigatório.");
        valor=valor.trim();
        if(valor.length()>max || !vazio&&valor.isEmpty()) throw new IllegalArgumentException(nome+" deve ter "+(vazio?"até ":"entre 1 e ")+max+" caracteres.");
        return valor;
    }
    private void intervalo(Integer valor,int min,int max,String nome) {
        if(valor==null || valor<min || valor>max) throw new IllegalArgumentException(nome+" deve estar entre "+min+" e "+max+".");
    }
    private IllegalStateException ausente() { return new IllegalStateException("Configurações da clínica não inicializadas; verifique a migração V13."); }
}
