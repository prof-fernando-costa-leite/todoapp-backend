# TodoApp API Documentation

Base URL: `http://localhost:8080/api/v1`

All endpoints (except `POST /auth/register` and `POST /auth/login`) require the header:

```
Authorization: Bearer <accessToken>
```

---

## Standard Error Format

All errors return:

```json
{
  "timestamp": "2026-04-01T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Problem description",
  "path": "/api/v1/...",
  "actionId": "optional-uuid"
}
```

| Status | Meaning                                  |
|--------|------------------------------------------|
| 400    | Input validation failed                  |
| 401    | Token missing, invalid or expired        |
| 403    | User does not have permission            |
| 404    | Resource not found                       |
| 409    | Business conflict (e.g. active blocker)  |
| 500    | Unexpected internal error                |

---

## Auth

### Register user
```
POST /auth/register
```

**Body:**
```json
{
  "email": "alice@example.com",
  "displayName": "Alice",
  "password": "MyPassword@123"
}
```

**Response 201:**
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
  "password": "MyPassword@123"
}
```

**Response 200:**
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

> Save the `accessToken` and send it with all authenticated requests.

---

### Authenticated user profile
```
GET /auth/me
```

**Response 200:** Same as the `user` object from login.

---

### List users for member selection
```
GET /auth/users
```

Returns all registered users with a lightweight payload for selects/autocomplete in the frontend.

**Response 200:**
```json
[
  { "id": "uuid-owner", "displayName": "Alice" },
  { "id": "uuid-member", "displayName": "Bob" }
]
```

---

## Groups

A group is a team. Only group members can access its boards and tasks.

### Create group
```
POST /groups
```

**Body:**
```json
{
  "name": "Product Team",
  "memberIds": ["uuid-user-b", "uuid-user-c"]
}
```

> The creator automatically becomes `OWNER`. The IDs provided in `memberIds` join as `MEMBER`.

**Response 201:** `GroupDetailsResponse` (see below)

---

### List authenticated user's groups
```
GET /groups
```

**Response 200:**
```json
[
  {
    "id": "uuid",
    "name": "Product Team",
    "createdBy": "uuid",
    "createdAt": "2026-04-01T10:00:00Z"
  }
]
```

---

### Group details
```
GET /groups/{groupId}
```

**Response 200:**
```json
{
  "id": "uuid",
  "name": "Product Team",
  "createdBy": "uuid",
  "createdAt": "2026-04-01T10:00:00Z",
  "members": [
    { "userId": "uuid", "displayName": "Alice", "role": "OWNER", "addedAt": "2026-04-01T10:00:00Z" },
    { "userId": "uuid", "displayName": "Bob", "role": "MEMBER", "addedAt": "2026-04-01T10:01:00Z" }
  ]
}
```

---

### Add member
```
POST /groups/{groupId}/members
```

**Body:**
```json
{ "userId": "user-uuid" }
```

> Only the `OWNER` can add members.

**Response 200:** Updated `GroupDetailsResponse`.

---

### Remove member
```
DELETE /groups/{groupId}/members/{userId}
```

> Only the `OWNER` can remove. The owner cannot remove themselves.

**Response 204** (no body)

---

## Boards

A board belongs to a group. When created, it automatically gets 4 default statuses (`TODO`, `BLOCKED`, `DOING`, `DONE`) and initial transitions between them.

### Create board
```
POST /groups/{groupId}/boards
```

**Body:**
```json
{
  "name": "Sprint 1",
  "description": "Optional"
}
```

**Response 201:** `BoardDetailsResponse` with statuses and transitions already created.

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

### List group boards
```
GET /groups/{groupId}/boards
```

**Response 200:** List of `BoardResponse` (without statuses/transitions).

---

### Board details
```
GET /boards/{boardId}
```

**Response 200:** Complete `BoardDetailsResponse` (same as create response, including `members`).

---

### List board statuses
```
GET /boards/{boardId}/statuses
```

**Response 200:** List of `BoardStatusResponse`.

---

### List board transitions
```
GET /boards/{boardId}/transitions
```

**Response 200:**
```json
[
  { "fromStatusId": "uuid", "toStatusId": "uuid" }
]
```

---

### Create custom status
```
POST /boards/{boardId}/statuses
```

**Body:**
```json
{
  "code": "REVIEW",
  "name": "In Review",
  "rank": 25,
  "initial": false,
  "terminal": false
}
```

> `SYSTEM` statuses cannot be removed. If `initial: true`, the flag is removed from the previous status automatically.

**Response 201:** `BoardStatusResponse`

---

### Replace board transitions
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

> Replaces **all** existing board transitions at once.

**Response 200:** Updated list of `BoardTransitionResponse`.

---

### Delete custom status
```
DELETE /boards/{boardId}/statuses/{statusId}
```

> Fails with `403` if it's a `SYSTEM` status or `initial`.  
> Fails with `409` if there are tasks using the status.

**Response 204** (no body)

---

### Delete board
```
DELETE /boards/{boardId}
```

> Only group members can delete the board.  
> Deletion is cascading (statuses, transitions, tasks and task history).

**Response 204** (no body)

---

## Tasks

### Create task
```
POST /boards/{boardId}/tasks
```

**Body:**
```json
{
  "title": "Implement login screen",
  "description": "Optional, up to 2000 characters",
  "assigneeId": "optional-uuid",
  "points": 5,
  "priority": "HIGH",
  "blockerTaskId": "optional-uuid"
}
```

> `priority`: `LOW` | `MEDIUM` | `HIGH` | `CRITICAL`  
> The task is created in the board's `initial` status.  
> The `assigneeId` and `blockerTaskId` must belong to the same group/board.

**Response 201:** `TaskResponse`

```json
{
  "id": "uuid",
  "boardId": "uuid",
  "creatorId": "uuid",
  "creatorDisplayName": "Alice",
  "assigneeId": null,
  "assigneeDisplayName": null,
  "title": "Implement login screen",
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

### List board tasks
```
GET /boards/{boardId}/tasks
```

**Response 200:** List of `TaskResponse`.

> `TaskResponse` includes `creatorDisplayName` (always filled) and `assigneeDisplayName` (null when there is no assignee).

---

### My tasks
```
GET /tasks/mine
```

> Returns tasks where the authenticated user is the creator or assignee.

**Response 200:** List of `TaskResponse`.

---

### Task details
```
GET /tasks/{taskId}
```

**Response 200:** `TaskResponse`.

---

### Update task data
```
PATCH /tasks/{taskId}
```

**Body** (all optional):
```json
{
  "title": "New title",
  "description": "New description",
  "points": 8,
  "priority": "CRITICAL"
}
```

**Response 200:** Updated `TaskResponse`.

---

### Change task status
```
PATCH /tasks/{taskId}/status
```

**Body:**
```json
{ "statusId": "destination-status-uuid" }
```

**Business rules:**
- The transition must exist on the board (configured in `PUT /boards/{boardId}/transitions`).
- If the task has a `blockerTaskId` pointing to a **non-terminal** task, it **cannot advance** to a status with higher rank (returns `409`).
- If the task is already in the informed status, returns `400`.

**Response 200:** Updated `TaskResponse`.

---

### Change task assignee
```
PATCH /tasks/{taskId}/assignee
```

**Body:**
```json
{ "assigneeId": "uuid-or-null" }
```

> Send `null` to remove the assignee. The assignee must be a group member.

**Response 200:** Updated `TaskResponse`.

---

### Change blocking task
```
PATCH /tasks/{taskId}/blocker
```

**Body:**
```json
{ "blockerTaskId": "uuid-or-null" }
```

> Send `null` to remove the blocker. The blocking task must belong to the same board.  
> A task cannot block itself.

**Response 200:** Updated `TaskResponse`.

---

### Task status history
```
GET /tasks/{taskId}/history
```

**Response 200:**
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

### Delete task
```
DELETE /tasks/{taskId}
```

> Only group members can delete the task.  
> If other tasks point to it in `blockerTaskId`, the link is automatically removed.

**Response 204** (no body)

---

## Main Flows

### 1. Registration and login
```
POST /auth/register → POST /auth/login → save accessToken → all other calls
```

### 2. Create team and board
```
POST /groups  (with memberIds of colleagues)
  └─ POST /groups/{groupId}/boards
       └─ board created with 4 default statuses and automatic transitions
```

### 3. Work with tasks
```
POST /boards/{boardId}/tasks          → task created in TODO
PATCH /tasks/{taskId}/status          → move to DOING (checks blocking)
PATCH /tasks/{taskId}/status          → move to DONE (checks blocking)
GET  /tasks/{taskId}/history          → see full status history
```

### 4. Blocking between tasks
```
POST /boards/{boardId}/tasks  (task A, no blocking)
POST /boards/{boardId}/tasks  (task B, blockerTaskId = task A id)

PATCH /tasks/{taskB}/status { statusId: uuid-doing }
  → 409: task B is blocked by task A which is open

PATCH /tasks/{taskA}/status { statusId: uuid-done }
  → 200: task A completed (terminal)

PATCH /tasks/{taskB}/status { statusId: uuid-doing }
  → 200: blocking released
```

### 5. Manage team
```
POST   /groups/{groupId}/members            → add member (OWNER only)
DELETE /groups/{groupId}/members/{userId}   → remove member (OWNER only)
```

### 6. Customize workflow
```
POST /boards/{boardId}/statuses              → create REVIEW status (rank 25)
PUT  /boards/{boardId}/transitions           → redefine transitions with REVIEW
DELETE /boards/{boardId}/statuses/{statusId} → remove custom status (if not used)
```

---

## Permissions Summary

| Action                      | Who can                          |
|-----------------------------|----------------------------------|
| Create group                | Any authenticated user           |
| View group / board / task   | Group members                    |
| Add / remove member         | Group `OWNER` only               |
| Create board                | Group members                    |
| Create / edit task          | Group members                    |
| Delete task                 | Group members                    |
| Delete board                | Group members                    |
| Change task status          | Group members                    |
| Delete custom status        | Group members (no tasks using it)|
| View my tasks               | Authenticated user (creator/assignee) |

