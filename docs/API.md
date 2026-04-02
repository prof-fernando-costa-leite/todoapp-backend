# TodoApp — Documentação da API

Base URL: `http://localhost:8080/api/v1`

Todos os endpoints (exceto `POST /auth/register` e `POST /auth/login`) exigem o header:

```
Authorization: Bearer <accessToken>
```

---

## Formato de erro padrão

Todos os erros retornam:

```json
{
  "timestamp": "2026-04-01T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Descrição do problema",
  "path": "/api/v1/...",
  "actionId": "uuid-opcional"
}
```

| Status | Significado                              |
|--------|------------------------------------------|
| 400    | Validação de entrada falhou              |
| 401    | Token ausente, inválido ou expirado      |
| 403    | Usuário não tem permissão                |
| 404    | Recurso não encontrado                   |
| 409    | Conflito de negócio (ex: bloqueio ativo) |
| 500    | Erro interno inesperado                  |

---

## Auth

### Registrar usuário
```
POST /auth/register
```

**Body:**
```json
{
  "email": "alice@example.com",
  "displayName": "Alice",
  "password": "MinhaSenh@123"
}
```

**Resposta 201:**
```json
{
  "id": "uuid",
  "email": "alice@example.com",
  "displayName": "Alice",
  "createdAt": "2026-04-01T10:00:00Z"
}
```

---

### Login
```
POST /auth/login
```

**Body:**
```json
{
  "email": "alice@example.com",
  "password": "MinhaSenh@123"
}
```

**Resposta 200:**
```json
{
  "accessToken": "eyJ...",
  "expiresAt": "2026-04-01T12:00:00Z",
  "user": {
    "id": "uuid",
    "email": "alice@example.com",
    "displayName": "Alice",
    "createdAt": "2026-04-01T10:00:00Z"
  }
}
```

> Salve `accessToken` e envie em todas as requisições autenticadas.

---

### Perfil do usuário autenticado
```
GET /auth/me
```

**Resposta 200:** igual ao objeto `user` do login.

---

### Listar usuários para seleção de membros
```
GET /auth/users
```

Retorna todos os usuários cadastrados com payload enxuto para selects/autocomplete no frontend.

**Resposta 200:**
```json
[
  { "id": "uuid-owner", "displayName": "Alice" },
  { "id": "uuid-member", "displayName": "Bob" }
]
```

---

## Groups

Um grupo é uma equipe. Apenas membros do grupo acessam seus boards e tasks.

### Criar grupo
```
POST /groups
```

**Body:**
```json
{
  "name": "Time de Produto",
  "memberIds": ["uuid-usuario-b", "uuid-usuario-c"]
}
```

> O criador vira automaticamente `OWNER`. Os IDs informados em `memberIds` entram como `MEMBER`.

**Resposta 201:** `GroupDetailsResponse` (ver abaixo)

---

### Listar grupos do usuário autenticado
```
GET /groups
```

**Resposta 200:**
```json
[
  {
    "id": "uuid",
    "name": "Time de Produto",
    "createdBy": "uuid",
    "createdAt": "2026-04-01T10:00:00Z"
  }
]
```

---

### Detalhe de um grupo
```
GET /groups/{groupId}
```

**Resposta 200:**
```json
{
  "id": "uuid",
  "name": "Time de Produto",
  "createdBy": "uuid",
  "createdAt": "2026-04-01T10:00:00Z",
  "members": [
    { "userId": "uuid", "displayName": "Alice", "role": "OWNER", "addedAt": "2026-04-01T10:00:00Z" },
    { "userId": "uuid", "displayName": "Bob", "role": "MEMBER", "addedAt": "2026-04-01T10:01:00Z" }
  ]
}
```

---

### Adicionar membro
```
POST /groups/{groupId}/members
```

**Body:**
```json
{ "userId": "uuid-do-usuario" }
```

> Somente o `OWNER` pode adicionar membros.

