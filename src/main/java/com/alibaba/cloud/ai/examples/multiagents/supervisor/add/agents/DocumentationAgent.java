/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.supervisor.add.agents;

import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.prior.HpsPriorKnowledge;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Produces concise English report sections for Iteration 3 and Iteration 4.
 */
@Component
public class DocumentationAgent {

	private static final String SYSTEM_PROMPT = """
			You are DocumentationAgent for the Hotel Pricing System ADD 3.0 assignment.
			Your responsibility is to convert previous agent outputs into concise English report content.

			=== PRIOR KNOWLEDGE - use ONLY the information listed below ===
			%s
			=== END OF PRIOR KNOWLEDGE ===

			RULES:
			1. Generate only Iteration 3 or Iteration 4 content, according to the input.
			2. Do not generate Iteration 1 or Iteration 2 content.
			3. Use only the prior knowledge and previous agent outputs.
			4. Do not add requirements beyond the provided case.
			5. Preserve Mermaid or PlantUML code when diagrams are produced.
			6. If the reviewer identified minor issues, incorporate reasonable corrections without
			   adding external assumptions.
			7. Write in clear and concise English.

			Output only the report section for the current iteration with exactly this structure:
			ADD Step 2: Establish the Iteration Goal by Selecting Drivers
			ADD Step 3: Choose One or More Elements of the System to Refine
			ADD Step 4: Choose One or More Design Concepts
			ADD Step 5: Instantiate Architectural Elements, Allocate Responsibilities, and Define Interfaces
			ADD Step 6: Sketch Views and Record Design Decisions
			ADD Step 7: Perform Analysis of Current Design
			""".formatted(HpsPriorKnowledge.ALL_PRIOR_KNOWLEDGE);

	private final ReactAgent agent;

	public DocumentationAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
		this.agent = ReactAgent.builder()
				.name("documentation")
				.systemPrompt(SYSTEM_PROMPT)
				.model(chatModel)
				.inputType(String.class)
				.build();
	}

	public String document(String inputPrompt) throws Exception {
		return agent.call(new UserMessage(inputPrompt)).getText();
	}
}
