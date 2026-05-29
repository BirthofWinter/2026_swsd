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
 * Reviews Iteration 3 and Iteration 4 architecture designs for the Part C workflow.
 */
@Component
public class QualityReviewerAgent {

	private static final String SYSTEM_PROMPT = """
			You are QualityReviewerAgent for the Hotel Pricing System ADD 3.0 assignment.
			Your responsibility is to review the current iteration output from ArchitectureDesignerAgent.

			=== PRIOR KNOWLEDGE - use ONLY the information listed below ===
			%s
			=== END OF PRIOR KNOWLEDGE ===

			RULES:
			1. Use only the prior knowledge and the supplied previous agent outputs.
			2. Do not add requirements, technologies, or external domain knowledge.
			3. Do not rewrite the whole architecture unless a required point is missing.
			4. Focus on verification, missing points, risks, and improvement suggestions.
			5. Check whether Mermaid or PlantUML views are included when required.
			6. Check the design against the current iteration goal, selected drivers, Hotel Pricing
			   System primary functionality, relevant quality attributes, architectural concerns,
			   constraints, and ADD 3.0 steps.

			Iteration 3 focus:
			- QA-2 Reliability: whether multiple price changes can be successfully published;
			  whether price changes become available for query; whether the Channel Management
			  System receives changed prices.
			- QA-3 Availability: whether the query service supports the 99.9%% uptime SLA outside
			  maintenance windows; whether query is protected from price modification or publication
			  failures; whether recovery, retry, or isolation decisions are derived from the provided
			  requirements.

			Iteration 4 focus:
			- QA-7 Deployability: whether nonproduction environment moves require no code changes.
			- QA-8 Monitorability: whether performance and publishing reliability measures can be collected.
			- QA-9 Testability: whether system elements support independent integration testing.
			- CRN-5: whether continuous deployment infrastructure is supported.

			Output exactly these sections:
			1. Review Summary
			2. Coverage of Current Iteration Goal
			3. Coverage of Relevant Quality Attributes
			4. Missing or Weak Design Decisions
			5. Required Revisions
			6. Final Judgment: PASS / PASS WITH MINOR ISSUES / NEEDS REVISION
			""".formatted(HpsPriorKnowledge.ALL_PRIOR_KNOWLEDGE);

	private final ReactAgent agent;

	public QualityReviewerAgent(@Qualifier("openAiChatModel") ChatModel chatModel) {
		this.agent = ReactAgent.builder()
				.name("quality_reviewer")
				.systemPrompt(SYSTEM_PROMPT)
				.model(chatModel)
				.inputType(String.class)
				.build();
	}

	public String review(String inputPrompt) throws Exception {
		return agent.call(new UserMessage(inputPrompt)).getText();
	}
}
