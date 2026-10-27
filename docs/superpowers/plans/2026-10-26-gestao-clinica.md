# Gestão clínica Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox syntax.

**Goal:** Implementar fotos comparáveis, evolução por marcação, recibos, mensalidade explícita e turmas.
**Architecture:** Ampliar módulos atuais, preservar legado. APIs autenticadas para galeria e ficha estruturada; cobertura mensal explícita; agenda agrupa sessões compatíveis mantendo pacientes individuais.
**Tech Stack:** Java 21, Spring Boot, PostgreSQL/Flyway, Angular 21, PrimeNG.
**Spec:** ../specs/2026-10-26-gestao-clinica.md e esclarecimento do usuário: marcar pressão aferida/equipamentos usados, sem digitar tudo.

## Global Constraints
- Usuário autorizou implementação em 26/10/2026. Sem deploy/merge em produção.
- Branch feat/gestao-clinica em cópias isoladas /workspace/fisio e /workspace/front-fisio.
- Mensalidade mantém modelo existente sem franquia. Não adicionar gateway nem emissão fiscal automática.
- Fotos privadas na primeira entrega: PostgreSQL bytea (máximo 5 MiB JPEG/PNG validado/decodificado), API autenticada, sem URLs públicas ou novo serviço externo; adaptável futuramente a object storage.
- Preservar texto de evolução e registros financeiros antigos; não inferir toda cortesia como mensalidade na migração.
- Sem novos construtores genéricos de formulário, IA ou gestão de salas/profissionais.

## Review Focus
- Isolamento entre IDs de paciente e imagem; tipos e tamanhos inválidos rejeitados.
- Pressão aferida exige valores válidos; evolução antiga permanece legível; atualização concorrente é rejeitada.
- Recibo exige recebimento confirmado e conserva snapshot/reimpressão; mensalista não paga avulso.
- Recorrência e remarcação respeitam capacidade; paciente duplicado não ocupa duas vagas.
- Turmas filtradas mantêm ações individuais, sem mover/cancelar todos implicitamente.

## Task 1 — APIs clínicas (backend; novos arquivos, sem editar arquivos de Task 2)
Files: novos FotoClinicaEntity/Repository/Service/Controller/DTO e EvolucaoClinicaEntity/Repository/Service/Controller/DTO; migrations V10__fotos_clinicas.sql e V11__evolucoes_clinicas.sql. Usar pacotes existentes e incluir testes.
Interfaces:
- GET /api/v1/pacientes/{pacienteId}/fotos -> FotoClinicaDTO[] {id,pacienteId,dataCaptura,regiao,observacao,criadoEm,autor}.
- POST mesmo path multipart imagem (file), dataCaptura (yyyy-MM-dd), regiao, observacao -> DTO. DELETE /{fotoId}; GET /{fotoId}/imagem -> JPEG/PNG bytes autenticados, Cache-Control no-store; verificar pacienteId.
- GET /api/v1/agendamentos/{id}/evolucao-estruturada -> {versao:number,procedimentos:string[],equipamentos:string[],pressaoSistolica:number|null,pressaoDiastolica:number|null,dor:number|null,observacoes:string,resumo:string,autor:string,atualizadoEm:string}; vazio versao=0.
- PUT mesmo path recebe {versao,procedimentos,equipamentos,pressaoSistolica,pressaoDiastolica,dor,observacoes}; salva nova revisão, resumo determinístico e texto Atendimento.evolucao sem perder legado (primeira revisão incorpora texto antigo se necessário). GET /historico -> revisões. Pressão usa procedimento PRESSAO_ARTERIAL; códigos restantes catálogo acordado frontend: ALONGAMENTO,FORTALECIMENTO,MOBILIZACAO,TERAPIA_MANUAL,EXERCICIO_RESPIRATORIO,PILATES; equipamentos REFORMER,CADILLAC,CHAIR,BARREL,SOLO,ACESSORIOS. Limites arrays/texto e autorização padrão clínica autenticada; autoria pelo principal.
- [ ] Testes RED para associação paciente/foto, imagem inválida/tamanho, pressão condicional, resumo e conflito de versão.
- [ ] Implementar e passar testes, registrar evidências em /workspace/task-clinica-report.md.

