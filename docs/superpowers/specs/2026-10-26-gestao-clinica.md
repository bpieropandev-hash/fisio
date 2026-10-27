# Avaliação preliminar — gestão de fisioterapia
Data: 26/10/2026. Escopo: análise estática de código e pesquisa de referências. Não houve execução da aplicação, consulta a pacientes reais, alterações de código ou validação do ambiente em produção. Propostas ainda dependem de definições de negócio; este documento não é uma especificação aprovada.

## Base analisada
- Backend: fisio, commit 0d5cca11c1f9b8bb5000ef4041a07c40a61c6304; Java 21, Spring Boot 3.5.16, JPA, PostgreSQL, Flyway, OpenPDF ([pom.xml](https://github.com/bpieropandev-hash/fisio/blob/0d5cca11c1f9b8bb5000ef4041a07c40a61c6304/pom.xml)).
- Frontend: front-fisio, commit 2b60076d621d6a78ba65a786f9ccee18a1301a80; Angular 21, PrimeNG e FullCalendar ([package.json](https://github.com/bpieropandev-hash/front-fisio/blob/2b60076d621d6a78ba65a786f9ccee18a1301a80/package.json)).
- O domínio separa serviços de aplicação, modelos, portas e adaptadores. As melhorias cabem nessa estrutura; não há justificativa identificada para reescrever o sistema.

## Evidências do estado atual
| Área | Evidência | Consequência |
|---|---|---|
| Paciente | [Paciente.java](https://github.com/bpieropandev-hash/fisio/blob/0d5cca11c1f9b8bb5000ef4041a07c40a61c6304/src/main/java/com/physio/domain/model/Paciente.java) contém cadastro e anamnese, sem coleção de fotos | Galeria clínica precisa de armazenamento e metadados próprios |
| Evolução | [Atendimento.java](https://github.com/bpieropandev-hash/fisio/blob/0d5cca11c1f9b8bb5000ef4041a07c40a61c6304/src/main/java/com/physio/domain/model/Atendimento.java) contém String evolucao; [atualização](https://github.com/bpieropandev-hash/fisio/blob/0d5cca11c1f9b8bb5000ef4041a07c40a61c6304/src/main/java/com/physio/application/service/AtualizarAtendimentoService.java) substitui o texto | Evolução estruturada e histórico de revisões exigem extensão do modelo |
| Mensalidade | [CriarAssinaturaService.java](https://github.com/bpieropandev-hash/fisio/blob/0d5cca11c1f9b8bb5000ef4041a07c40a61c6304/src/main/java/com/physio/application/service/CriarAssinaturaService.java) aceita serviço ativo sem restringir ao Pilates; [tela](https://github.com/bpieropandev-hash/front-fisio/blob/2b60076d621d6a78ba65a786f9ccee18a1301a80/src/app/features/financeiro/assinaturas/assinaturas.component.ts) carrega serviços ativos | Já existe base para mensalidade de fisioterapia; validar uso e regras faltantes antes de duplicar funcionalidade |
| Cobranças | [GeradorCobrancaService.java](https://github.com/bpieropandev-hash/fisio/blob/0d5cca11c1f9b8bb5000ef4041a07c40a61c6304/src/main/java/com/physio/application/service/GeradorCobrancaService.java) gera cobranças por assinatura/mês/ano, com proteção contra duplicação | Reutilizar motor e revisar vigência, proporcionalidade e competência |
| Recibos | [RelatorioPDFService.java](https://github.com/bpieropandev-hash/fisio/blob/0d5cca11c1f9b8bb5000ef4041a07c40a61c6304/src/main/java/com/physio/application/service/RelatorioPDFService.java) gera relatórios financeiros e de evolução; não foi localizado módulo dedicado de recibos nos arquivos inspecionados | Reaproveitar biblioteca de PDF, acrescentando registro próprio de pagamento e documento |
| Turmas | [RealizarAgendamentoService.java](https://github.com/bpieropandev-hash/fisio/blob/0d5cca11c1f9b8bb5000ef4041a07c40a61c6304/src/main/java/com/physio/application/service/RealizarAgendamentoService.java) limita fisioterapia a 1 e Pilates a 15; mensagem de Pilates diz 5 | Capacidade precisa de regra única configurável; não basta agrupar visualmente |
| Agenda | [agenda.component.ts](https://github.com/bpieropandev-hash/front-fisio/blob/2b60076d621d6a78ba65a786f9ccee18a1301a80/src/app/features/agenda/agenda.component.ts) cria um evento por atendimento e possui conclusão em lote | Reaproveitar ações individuais e lote, acrescentando agrupamento |

## Recomendações para as cinco melhorias

### 1. Fotos e comparação antes/depois
Criar galeria na ficha individual do paciente, acessível pela tela de pacientes. Separar foto de identificação das imagens clínicas. Registrar data da captura, região corporal, posição/ângulo, observação, profissional e atendimento opcional. Permitir selecionar duas datas e comparar lado a lado, mantendo originais.

Proposta técnica: entidade FotoClinica com pacienteId, atendimentoId opcional, chave do objeto, tipo/tamanho, dataCaptura e autoria. Guardar arquivos em armazenamento privado; banco guarda metadados. Autorizar o acesso por paciente e perfil, usar links temporários, miniaturas e limites de upload. Não enviar todas as imagens junto com a listagem de pacientes. Registrar a finalidade e a autorização aplicável; autorização de divulgação é distinta do uso no prontuário.

Primeira entrega: upload, galeria, comparação e identificação de autoria. Posteriormente: marcações sobre cópia da imagem e relatórios comparativos. Validação: outro usuário sem permissão não acessa nem por URL; comparação preserva datas/originais; upload inválido é rejeitado.

### 2. Evolução dinâmica
Definição confirmada pelo usuário: o profissional deve marcar o que foi feito em vez de escrever tudo. Exemplos fornecidos: aferição de pressão e seleção de equipamentos utilizados no Pilates.

Proposta inicial: ficha por serviço com seleção múltipla de procedimentos, equipamentos e exercícios, campos condicionais e observação opcional. Marcar “pressão aferida” abre pressão sistólica/diastólica em mmHg, momento e observação; diferenciar não aferida de aferida sem valor informado. Para Pilates, sugerir catálogo editável de Reformer, Cadillac, Chair, Barrel, solo e acessórios; seleção de equipamento pode abrir exercícios, séries/repetições e resistência quando relevantes. Esses itens são exemplos propostos, a validar com a profissional. Dor e outras medidas entram somente nos modelos em que forem úteis. Não pré-marcar procedimentos como realizados nem copiar seleção de sessão anterior automaticamente. Permitir gerar resumo textual determinístico a partir do que foi preenchido e revisá-lo antes da finalização, mantendo as respostas estruturadas como fonte. Rascunho, autoria e linha do tempo completam o fluxo. Gráficos usam medidas comparáveis com unidade, escala e contexto preservados.

Proposta técnica: EvolucaoClinica vinculada ao atendimento, versão do modelo, respostas estruturadas, resumo, autor e datas. Histórico de alterações e finalização; correção após finalização gera revisão/adendo rastreável. Preservar evolucao legado, sem tentar inferir automaticamente métricas de texto histórico. Começar com poucos modelos definidos, deixando um construtor completo de formulários para depois.

Validação: texto antigo continua legível; edição concorrente não sobrescreve silenciosamente; alterações em um modelo não mudam evoluções já finalizadas; uma evolução não aparece no prontuário de outro participante da turma.

### 3. Recibo de pagamento
Disponibilizar emissão no financeiro e na ficha do paciente após registrar recebimento. Incluir identificador, emitente, paciente, pagador quando diferente, serviço/competência, valor efetivamente recebido, data e forma de pagamento. Reimpressão usa o mesmo documento; cancelamento/estorno mantém rastreabilidade.

Proposta técnica: Pagamento e Recibo independentes da apresentação do PDF; recibo guarda snapshot dos dados no momento da emissão. Atendimento avulso e cobrança mensal apontam para o pagamento correspondente. Pagamento parcial, se incluído no escopo, gera recibo do valor recebido e mantém saldo em aberto. Evitar emitir documento de quitação só porque o atendimento foi concluído.

No código atual, avulso passa a PAID pela presença de recebedor e forma de pagamento; não há data de recebimento no modelo Atendimento. A emissão confiável exige preencher essa lacuna. Reutilizar OpenPDF.

Fiscal: verificar PF/PJ e enquadramento. Para fisioterapeuta pessoa física abrangido pela regra, Receita Saúde não é substituído pelo PDF interno. Não pressupor API de integração. A nota de pesquisa contém a fonte oficial.

### 4. Mensalidade de fisioterapia
Reutilizar Assinatura e CobrancaMensal. Oferecer escolha clara entre sessão avulsa e plano mensal, com serviço, valor, vencimento, vigência e condições.

Decisão necessária: valor mensal cobre quantidade fixa de sessões, frequência semanal ou uso ilimitado? Também definir faltas, reposição, feriados, entrada no meio do mês, pausa, cancelamento, excedentes e reajuste. Não assumir que 2 vezes por semana equivale sempre a 8 sessões mensais.

Proposta técnica: modalidade de cobrança explícita e vínculo da sessão à assinatura/competência; se houver franquia, registro de consumo por sessão. Hoje valorCobrado == 0 identifica mensalista na atualização e agenda: isso confunde cortesia com cobertura contratual. A sessão deve mostrar “coberta pelo plano”; o débito pertence à cobrança mensal.

Revisar vigência na data agendada, pois o fluxo consultado busca assinatura ativa sem passar a data do atendimento. O gerador consultado percorre assinaturas ativas, sem regra temporal explícita no serviço. Validar comportamento integrado antes de declarar defeito em produção.

Validação: cobrança única por competência; sessão coberta não gera cobrança avulsa; cancelamento não altera documentos pagos; início futuro não cobre sessões anteriores; relatórios não contam receita duas vezes.

### 5. Mais de um paciente no horário: turma
Primeira entrega recomendada: um cartão de turma para atendimentos compatíveis, com quantidade de participantes e abertura de lista individual. Preservar presença, falta, cancelamento, pagamento e evolução de cada paciente. Mesma hora isoladamente não define a mesma turma: considerar serviço, início/fim e, quando existentes, profissional e sala.

Para fisioterapia coletiva, tornar capacidade configurável e validar tanto criação como remarcação no backend. A atualização inspecionada muda horários sem repetir a validação de capacidade do agendamento; esse caminho precisa ser coberto. Impedir paciente duplicado e excesso de lotação inclusive com duas requisições simultâneas.

Se houver matrícula fixa, vagas, lista de espera e recorrência própria, criar Turma, SessaoTurma e Participacao explicitamente. Não adicionar essa complexidade apenas para resolver sobreposição visual.

Validação: falta de um participante não cancela os demais; filtro por paciente mantém acesso ao seu atendimento; mover um participante não move todos sem escolha explícita; turmas simultâneas em locais distintos não são misturadas.

## Alternativas
1. Alterações pontuais nas telas: menor esforço inicial, porém preserva ambiguidades financeiras e limita prontuário/turmas.
2. Evolução incremental dos módulos atuais — recomendada: manter stack, acrescentar entidades clínicas/financeiras necessárias e entregar em etapas.
3. Reestruturação ampla: custo maior e risco de migração; não sustentada pelas cinco necessidades apresentadas.

A alternativa 2 é recomendação técnica, ainda não uma escolha aprovada pelo usuário.

## Ordem sugerida e esforço relativo
1. Confirmar regras de cobrança e turmas, separar cobertura de pagamento e uniformizar capacidade: fundação.
2. Mensalidade de fisioterapia e recibos: esforço médio; maior se incluir gateway, pagamentos parciais ou integração fiscal.
3. Turmas na agenda: médio para agrupamento; alto para matrícula/recorrência/lista de espera.
4. Evolução estruturada: médio-alto; cresce bastante com construtor genérico e assinatura digital.
5. Fotos comparativas: médio, condicionado à disponibilidade de armazenamento privado.

Fotos podem antecipar-se conforme prioridade clínica. As faixas não são estimativas em dias: faltam regras acordadas, execução local e validação do ambiente.

## Estratégia de aplicação
Adicionar tabelas/colunas via Flyway, preservar histórico e manter contratos antigos durante transição do frontend. Introduzir os novos vínculos financeiros sem classificar automaticamente todo valor zero como mensalidade. Extrair formulários/diálogos novos do componente de agenda para conter seu crescimento. Autorizar imagens, prontuários e documentos no backend.

Verificar fluxos reais em homologação: paciente avulso, mensalista, cortesia, falta, cancelamento, remarcação e turma com estados mistos. Para migrações, fazer backup e testar restauração; rollback de interface não deve apagar dados novos. Testes prioritários são regras financeiras, acesso ao prontuário/imagens, concorrência de vagas e preservação do legado.

## Ideias adicionais para priorizar depois
- Resumo do paciente com próxima sessão, objetivo terapêutico, evolução e situação financeira.
- Alertas de evolução pendente, reavaliação e vencimento.
- Lembretes e confirmação de presença; lista de espera para preencher cancelamentos.
- Plano terapêutico e exercícios domiciliares.
- Indicadores de faltas, ocupação, adesão, receita recebida e inadimplência separados.
- Perfis de recepção, fisioterapeuta e gestor, com trilha de alterações.

Comparação de mercado e fontes verificadas: ver pesquisa-gestao-clinica.md.

