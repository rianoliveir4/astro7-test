# Abertura de Atendimentos

Solução para o desafio full stack da Astro. A recepção informa nome e CPF do paciente, o backend registra o atendimento e um job busca o protocolo no httpbin em segundo plano. O frontend acompanha o status até o protocolo ser gerado.

**Stack:** Java 25 + Spring Boot 4.1, MySQL 8.4, Next.js 16 + React 19 + TypeScript + Tailwind CSS.

## Como rodar

Pré-requisitos: **Java 25**, **Node 24** e **Docker** em execução.

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

O Spring Boot sobe o MySQL a partir do `compose.yaml` e o Flyway aplica as migrations. A API roda em `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

O frontend roda em `http://localhost:3000`. A URL da API pode ser alterada em `NEXT_PUBLIC_API_URL` (ver `.env.example`).

### Testes

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

Os testes de integração do backend usam Testcontainers, então o Docker precisa estar em execução.

## Fluxo

1. O frontend valida nome e CPF e envia o `POST`.
2. O backend salva o atendimento como `PENDING` e responde imediatamente.
3. A cada 5 segundos, um job busca os atendimentos pendentes e chama o `https://httpbin.org/uuid` para cada um.
4. Em caso de sucesso, o UUID é salvo como protocolo e o status passa para `COMPLETED`. Em caso de erro, uma nova tentativa é agendada.
5. O frontend consulta o backend a cada 2 segundos e atualiza a tela quando o protocolo fica disponível.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/appointments` | Abre um atendimento (`{"patientName", "patientCpf"}`) |
| `GET` | `/api/appointments/{id}` | Consulta um atendimento |
| `GET` | `/api/appointments?ids=1,2,3` | Consulta vários atendimentos (usado pelo polling) |

Os erros seguem o padrão Problem Details: `400` com a mensagem de cada campo, `404` para atendimento inexistente e `409` quando o CPF já tem um atendimento em andamento.

## Decisões técnicas

### Backend

**Processamento assíncrono com job agendado.** A geração do protocolo fica em um job com `@Scheduled`, separado da abertura. O `POST` só grava o atendimento e responde, então lentidão ou indisponibilidade do httpbin não afeta a recepção.

**Claim atômico com UPDATE condicional.** Antes de processar, o job executa `UPDATE ... SET status = 'PROCESSING' WHERE id = ? AND status = 'PENDING'` e só segue se a linha foi alterada. Com isso um atendimento nunca é processado duas vezes, mesmo com mais de uma instância da aplicação rodando.

**Processamento paralelo com virtual threads.** Cada atendimento é processado em uma virtual thread própria, então uma chamada lenta não segura as outras. Optei por virtual threads porque são leves e dispensam configurar e dimensionar um pool. Existe um teste de integração que valida esse cenário com um atendimento propositalmente lento.

**Timeouts no cliente HTTP.** O `RestClient` tem limite de 2s para conexão e 5s para leitura. Sem isso, uma chamada travada poderia prender o processamento indefinidamente.

**Retry com backoff exponencial.** Quando a chamada falha, o atendimento volta para `PENDING` com uma nova tentativa agendada em intervalos crescentes (5s, 10s, 20s...). Após 5 tentativas o status vira `FAILED`, mas o registro continua no banco com o último erro salvo.

**Recuperação de atendimentos travados.** Se a aplicação cair no meio de um processamento, o atendimento ficaria em `PROCESSING` para sempre. O job devolve para `PENDING` os que estão nesse status há mais de 2 minutos.

**Chamada HTTP fora da transação.** O job marca o atendimento como `PROCESSING`, faz a chamada sem transação aberta e grava o resultado em uma transação curta, evitando segurar conexão do banco enquanto espera o serviço externo.

**Transições de status na entidade.** Os métodos `complete` e `registerFailure` ficam na própria `Appointment` e validam o estado atual, impedindo transições inválidas. O service apenas orquestra.

**Arquitetura em camadas** (controller, service, repository, entity, dto, client, scheduler). Para o tamanho do projeto, achei a separação por camada mais direta de navegar do que por feature.

**Migrations com Flyway.** O schema é versionado em migrations e o Hibernate só valida (`ddl-auto: validate`), sem alterar tabelas automaticamente.

**Configuração tipada.** URL do serviço externo, timeouts, tentativas e intervalo do job ficam no `application.yaml`, mapeados em um `record` com Bean Validation. Configuração inválida impede a aplicação de subir.

### Frontend

**Polling com TanStack Query.** O `refetchInterval` consulta o backend a cada 2 segundos e para automaticamente quando não há atendimentos pendentes. Em caso de falha de conexão, os últimos dados continuam na tela com um aviso até a conexão voltar.

**Uma única requisição para todos os atendimentos.** O polling usa `GET ?ids=...` com todos os atendimentos acompanhados, em vez de uma requisição por atendimento.

**Validação com Zod e React Hook Form.** O CPF é validado com máscara e dígito verificador antes do envio. O backend valida novamente, e os erros de campo retornados pela API são exibidos no campo correspondente.

**Acompanhamento persistido no `localStorage`.** Os ids acompanhados ficam salvos no navegador, permitindo acompanhar vários pacientes ao mesmo tempo sem perder a lista ao recarregar a página.

**Novo atendimento após conclusão.** Cada CPF pode ter apenas um atendimento em andamento. Quando o anterior termina, a opção "Novo atendimento" preenche o formulário com o mesmo paciente. Se houver um atendimento ativo, a API retorna `409` e a tela destaca o atendimento existente.

**Componentes próprios com Tailwind**, sem biblioteca de UI, já que a interface tem poucos componentes.

## Simulando falhas

O httpbin tem rotas que simulam erro e lentidão. Para testar o retry, basta trocar o `path` do serviço externo ao subir o backend:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--appointment.protocol.path=/status/500
```

| `path` | Comportamento |
|---|---|
| `/uuid` | Sucesso |
| `/status/500` | Erro: as tentativas aumentam até `FAILED` |
| `/delay/10` | Lentidão: estoura o timeout e entra no retry |

## Melhorias futuras

- A verificação de atendimento ativo por CPF é feita na aplicação. Duas requisições simultâneas para o mesmo CPF poderiam passar pela checagem; uma constraint no banco daria garantia total.
- O polling atende bem esse volume, mas em escala maior eu usaria Server-Sent Events para o backend notificar o frontend em vez de consultas periódicas.
