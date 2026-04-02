package org.misterstorm.eventplatform.todoapp.integration

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TodoApiIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // This test intentionally focuses on end-to-end API wiring while branch-heavy rules stay in unit tests.

    @Test
    fun `full flow should enforce auth visibility and blocker rules`() {
        val ownerId = register("owner@example.com", "Owner", "OwnerPass123")
        val userB = register("member@example.com", "Member", "MemberPass123")
        val tokenA = login("owner@example.com", "OwnerPass123")
        val tokenB = login("member@example.com", "MemberPass123")

        val groupId = requiredText(createGroup(tokenA, listOf(userB)), "id")
        val board = createBoard(tokenA, groupId)
        val boardId = requiredText(board, "id")
        val members = board.required("members")
        assertEquals(2, members.size())
        assertTrue(members.any { it.required("id").asString() == ownerId && it.required("displayName").asString() == "Owner" })
        assertTrue(members.any { it.required("userId").asString() == ownerId && it.required("displayName").asString() == "Owner" })
        assertTrue(members.any { it.required("id").asString() == userB && it.required("displayName").asString() == "Member" })
        assertTrue(members.any { it.required("userId").asString() == userB && it.required("displayName").asString() == "Member" })
        val statuses = board.required("statuses")
        val todoStatusId = statusIdByCode(statuses, "TODO")
        val doingStatusId = statusIdByCode(statuses, "DOING")
        val doneStatusId = statusIdByCode(statuses, "DONE")

        val blockerTaskId = requiredText(createTask(tokenA, boardId, "Task bloqueante", null), "id")
        val blockedTaskId = requiredText(createTask(tokenA, boardId, "Task bloqueada", blockerTaskId), "id")

        mockMvc.perform(
            patch("/api/v1/tasks/$blockedTaskId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $tokenA")
                .content("{}"),
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            patch("/api/v1/tasks/$blockedTaskId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $tokenA")
                .content("{" + "\"statusId\":\"$doingStatusId\"}"),
        ).andExpect(status().isConflict)

        mockMvc.perform(
            patch("/api/v1/tasks/$blockerTaskId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $tokenA")
                .content("{" + "\"statusId\":\"$doingStatusId\"}"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            patch("/api/v1/tasks/$blockerTaskId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $tokenA")
                .content("{" + "\"statusId\":\"$doneStatusId\"}"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            patch("/api/v1/tasks/$blockedTaskId/status")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer $tokenA")
                .content("{" + "\"statusId\":\"$doingStatusId\"}"),
        ).andExpect(status().isOk)

        val myTasksB = mockMvc.perform(
            get("/api/v1/tasks/mine")
                .header("Authorization", "Bearer $tokenB"),
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        assertEquals(0, objectMapper.readTree(myTasksB).size())

        val history = mockMvc.perform(
            get("/api/v1/tasks/$blockedTaskId/history")
                .header("Authorization", "Bearer $tokenA"),
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        assertEquals(2, objectMapper.readTree(history).size())

        // Smoke checks for remaining endpoints.
        mockMvc.perform(
            get("/api/v1/groups")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/groups/$groupId")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/boards/$boardId")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/boards/$boardId/statuses")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/v1/boards/$boardId/transitions")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        val customStatus = mockMvc.perform(
            post("/api/v1/boards/$boardId/statuses")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + "\"code\":\"QA\",\"name\":\"QA\",\"rank\":25}"),
        ).andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString
        val customStatusId = requiredText(objectMapper.readTree(customStatus), "id")

        mockMvc.perform(
            put("/api/v1/boards/$boardId/transitions")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{" +
                        "\"transitions\":[" +
                        "{\"fromStatusId\":\"$todoStatusId\",\"toStatusId\":\"$doingStatusId\"}," +
                        "{\"fromStatusId\":\"$doingStatusId\",\"toStatusId\":\"$doneStatusId\"}" +
                        "]}" ,
                ),
        ).andExpect(status().isOk)

        mockMvc.perform(
            delete("/api/v1/boards/$boardId/statuses/$customStatusId")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isNoContent)

        // Test updateTask
        mockMvc.perform(
            patch("/api/v1/tasks/$blockerTaskId")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated Task\",\"points\":5,\"priority\":\"HIGH\"}"),
        ).andExpect(status().isOk)

        // Test updateAssignee - assign blockedTask to userB
        mockMvc.perform(
            patch("/api/v1/tasks/$blockedTaskId/assignee")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\":\"$userB\"}"),
        ).andExpect(status().isOk)

        val clearedAssignee = mockMvc.perform(
            patch("/api/v1/tasks/$blockedTaskId")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\":null}"),
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        assertTrue(objectMapper.readTree(clearedAssignee).required("assigneeId").isNull)

        // Test updateBlocker - set the blocker back (non-null path)
        mockMvc.perform(
            patch("/api/v1/tasks/$blockedTaskId/blocker")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockerTaskId\":\"$blockerTaskId\"}"),
        ).andExpect(status().isOk)

        // Test updateBlocker - remove the blocker (null path)
        mockMvc.perform(
            patch("/api/v1/tasks/$blockedTaskId/blocker")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockerTaskId\":null}"),
        ).andExpect(status().isOk)

        // Test deleteTask - first restore blocker reference, then delete blocker task and assert dependent task is unblocked
        mockMvc.perform(
            patch("/api/v1/tasks/$blockedTaskId/blocker")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"blockerTaskId\":\"$blockerTaskId\"}"),
        ).andExpect(status().isOk)

        mockMvc.perform(
            delete("/api/v1/tasks/$blockerTaskId")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isNoContent)

        val blockedAfterDelete = mockMvc.perform(
            get("/api/v1/tasks/$blockedTaskId")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        assertTrue(objectMapper.readTree(blockedAfterDelete).required("blockerTaskId").isNull)

        // Test listBoardTasks
        mockMvc.perform(
            get("/api/v1/boards/$boardId/tasks")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        // Test listBoards (groups/{groupId}/boards)
        mockMvc.perform(
            get("/api/v1/groups/$groupId/boards")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        // Test getTask
        mockMvc.perform(
            get("/api/v1/tasks/$blockedTaskId")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        // Test deleteBoard
        val boardToDelete = requiredText(createBoard(tokenA, groupId), "id")
        mockMvc.perform(
            delete("/api/v1/boards/$boardToDelete")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isNoContent)
        mockMvc.perform(
            get("/api/v1/boards/$boardToDelete")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isNotFound)

        // Test addMember - register a new user and add to group
        val userC = register("extra@example.com", "Extra User", "ExtraPass123")
        mockMvc.perform(
            post("/api/v1/groups/$groupId/members")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"$userC\"}"),
        ).andExpect(status().isOk)

        // Test removeMember
        mockMvc.perform(
            delete("/api/v1/groups/$groupId/members/$userC")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isNoContent)

        // Test auth/me
        mockMvc.perform(
            get("/api/v1/auth/me")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)

        // Test auth/users list for member picker
        val users = mockMvc.perform(
            get("/api/v1/auth/users")
                .header("Authorization", "Bearer $tokenA"),
        ).andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        val usersJson = objectMapper.readTree(users)
        assertTrue(usersJson.any { it.required("id").asString() == ownerId && it.required("displayName").asString() == "Owner" })
        assertTrue(usersJson.any { it.required("id").asString() == userB && it.required("displayName").asString() == "Member" })

        // Test unauthorized access (no token) - covers JwtAuthenticationFilter error path
        mockMvc.perform(
            get("/api/v1/tasks/mine"),
        ).andExpect(status().isUnauthorized)
    }

    private fun register(email: String, name: String, password: String): String {
        val body = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody("email" to email, "displayName" to name, "password" to password)),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return requiredText(objectMapper.readTree(body), "id")
    }

    private fun login(email: String, password: String): String {
        val body = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody("email" to email, "password" to password)),
        )
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString

        return requiredText(objectMapper.readTree(body), "accessToken")
    }

    private fun requiredText(node: JsonNode, fieldName: String): String = node.required(fieldName).asString()

    private fun jsonBody(vararg fields: Pair<String, Any?>): String =
        objectMapper.writeValueAsString(fields.toMap())

    private fun statusIdByCode(statuses: JsonNode, code: String): String =
        statuses.first { it.required("code").asString() == code }
            .required("id")
            .asString()

    private fun createGroup(token: String, memberIds: List<String>): JsonNode {
        val body = mockMvc.perform(
            post("/api/v1/groups")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody("name" to DEFAULT_GROUP_NAME, "memberIds" to memberIds)),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(body)
    }

    private fun createBoard(token: String, groupId: String): JsonNode {
        val body = mockMvc.perform(
            post("/api/v1/groups/$groupId/boards")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonBody("name" to DEFAULT_BOARD_NAME)),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(body)
    }

    private fun createTask(token: String, boardId: String, title: String, blockerTaskId: String?): JsonNode {
        val payload = linkedMapOf<String, Any?>("title" to title)
        if (blockerTaskId != null) {
            payload["blockerTaskId"] = blockerTaskId
        }
        val body = mockMvc.perform(
            post("/api/v1/boards/$boardId/tasks")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)),
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return objectMapper.readTree(body)
    }

    companion object {
        private const val DEFAULT_GROUP_NAME = "Core team"
        private const val DEFAULT_BOARD_NAME = "Engineering"

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("todoapp_test")
            .withUsername("todoapp")
            .withPassword("todoapp")

        @JvmStatic
        @DynamicPropertySource
        @Suppress("unused")
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgres.jdbcUrl }
            registry.add("spring.datasource.username") { postgres.username }
            registry.add("spring.datasource.password") { postgres.password }
            registry.add("app.security.jwt.secret") {
                "VGhpc0lzQVN0cm9uZ0RldmVsb3BtZW50U2VjcmV0Rm9yVGhlVG9kb0FwcEFQSR=="
            }
            registry.add("app.security.jwt.expiration") { "PT2H" }
        }
    }
}