## Task 2 — Financeiro e capacidade (backend; dono dos arquivos comuns Atendimento e AgendamentoController)
Files: modelos/entidades/DTOs/mappers Atendimento e ServicoConfig, serviços agendamento/atualização/cobrança, novos Recibo*; V12__cobertura_capacidade_recibos.sql. Nunca editar migrations V10/V11.
Interfaces:
- AtendimentoResponseDTO acrescenta dataHoraFim,statusPagamento,assinaturaId:number|null,modalidadeCobranca:'AVULSO'|'MENSALIDADE'|'LEGADO',dataPagamento:string|null. Campos antigos preservados.
- ServicoConfig DTO CRUD acrescenta capacidade:number (1..100, defaults fisio=1/Pilates=15); duração existente preservada. Front passa capacidade configurada.
- Criar atendimento vincula assinatura vigente na data; valor zero avulso não implica plano. Legado ambíguo marcado LEGADO e tratado conservadoramente. Mensalidade exibe cobertura explicitamente.
- Atualização atendimento aceita dataPagamento yyyy-MM-dd, exige ao confirmar pagamento novo avulso. Revisar conclusão em lote para data atual explícita/documentada. Evitar receita duplicada.
- POST /api/v1/recibos {atendimentoId?:number,cobrancaId?:number,dataPagamento:string,pagadorNome:string,pagadorDocumento:string} exatamente uma origem -> {id,numero,...}; aceita apenas pago e valor positivo, guarda snapshot emitente por configuração, paciente/pagador e pagamento, reemissão idempotente. GET /api/v1/recibos/{id}/pdf; GET /api/v1/recibos?atendimentoId= OU cobrancaId= -> DTO[]. Não apresentar como Receita Saúde.
- Validação de vagas backend única criação/remarcação, overlap cancelados excluídos, proibir duplicata paciente, concorrência serializada por lock transacional apropriado; corrigir contagem countPacientes em lote e mensagem 5 vs 15. Operação múltiplos pacientes não deve parcialmente criar antes de erro sem informar.
- Mensalidade genérica existente deve respeitar vigência, gerador não gera competência anterior ao início; manter unicidade.
- [ ] Testes RED cobertura/cortesia, capacidade/remarcação/duplicado, recibo pendente/pago/reimpressão/snapshot, competência.
- [ ] Implementar e passar testes, relatório /workspace/task-financeiro-report.md.

## Task 3 — Componentes clínicos (frontend; dono pacientes + novos componentes, não agenda)
Files: features/pacientes/pacientes.component.*, novos shared/components/evolucao-ficha/*, features/pacientes/fotos-paciente/*, core/services/clinica.service.ts e interfaces/clinica.interface.ts.
Interfaces: APIs Task1. Componente standalone selector app-evolucao-ficha inputs atendimentoId:number e output salva:string (resumo). Renderiza ficha e botão salvar próprios, on changes recarrega; template acessível, erros/carregamento, histórico. Task4 incorpora ao modal agenda. Galeria paciente abre por ação em pacientes, upload, duas seleções comparação, exclusão confirmada, blobs autenticados e revokeObjectURL no teardown.
- [ ] Testes RED pressão condicional/reset paciente, DTO selecionado, estado de erro e comparação.
- [ ] Implementar, ng build e testes; relatório /workspace/task-ui-clinica-report.md.

## Task 4 — Integração agenda e financeiro (frontend; não editar pacientes/componentes Task3)
Files: agenda.component.*, novo agenda-group.util.ts, agendamento interfaces/service, serviços CRUD, financeiro assinaturas/cobranças, novo recibo dialog/service/interface. Interfaces Tasks2/3.
- Agrupar por serviço+início+fim, com lista de participantes e ações individuais. Filtrar indivíduos antes de agrupar; não arrastar turma inteira. Evento individual usa fim explícito.
- Importar app-evolucao-ficha no modal edição agenda; salva event atualiza evolucaoEdit. Manter texto legado opcional; botão concluir usa resumo salvo. Capacidade editável serviço.
- Mensalidade identificada explicitamente por assinaturaId/modalidade; ação de nova mensalidade acessível e rotulada fisioterapia/Pilates, explicar sem franquia; usar serviços existentes.
- Data de recebimento no atendimento; emitir/baixar/reimprimir recibo para avulso pago e mensalidade paga. Recibo interno identificado com orientação contextual Receita Saúde.
- [ ] Testes RED grupos compatíveis/status mistos/filter; classificação cortesia; download de recibo usa Blob.
- [ ] Implementar build/testes; relatório /workspace/task-ui-agenda-report.md.

## Task 5 — Integração, revisão e entrega
- [ ] Executar Maven test/verify conforme infraestrutura e npm test -- --watch=false + npm run build; corrigir erros reais.
- [ ] Revisão independente dos dois diffs, incluindo migrations, DTOs, autorização e interface; corrigir achados.
- [ ] Documentar configuração de emitente, armazenamento, limites e migração/homologação; criar commits separados por repo. Não deploy nem merge.

## Execution ledger
Ruling: A autorização de implementar abrange a proposta já apresentada; seguir sem novo ciclo de aprovação de plano.
Ruling: armazenamento bytea privado limita custo de infraestrutura e permite entregar upload funcional sem provisionamento; migrável para objetos se volume crescer.
Ruling: módulos independentes compartilham só os contratos acima. Task1 não edita Atendimento/Controller comuns; usa repository existente para resumo. Task2 os possui. Task3 não edita agenda; Task4 integra selector. Revisão conjunta ao terminar.
Preflight: Task1/2 compartilham Atendimento por repository existente, sem edição conjunta; Task1/3 contratos clínicos idênticos; Task2/4 campos financeiros/capacidade idênticos; Task3/4 selector e output especificados. Todas tasks possuem testes correspondentes às regras. Sem instruções AGENTS.md encontradas nos clones.