**Resposta 200:** `GroupDetailsResponse` atualizado.

---

### Remover membro
```
DELETE /groups/{groupId}/members/{userId}
```

> Somente o `OWNER` pode remover. O dono não pode remover a si mesmo.

**Resposta 204** (sem body)

---

## Boards

Um board pertence a um grupo. Ao ser criado, ganha automaticamente 4 statuses padrão (`TODO`, `BLOCKED`, `DOING`, `DONE`) e as transições iniciais entre eles.

### Criar board
```
POST /groups/{groupId}/boards
```

**Body:**
```json
{
  "name": "Sprint 1",
  "description": "Opcional"
}
```

**Resposta 201:** `BoardDetailsResponse` com statuses e transições já criados.

```json
{
  "id": "uuid",
  "groupId": "uuid",
  "name": "Sprint 1",
  "description": null,
  "createdBy": "uuid",
  "createdAt": "2026-04-01T10:00:00Z",
  "members": [
    { "id": "uuid-owner", "userId": "uuid-owner", "displayName": "Alice", "role": "OWNER" },
    { "id": "uuid-member", "userId": "uuid-member", "displayName": "Bob", "role": "MEMBER" }
  ],
  "statuses": [
    {
      "id": "uuid-todo",
      "code": "TODO",
      "name": "To Do",
      "kind": "SYSTEM",
      "rank": 10,
      "initial": true,
      "terminal": false
    },
    {
      "id": "uuid-blocked",
      "code": "BLOCKED",
      "name": "Blocked",
      "kind": "SYSTEM",
      "rank": 15,
      "initial": false,
      "terminal": false
    },
    {
      "id": "uuid-doing",
      "code": "DOING",
      "name": "Doing",
      "kind": "SYSTEM",
      "rank": 20,
      "initial": false,
      "terminal": false
    },
    {
      "id": "uuid-done",
      "code": "DONE",
      "name": "Done",
      "kind": "SYSTEM",
      "rank": 30,
      "initial": false,
      "terminal": true
    }
  ],
  "transitions": [
    { "fromStatusId": "uuid-todo",    "toStatusId": "uuid-doing"    },
    { "fromStatusId": "uuid-todo",    "toStatusId": "uuid-blocked"  },
    { "fromStatusId": "uuid-doing",   "toStatusId": "uuid-todo"     },
    { "fromStatusId": "uuid-doing",   "toStatusId": "uuid-blocked"  },
    { "fromStatusId": "uuid-doing",   "toStatusId": "uuid-done"     },
    { "fromStatusId": "uuid-blocked", "toStatusId": "uuid-todo"     },
    { "fromStatusId": "uuid-blocked", "toStatusId": "uuid-doing"    },
    { "fromStatusId": "uuid-done",    "toStatusId": "uuid-doing"    }
  ]
}
```

---

### Listar boards de um grupo
```
GET /groups/{groupId}/boards
```

**Resposta 200:** lista de `BoardResponse` (sem statuses/transições).

---

### Detalhe de um board
```
GET /boards/{boardId}
```

**Resposta 200:** `BoardDetailsResponse` completo (igual ao retorno do create, incluindo `members`).

---

### Listar statuses de um board
```
GET /boards/{boardId}/statuses
```

**Resposta 200:** lista de `BoardStatusResponse`.

---

### Listar transições de um board
```
GET /boards/{boardId}/transitions
```

**Resposta 200:**
```json
[
  { "fromStatusId": "uuid", "toStatusId": "uuid" }
]
```

---

### Criar status customizado
```
POST /boards/{boardId}/statuses
```

**Body:**
```json
{
  "code": "REVIEW",
  "name": "Em Revisão",
  "rank": 25,
  "initial": false,
  "terminal": false
}
```

> Statuses `SYSTEM` não podem ser removidos. Se `initial: true`, o flag é removido do status anterior automaticamente.

**Resposta 201:** `BoardStatusResponse`

---

