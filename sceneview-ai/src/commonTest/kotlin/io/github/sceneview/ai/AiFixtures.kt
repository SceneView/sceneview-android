package io.github.sceneview.ai

import kotlinx.serialization.json.JsonObject

/** Three tools and one conversation, shared by the codec tests. */
internal object AiFixtures {
    val TOOLS = listOf(
        AiToolSpec("list_planes", "The planes ARCore detected.", AiSchemas.objectSchema()),
        AiToolSpec(
            "raycast",
            "A ray through a keyframe pixel.",
            AiSchemas.objectSchema(
                "frame" to AiSchemas.integer("Keyframe index"),
                "x" to AiSchemas.number("0..1 from the left"),
                "y" to AiSchemas.number("0..1 from the top"),
            ),
        ),
        AiToolSpec(
            "add_wall",
            "A wall along its floor line.",
            AiSchemas.objectSchema(
                "from" to AiSchemas.numbers("[x, z] of one end", count = 2),
                "to" to AiSchemas.numbers("[x, z] of the other end", count = 2),
                "kind" to AiSchemas.enumOf(listOf("solid", "glass"), "What the wall is made of"),
            ),
        ),
    )

    val TURN = AiTurn(
        system = "system prompt",
        messages = listOf(
            AiMessage(
                AiMessage.Role.User,
                listOf(
                    AiContent.Text("Session digest: {}"),
                    AiContent.Image("image/jpeg", "AAAA"),
                    AiContent.Text("Request: Rebuild this room"),
                ),
            ),
            AiMessage(
                AiMessage.Role.Assistant,
                listOf(AiContent.ToolUse("toolu_1", "list_planes", JsonObject(emptyMap()))),
            ),
            AiMessage(
                AiMessage.Role.User,
                listOf(
                    AiContent.ToolResult("toolu_1", "{\"planes\":[]}", isError = false),
                    AiContent.ToolResult("toolu_2", "refused", isError = true),
                ),
            ),
        ),
        tools = TOOLS,
    )
}
