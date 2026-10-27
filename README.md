# Abertura de Atendimentos — Desafio Full Stack

Aplicação para abertura de atendimentos de uma clínica. O paciente informa nome e CPF, o backend registra o atendimento e um processo periódico obtém o protocolo em um serviço externo ([httpbin.org/uuid](https://httpbin.org/uuid)). O frontend acompanha o status até o protocolo ficar disponível.

| Camada | Tecnologias |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Spring Data JPA, Flyway, Bean Validation |
| Banco | MySQL 8.4 (Docker) |
| Frontend | Next.js 16, React 19, TypeScript, Tailwind CSS 4, TanStack Query, React Hook Form, Zod |
| Testes | Backend: JUnit 5, Mockito, MockMvc, Testcontainers, Awaitility · Frontend: Vitest, Testing Library |

## Como rodar

Pré-requisitos: **Java 25**, **Node 24** e **Docker** em execução.

**Backend**

```bash
cd backend
./mvnw spring-boot:run
```

O Spring Boot sobe o MySQL automaticamente a partir do `compose.yaml` e o Flyway cria a tabela. A API fica em `http://localhost:8080`.

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

A aplicação fica em `http://localhost:3000`. Para apontar para outra API, copie `.env.example` para `.env.local` e ajuste `NEXT_PUBLIC_API_URL`.

**Testes**

```bash
cd backend && ./mvnw test
cd frontend && npm test
```

Os testes de integração do backend sobem um MySQL descartável via Testcontainers.

## Fluxo

```
Frontend                Backend (API)                    Job (a cada 5s)                httpbin
   │  POST nome+CPF          │                                 │                            │
   │────────────────────────►│ valida, grava PENDING           │                            │
   │◄──── 201 {id, PENDING} ─│                                 │                            │
   │                         │                                 │ busca PENDING prontos      │
   │  GET /{id} (polling)    │                                 │ claim atômico → PROCESSING │
   │────────────────────────►│                                 │ (virtual thread por item)  │
   │◄──── PROCESSING ────────│                                 │───────── GET /uuid ───────►│
   │                         │                                 │◄──────── {uuid} ───────────│
   │                         │                                 │ grava protocolo, COMPLETED │
   │  GET /{id}              │                                 │                            │
   │────────────────────────►│                                 │                            │
   │◄── COMPLETED + protocolo│                                 │                            │
```

Status: `PENDING → PROCESSING → COMPLETED`. Em caso de falha o atendimento volta para `PENDING` com nova tentativa agendada (backoff exponencial) e só vira `FAILED` após o número máximo de tentativas.

## API

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/appointments` | Abre um atendimento. Body: `{"patientName": "Ana Souza", "patientCpf": "529.982.247-25"}`. Retorna `201` + header `Location`. |
| `GET` | `/api/appointments/{id}` | Consulta um atendimento (usado no polling). |
| `GET` | `/api/appointments?ids=1,2,3` | Consulta vários atendimentos de uma vez (até 50). |

Erros seguem o formato [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457):

| Status | Quando |
|---|---|
| `400` | Dados inválidos. O campo `errors` traz a mensagem por campo. |
| `404` | Atendimento não encontrado. |
| `409` | O CPF já possui atendimento em andamento. O campo `activeAppointmentId` informa qual. |

Exemplo de resposta:

```json
{
  "id": 1,
  "patientName": "Ana Souza",
  "patientCpf": "52998224725",
  "status": "COMPLETED",
  "protocol": "4f1c7a2e-1111-4a5b-9c3d-0e2f6b7a8c9d",
  "attemptCount": 0,
  "nextAttemptAt": null,
  "createdAt": "2026-10-26T10:00:00",
  "updatedAt": "2026-10-26T10:00:05"
}
```

## Decisões técnicas

- **Abertura desacoplada do serviço externo.** O `POST` apenas grava o atendimento como `PENDING` e responde imediatamente. Quem chama o httpbin é o job, então lentidão ou falha externa nunca afeta a abertura.
- **Claim atômico.** Antes de processar, o job executa `UPDATE ... SET status = 'PROCESSING' WHERE id = ? AND status = 'PENDING'` e só segue se uma linha foi alterada. Isso garante que um atendimento nunca é processado duas vezes, mesmo com múltiplas instâncias da aplicação.
- **Processamento paralelo com virtual threads.** O job não espera cada chamada terminar: cada atendimento é processado em sua própria virtual thread. Um atendimento lento não bloqueia os demais, e o timeout garante que nenhuma chamada fique presa.
- **Timeouts no cliente HTTP.** Conexão (2s) e leitura (5s) com limite, para nenhuma chamada ficar presa indefinidamente.
- **Nenhuma transação aberta durante a chamada HTTP.** A chamada externa acontece fora de transação; o resultado é gravado depois, em uma transação curta.
- **Falhas nunca perdem o registro.** Erros incrementam `attemptCount`, guardam `lastError` e agendam `nextAttemptAt` com backoff exponencial (5s, 10s, 20s...). Após `max-attempts` o atendimento vira `FAILED`, mas continua no banco.
- **Recuperação de atendimentos travados.** Se a aplicação cair no meio de um processamento, o job devolve para `PENDING` os atendimentos em `PROCESSING` há mais de `stuck-timeout`.
- **Regras de negócio na entidade.** `Appointment` concentra as transições de status (`complete`, `registerFailure`), impedindo estados inválidos.
- **Novo atendimento após concluir.** Um CPF só pode ter um atendimento ativo (`PENDING`/`PROCESSING`). Após `COMPLETED` ou `FAILED`, um novo pode ser aberto.
- **Schema versionado com Flyway** e `ddl-auto: validate`: o Hibernate apenas valida o schema, nunca o altera.

## Frontend

Uma única página no estilo de sistema de recepção: formulário no topo, resumo com a contagem por status e a tabela dos atendimentos em acompanhamento (vira lista no celular), com indicação de atualização automática.

- **Validação no cliente** com Zod: nome obrigatório (3 a 150 caracteres) e CPF com máscara e dígito verificador. Erros `400` do backend também são exibidos no campo correspondente.
- **Polling com TanStack Query**: uma única requisição `GET /api/appointments?ids=...` atualiza todos os atendimentos a cada 2 segundos e para sozinha quando nenhum está `PENDING` ou `PROCESSING`. Se a conexão cair, os últimos dados continuam na tela com um aviso de reconexão.
- **Status de cada atendimento**: aguardando, processando, concluído (com o protocolo e opção de copiar) e falhou. Falhas temporárias mostram o número da próxima tentativa. A cor é usada apenas para comunicar o status.
- **Vários pacientes ao mesmo tempo**: os ids acompanhados ficam no `localStorage`, então a lista sobrevive a um recarregamento da página.
- **Novo atendimento**: atendimentos concluídos ou com falha oferecem "Novo atendimento", que preenche o formulário com o mesmo paciente. Se o CPF já tiver um atendimento ativo (`409`), ele é destacado na lista.

```
frontend/src/
├── app/          layout, página e provider do TanStack Query
├── components/   formulário, resumo por status, tabela, linha do atendimento e status
├── hooks/        polling dos atendimentos e ids no localStorage
├── lib/          cliente da API e utilitários de CPF
└── types/        tipos da API
```

## Configuração

Todos os parâmetros ficam em `backend/src/main/resources/application.yaml`:

```yaml
appointment:
  frontend-url: http://localhost:3000
  protocol:
    base-url: https://httpbin.org
    path: /uuid
    connect-timeout: 2s
    read-timeout: 5s
  processing:
    max-attempts: 5
    initial-backoff: 5s
    batch-size: 20
    stuck-timeout: 2m
  job:
    interval: 5s
```

### Simulando falhas do serviço externo

Basta trocar `appointment.protocol.path` (ou passar como argumento) e reiniciar:

| `path` | Comportamento |
|---|---|
| `/uuid` | Sucesso |
| `/status/500` | Erro do servidor: tentativas incrementam até `FAILED` |
| `/delay/10` | Resposta lenta: estoura o timeout de leitura e entra em retry |

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--appointment.protocol.path=/status/500
```

## Limitações conhecidas

- A verificação de atendimento ativo por CPF é feita na aplicação. Duas requisições simultâneas exatamente no mesmo instante para o mesmo CPF poderiam passar pela checagem; uma garantia absoluta exigiria uma constraint específica no banco.
- O polling do frontend é simples e eficiente para o volume do desafio; em escala maior, Server-Sent Events ou WebSocket evitariam requisições repetidas.