### Substituir transições de um board
```
PUT /boards/{boardId}/transitions
```

**Body:**
```json
{
  "transitions": [
    { "fromStatusId": "uuid-todo",  "toStatusId": "uuid-doing"  },
    { "fromStatusId": "uuid-doing", "toStatusId": "uuid-review" },
    { "fromStatusId": "uuid-review","toStatusId": "uuid-done"   }
  ]
}
```

> Substitui **todas** as transições existentes do board de uma vez.

**Resposta 200:** lista atualizada de `BoardTransitionResponse`.

---

### Remover status customizado
```
DELETE /boards/{boardId}/statuses/{statusId}
```

> Falha com `403` se for status `SYSTEM` ou `initial`.  
> Falha com `409` se houver tasks usando o status.

**Resposta 204** (sem body)

---

### Remover board
```
DELETE /boards/{boardId}
```

> Apenas membros do grupo podem remover o board.  
> A remoção é em cascata (statuses, transições, tasks e histórico das tasks do board).

**Resposta 204** (sem body)

---

## Tasks

### Criar task
```
POST /boards/{boardId}/tasks
```

**Body:**
```json
{
  "title": "Implementar tela de login",
  "description": "Opcional, até 2000 caracteres",
  "assigneeId": "uuid-opcional",
  "points": 5,
  "priority": "HIGH",
  "blockerTaskId": "uuid-opcional"
}
```

> `priority`: `LOW` | `MEDIUM` | `HIGH` | `CRITICAL`  
> A task nasce no status `initial` do board.  
> O `assigneeId` e o `blockerTaskId` devem pertencer ao mesmo grupo/board.

**Resposta 201:** `TaskResponse`

```json
{
  "id": "uuid",
  "boardId": "uuid",
  "creatorId": "uuid",
  "creatorDisplayName": "Alice",
  "assigneeId": null,
  "assigneeDisplayName": null,
  "title": "Implementar tela de login",
  "description": null,
  "createdAt": "2026-04-01T10:00:00Z",
  "updatedAt": "2026-04-01T10:00:00Z",
  "status": {
    "id": "uuid-todo",
    "code": "TODO",
    "name": "To Do",
    "rank": 10,
    "terminal": false
  },
  "points": 5,
  "priority": "HIGH",
  "blockerTaskId": null
}
```

---

### Listar tasks de um board
```
GET /boards/{boardId}/tasks
```

**Resposta 200:** lista de `TaskResponse`.

> `TaskResponse` inclui `creatorDisplayName` (sempre preenchido) e `assigneeDisplayName` (nulo quando não há responsável).

---

### Minhas tasks
```
GET /tasks/mine
```

> Retorna tasks onde o usuário autenticado é criador ou responsável.

**Resposta 200:** lista de `TaskResponse`.

---

### Detalhe de uma task
```
GET /tasks/{taskId}
```

**Resposta 200:** `TaskResponse`.

---

### Atualizar dados da task
```
PATCH /tasks/{taskId}
```

**Body** (todos opcionais):
```json
{
  "title": "Novo título",
  "description": "Nova descrição",
  "points": 8,
  "priority": "CRITICAL"
}
```

**Resposta 200:** `TaskResponse` atualizado.

---

### Mudar status da task
```
PATCH /tasks/{taskId}/status
```

**Body:**
```json
{ "statusId": "uuid-do-status-destino" }
```

**Regras de negócio:**
- A transição precisa existir no board (configurada em `PUT /boards/{boardId}/transitions`).
- Se a task tiver um `blockerTaskId` apontando para uma task **não terminal**, ela **não pode avançar** para um status com rank maior (retorna `409`).
- Se a task já estiver no status informado, retorna `400`.

**Resposta 200:** `TaskResponse` atualizado.

---

### Alterar responsável da task
```
PATCH /tasks/{taskId}/assignee
```

**Body:**
```json
{ "assigneeId": "uuid-ou-null" }
```

