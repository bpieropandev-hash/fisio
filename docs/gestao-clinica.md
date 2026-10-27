# Gestão clínica: configuração e homologação

Esta entrega acrescenta fotos clínicas, evolução por marcação, capacidade de atendimento coletivo, cobertura mensal explícita e recibos internos. Deve ser instalada junto com a branch correspondente de `front-fisio`.

## Configuração

- Java 21 e PostgreSQL, conforme a aplicação existente. Flyway aplica V10–V12; não executar os scripts manualmente junto com Flyway.
- `RECIBO_EMITENTE_NOME` e `RECIBO_EMITENTE_DOCUMENTO`: identidade de quem recebe o pagamento. Preencher com o emitente correto antes de emitir recibos. O documento é um recibo interno e não substitui Receita Saúde ou nota fiscal exigível.
- Fotos são armazenadas em `fotos_clinicas.conteudo` como `bytea`. Limite de arquivo 5 MiB, JPEG/PNG efetivamente decodificável e até 20 milhões de pixels. Metadados incluem data da captura, região, observação e autor. Não há URL pública; a imagem é servida pela API autenticada e sem cache HTTP.
- Backups do PostgreSQL passam a incluir as imagens e devem considerar crescimento de volume. Não há novo bucket ou serviço externo nesta entrega.
- A aplicação continua com o modelo de autorização existente: usuários autenticados da mesma clínica. Esta entrega não introduz isolamento entre clínicas nem perfis de recepção/profissional.

## Fluxos

### Fotos

Na tela de pacientes, abrir **Fotos**, enviar imagem com data e região e selecionar duas imagens para comparação. A galeria lista metadados separadamente do conteúdo. Excluir exige confirmação na tela. Fotos cadastrais de usuário continuam independentes das fotos clínicas.

### Evolução

Abrir o atendimento e marcar procedimentos/equipamentos efetivamente realizados. **Pressão aferida** exige sistólica/diastólica em mmHg; dor é opcional. **Salvar evolução** registra autor, revisão e resumo. **Ver histórico** mantém as revisões anteriores. O texto legado é preservado na primeira ficha.

O atendimento não pode ser salvo ou fechado silenciosamente enquanto a ficha está pendente. Complemento digitado pode ser levado explicitamente para Observações; salvar a ficha não o apaga. Conflitos de versão exigem recarregar e revisar; nunca substituir automaticamente a edição de outro profissional.

### Mensalidade e recibos

Criar mensalidade em **Financeiro → Assinaturas**, escolhendo serviço de fisioterapia ou Pilates, valor, início e vencimento. Esta versão mantém mensalidade sem franquia de sessões; não inclui gateway, débito automático, reposições ou controle de créditos.

Novos atendimentos cobertos têm vínculo explícito à assinatura. Não classificar cortesia como mensalista só por ter valor zero. Registros antigos ambíguos devem ser revisados antes de qualquer conversão em massa.

Recibos são emitidos para recebimentos confirmados com valor positivo e data de pagamento. Reabrir uma origem já emitida recupera o mesmo documento e seus dados históricos. O pagamento de mensalista deve gerar recibo pela cobrança mensal, sem repetir receita por sessão. Regularizar datas de pagamentos antigos antes de emitir novos documentos; não preencher datas presumidas.

### Turmas

Configurar **Capacidade** no serviço. Defaults preservam fisioterapia individual (1) e Pilates (15); aumentar a capacidade de fisioterapia permite atendimento coletivo. A agenda agrupa serviço e intervalo iguais, mostrando os participantes. Abrir/cancelar/evoluir um participante afeta só ele. Mover a turma inteira por arraste está desabilitado.

Não há cadastro de turmas fixas, matrícula, salas ou profissionais nesta entrega. Esses conceitos exigirão chaves de agrupamento adicionais ao serem introduzidos.

## Homologação antes de produção

1. Fazer backup do banco e testar restauração em ambiente isolado. Aplicar as migrações nesse banco e confirmar validação Hibernate.
2. Publicar a API compatível antes da nova interface, mantendo os registros históricos. A migração é aditiva; rollback de interface não deve excluir tabelas nem revisões novas.
3. Usar pacientes fictícios para testar upload/comparação, alteração de paciente durante carregamento, evolução antiga e nova, conflito entre duas abas, mensalidade e cortesia, recibo e reimpressão, turma cheia e remarcação.
4. Confirmar emitente, valores, competência e documento fiscal aplicável com a clínica.
5. Monitorar erros HTTP, falhas de upload, tamanho do banco e ocupação da agenda. Não registrar imagem, token ou conteúdo clínico em logs operacionais.

## Verificações automatizadas

- Backend: `./mvnw test` e `./mvnw verify` (integração PostgreSQL via Docker/Testcontainers).
- Frontend: `npm ci`, `npm test -- --watch=false`, `npm run build`.
- `ApiGestaoClinicaIT` exercita autenticação JWT, migrações, banco real e contratos clínicos/financeiros por HTTP. Testes unitários cobrem regras e rejeições. A validação visual da interface usa dados simulados, distinta dos testes de API reais.
