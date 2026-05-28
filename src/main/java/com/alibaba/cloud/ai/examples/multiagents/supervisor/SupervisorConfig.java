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
package com.alibaba.cloud.ai.examples.multiagents.supervisor;

import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.prior.HpsPriorKnowledge;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.tools.IterationTools;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the ADD Supervisor agent.
 *
 * <p>Architecture of the hybrid multi-agent system:
 * <pre>
 *   User / Runner
 *       │
 *       ▼
 *   Supervisor (add_supervisor)          ← ReactAgent with MemorySaver
 *   ├── tool: run_iteration_1 ──►  Pipeline-1 (4-stage fixed pipeline)
 *   ├── tool: run_iteration_2 ──►  Pipeline-2
 *   ├── tool: run_iteration_3 ──►  Pipeline-3
 *   └── tool: run_iteration_4 ──►  Pipeline-4
 *
 *   Each pipeline:
 *   Driver Analyst → Structure Designer → View Recorder → Quality Validator
 *   (Quality Validator may trigger one revision round back to Structure Designer)
 * </pre>
 *
 * <p>The Supervisor maintains multi-turn conversation state (via {@link MemorySaver})
 * and routes each iteration to the corresponding {@link IterationTools} method.
 * All agents use only the prior knowledge defined in
 * {@link HpsPriorKnowledge}.
 */
@Configuration
public class SupervisorConfig {

	private static final String SUPERVISOR_PROMPT =
			"You are the ADD Supervisor for the Hotel Pricing System (HPS) architecture design project.\n"
			+ "You coordinate the four-iteration ADD 3.0 design process using a structured multi-agent pipeline.\n\n"
			+ "=== PRIOR KNOWLEDGE — use ONLY the information listed below ===\n"
			+ HpsPriorKnowledge.ALL_PRIOR_KNOWLEDGE
			+ "=== END OF PRIOR KNOWLEDGE ===\n\n"
			+ "=== RULES ===\n"
			+ "1. Use ONLY the prior knowledge above. Do not introduce external domain knowledge.\n"
			+ "2. Run iterations strictly in order: run_iteration_1 first, then 2, 3, 4.\n"
			+ "3. Each iteration tool internally executes: "
			+ "Driver Analyst → Structure Designer → View Recorder → Quality Validator.\n"
			+ "4. After all four iterations complete, synthesize a concise overall architecture summary "
			+ "that references the Mermaid views produced in each iteration.\n"
			+ "5. All design decisions must trace back to driver IDs from the prior knowledge.\n\n"
			+ "=== WORKFLOW ===\n"
			+ "run_iteration_1 — Establishing an Overall System Structure (CRN-1, CRN-2, CRN-3, CON-1, CON-2, CON-6)\n"
			+ "run_iteration_2 — Identifying Structures for Primary Functionality (HPS-1…HPS-6, QA-5)\n"
			+ "run_iteration_3 — Addressing Reliability and Availability (QA-2, QA-3, QA-1, QA-4)\n"
			+ "run_iteration_4 — Addressing Development and Operations (QA-7, QA-8, QA-9, QA-6, CRN-4, CRN-5)\n";

	@Bean
	public MemorySaver memorySaver() {
		return new MemorySaver();
	}

	@Bean
	public ReactAgent supervisorAgent(
			@Qualifier("openAiChatModel") ChatModel chatModel,
			IterationTools iterationTools,
			MemorySaver memorySaver) {
		return ReactAgent.builder()
				.name("add_supervisor")
				.systemPrompt(SUPERVISOR_PROMPT)
				.model(chatModel)
				.saver(memorySaver)
				.methodTools(iterationTools)
				.build();
	}
}