> Enviar `null` remove o responsável. O responsável deve ser membro do grupo.

**Resposta 200:** `TaskResponse` atualizado.

---

### Alterar task bloqueante
```
PATCH /tasks/{taskId}/blocker
```

**Body:**
```json
{ "blockerTaskId": "uuid-ou-null" }
```

> Enviar `null` remove o bloqueio. A bloqueante deve pertencer ao mesmo board.  
> Uma task não pode bloquear a si mesma.

**Resposta 200:** `TaskResponse` atualizado.

---

### Histórico de status de uma task
```
GET /tasks/{taskId}/history
```

**Resposta 200:**
```json
[
  {
    "id": "uuid",
    "fromStatusId": null,
    "fromStatusName": null,
    "toStatusId": "uuid-todo",
    "toStatusName": "To Do",
    "changedBy": "uuid",
    "changedAt": "2026-04-01T10:00:00Z"
  },
  {
    "id": "uuid",
    "fromStatusId": "uuid-todo",
    "fromStatusName": "To Do",
    "toStatusId": "uuid-doing",
    "toStatusName": "Doing",
    "changedBy": "uuid",
    "changedAt": "2026-04-01T11:00:00Z"
  }
]
```

---

### Remover task
```
DELETE /tasks/{taskId}
```

> Apenas membros do grupo podem remover a task.  
> Se outras tasks apontarem para ela em `blockerTaskId`, o vínculo é removido automaticamente.

**Resposta 204** (sem body)

---

## Fluxos principais

### 1. Cadastro e login
```
POST /auth/register → POST /auth/login → salvar accessToken → todas as demais chamadas
```

### 2. Criar equipe e board
```
POST /groups  (com memberIds dos colegas)
  └─ POST /groups/{groupId}/boards
       └─ board criado com 4 statuses padrão e transições automáticas
```

### 3. Trabalhar com tasks
```
POST /boards/{boardId}/tasks          → task criada em TODO
PATCH /tasks/{taskId}/status          → mover para DOING (verifica bloqueio)
PATCH /tasks/{taskId}/status          → mover para DONE  (verifica bloqueio)
GET  /tasks/{taskId}/history          → ver todo o histórico de status
```

### 4. Bloqueio entre tasks
```
POST /boards/{boardId}/tasks  (task A, sem bloqueio)
POST /boards/{boardId}/tasks  (task B, blockerTaskId = id da task A)

PATCH /tasks/{taskB}/status { statusId: uuid-doing }
  → 409: task B está bloqueada pela task A que está aberta

PATCH /tasks/{taskA}/status { statusId: uuid-done }
  → 200: task A concluída (terminal)

PATCH /tasks/{taskB}/status { statusId: uuid-doing }
  → 200: bloqueio liberado
```

### 5. Gerenciar equipe
```
POST   /groups/{groupId}/members            → adicionar membro (somente OWNER)
DELETE /groups/{groupId}/members/{userId}   → remover membro  (somente OWNER)
```

### 6. Customizar workflow
```
POST /boards/{boardId}/statuses              → criar status REVIEW (rank 25)
PUT  /boards/{boardId}/transitions           → redefinir transições com REVIEW
DELETE /boards/{boardId}/statuses/{statusId} → remover status customizado (se não usado)
```

---

## Resumo de permissões

| Ação                         | Quem pode                        |
|------------------------------|----------------------------------|
| Criar grupo                  | Qualquer usuário autenticado     |
| Ver grupo / board / task     | Membros do grupo                 |
| Adicionar / remover membro   | Somente OWNER do grupo           |
| Criar board                  | Membros do grupo                 |
| Criar / editar task          | Membros do grupo                 |
| Remover task                 | Membros do grupo                 |
| Remover board                | Membros do grupo                 |
| Mudar status da task         | Membros do grupo                 |
| Remover status customizado   | Membros do grupo (sem tasks)     |
| Ver minhas tasks             | Usuário autenticado (criador/responsável) |

