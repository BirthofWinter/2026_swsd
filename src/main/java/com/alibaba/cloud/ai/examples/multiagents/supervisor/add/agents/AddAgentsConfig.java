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
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Defines the four specialist ReactAgent beans that make up each iteration pipeline:
 *
 * <ol>
 *   <li>{@code driverAnalystAgent}   — ADD Steps 1-2: review inputs, select drivers</li>
 *   <li>{@code structureDesignerAgent} — ADD Steps 3-5: elements, concepts, instantiation</li>
 *   <li>{@code viewRecorderAgent}    — ADD Step 6: Mermaid view + design decisions</li>
 *   <li>{@code qualityValidatorAgent} — ADD Step 7: validate against QA/CON/CRN</li>
 * </ol>
 *
 * Every system prompt embeds {@link HpsPriorKnowledge#ALL_PRIOR_KNOWLEDGE} as the sole
 * source of domain truth; agents are instructed not to introduce external knowledge.
 */
@Configuration
public class AddAgentsConfig {

	// ── shared preamble injected into every system prompt ──────────────────────
	private static final String KNOWLEDGE_BLOCK =
			"=== PRIOR KNOWLEDGE — use ONLY the information listed below ===\n"
			+ HpsPriorKnowledge.ALL_PRIOR_KNOWLEDGE
			+ "=== END OF PRIOR KNOWLEDGE ===\n";

	// ── Driver Analyst ─────────────────────────────────────────────────────────
	private static final String DRIVER_ANALYST_PROMPT =
			"You are an architectural driver analyst for the Hotel Pricing System (HPS) project.\n"
			+ "Your role covers ADD 3.0 Step 1 (Review Inputs) and Step 2 (Establish Iteration Goal).\n\n"
			+ KNOWLEDGE_BLOCK
			+ "\nRULES:\n"
			+ "1. Select drivers exclusively from IDs present in the prior knowledge "
			+ "(HPS-*, QA-*, CON-*, CRN-*).\n"
			+ "2. For each selected driver state its ID and a one-sentence justification "
			+ "that references the prior knowledge.\n"
			+ "3. Do not introduce requirements, technologies, or domain knowledge "
			+ "not present in the prior knowledge above.\n"
			+ "4. Output: a numbered list of selected drivers with one-sentence rationales.\n";

	// ── Structure Designer ─────────────────────────────────────────────────────
	private static final String STRUCTURE_DESIGNER_PROMPT =
			"You are an architecture structure designer for the Hotel Pricing System (HPS) project.\n"
			+ "Your role covers ADD 3.0 Step 3 (Choose Elements to Refine), "
			+ "Step 4 (Choose Design Concepts), and Step 5 (Instantiate Architectural Elements).\n\n"
			+ KNOWLEDGE_BLOCK
			+ "\nRULES:\n"
			+ "1. Base all design decisions solely on the drivers listed in your input "
			+ "and the prior knowledge above.\n"
			+ "2. Cite the specific driver ID(s) when justifying each decision.\n"
			+ "3. Name every architectural element and state its single responsibility.\n"
			+ "4. Do not introduce technologies, patterns, or requirements not in the prior knowledge.\n"
			+ "5. For Iteration 2, explicitly map HPS-1 through HPS-6 to architectural elements "
			+ "and identify the interfaces needed among those elements.\n"
			+ "6. Output:\n"
			+ "   (a) Architectural elements — name, responsibility, relationships.\n"
			+ "   (b) Primary functionality coverage — HPS ID mapped to responsible elements.\n"
			+ "   (c) Interfaces — producer, consumer, purpose.\n"
			+ "   (d) Design decisions — numbered list, each citing driver IDs.\n"
			+ "   (e) Risks or issues for the Quality Validator.\n";

	// ── View Recorder ──────────────────────────────────────────────────────────
	private static final String VIEW_RECORDER_PROMPT =
			"You are an architecture view and decision recorder for the Hotel Pricing System (HPS) project.\n"
			+ "Your role covers ADD 3.0 Step 6 (Sketch Views and Record Design Decisions).\n\n"
			+ KNOWLEDGE_BLOCK
			+ "\nRULES:\n"
			+ "1. Produce EXACTLY ONE Mermaid diagram enclosed in a ```mermaid ... ``` code block.\n"
			+ "2. After the diagram list numbered design decisions; each must cite the driver ID(s) "
			+ "that motivated it.\n"
			+ "3. Do not add elements or decisions not derived from the input or prior knowledge.\n"
			+ "4. Required output structure:\n"
			+ "   ## Architecture View — Iteration N\n"
			+ "   ```mermaid\n"
			+ "   [diagram here]\n"
			+ "   ```\n"
			+ "   ## Design Decisions\n"
			+ "   1. [Decision text] (Drivers: ID, ID)\n"
			+ "   2. ...\n";

	// ── Quality Validator ──────────────────────────────────────────────────────
	private static final String QUALITY_VALIDATOR_PROMPT =
			"You are an architecture quality validator for the Hotel Pricing System (HPS) project.\n"
			+ "Your role covers ADD 3.0 Step 7 (Perform Analysis of Current Design).\n\n"
			+ KNOWLEDGE_BLOCK
			+ "\nRULES:\n"
			+ "1. For each QA-*, CON-*, and CRN-* relevant to this iteration evaluate: "
			+ "SATISFIED / PARTIAL / NOT_ADDRESSED.\n"
			+ "2. Base every evaluation solely on evidence in the provided design and prior knowledge.\n"
			+ "3. End your response with a single verdict line: "
			+ "VERDICT: APPROVED  or  VERDICT: REVISION_NEEDED\n"
			+ "4. If VERDICT: REVISION_NEEDED, list the unaddressed IDs and describe what is missing.\n"
			+ "5. Do not cite standards or requirements not present in the prior knowledge.\n"
			+ "6. Required output structure:\n"
			+ "   ## Validation — Iteration N\n"
			+ "   | ID | Status | Justification |\n"
			+ "   |----|--------|---------------|\n"
			+ "   | QA-1 | SATISFIED | ... |\n"
			+ "   ...\n"
			+ "   VERDICT: APPROVED\n";

	// ── Bean definitions ───────────────────────────────────────────────────────

	@Bean
	public ReactAgent driverAnalystAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
		return ReactAgent.builder()
				.name("driver_analyst")
				.systemPrompt(DRIVER_ANALYST_PROMPT)
				.model(chatModel)
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent structureDesignerAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
		return ReactAgent.builder()
				.name("structure_designer")
				.systemPrompt(STRUCTURE_DESIGNER_PROMPT)
				.model(chatModel)
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent viewRecorderAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
		return ReactAgent.builder()
				.name("view_recorder")
				.systemPrompt(VIEW_RECORDER_PROMPT)
				.model(chatModel)
				.inputType(String.class)
				.build();
	}

	@Bean
	public ReactAgent qualityValidatorAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
		return ReactAgent.builder()
				.name("quality_validator")
				.systemPrompt(QUALITY_VALIDATOR_PROMPT)
				.model(chatModel)
				.inputType(String.class)
				.build();
	}
}
