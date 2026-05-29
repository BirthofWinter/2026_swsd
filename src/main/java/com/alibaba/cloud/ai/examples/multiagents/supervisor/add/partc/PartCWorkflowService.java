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
package com.alibaba.cloud.ai.examples.multiagents.supervisor.add.partc;

import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.agents.DocumentationAgent;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.agents.QualityReviewerAgent;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.logging.ConversationLogger;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.prior.HpsPriorKnowledge;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.workflow.LocalIterationContextService;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.workflow.LocalIterationContextService.IterationContext;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs only the C-side workflow for ADD Iteration 3 and Iteration 4.
 */
@Service
public class PartCWorkflowService {

	private static final Path OUTPUT_DIR = Path.of("outputs");
	private static final Path REPORT_FILE = OUTPUT_DIR.resolve("report-draft-part-c.md");

	private static final String ITERATION_3 =
			"Iteration 3: Addressing Reliability and Availability Quality Attributes";
	private static final String ITERATION_4 =
			"Iteration 4: Addressing Development and Operations";

	private final ReactAgent requirementAnalystAgent;
	private final ReactAgent architectureDesignerAgent;
	private final ReactAgent viewRecorderAgent;
	private final QualityReviewerAgent qualityReviewerAgent;
	private final DocumentationAgent documentationAgent;
	private final ConversationLogger conversationLogger;
	private final LocalIterationContextService contextService;

	public PartCWorkflowService(
			@Qualifier("driverAnalystAgent") ReactAgent requirementAnalystAgent,
			@Qualifier("structureDesignerAgent") ReactAgent architectureDesignerAgent,
			@Qualifier("viewRecorderAgent") ReactAgent viewRecorderAgent,
			QualityReviewerAgent qualityReviewerAgent,
			DocumentationAgent documentationAgent,
			ConversationLogger conversationLogger,
			LocalIterationContextService contextService) {
		this.requirementAnalystAgent = requirementAnalystAgent;
		this.architectureDesignerAgent = architectureDesignerAgent;
		this.viewRecorderAgent = viewRecorderAgent;
		this.qualityReviewerAgent = qualityReviewerAgent;
		this.documentationAgent = documentationAgent;
		this.conversationLogger = conversationLogger;
		this.contextService = contextService;
	}

	public List<PartCIterationResult> runPartC() throws Exception {
		Files.createDirectories(OUTPUT_DIR);
		conversationLogger.reset();
		Files.writeString(REPORT_FILE, "# Part C Report Draft\n\n", StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		List<PartCIterationResult> results = new ArrayList<>();
		String iteration2Context = loadPreviousContext(3);
		PartCIterationResult iteration3 = runIteration(3, ITERATION_3, iteration2Context);
		results.add(iteration3);

		String iteration4Context = """
				Previous ADD output supplied for Iteration 4. Use it only as prior architecture design context;
				do not treat it as new requirements.

				%s
				""".formatted(iteration3.documentationOutput());
		results.add(runIteration(4, ITERATION_4, iteration4Context));
		return results;
	}

	public Path getReportFile() {
		return REPORT_FILE;
	}

	public Path getConversationLogFile() {
		return conversationLogger.getLogFile();
	}

	private PartCIterationResult runIteration(int iterationNumber, String iterationName, String previousContext)
			throws Exception {
		String contextBlock = buildContextBlock(previousContext);

		String requirementInput = """
				%s

				Execute ADD Step 1 and Step 2 for this iteration only.
				Select only the architectural drivers that belong to this iteration plan.
				Do not regenerate Iteration 1 or Iteration 2.
				%s
				""".formatted(iterationName, contextBlock);
		String requirementOutput = requirementAnalystAgent.call(new UserMessage(requirementInput)).getText();
		conversationLogger.log(iterationName, "Requirement Analyst Agent", requirementInput, requirementOutput);

		String designInput = """
				%s

				Selected drivers from RequirementAnalystAgent:
				%s

				Execute ADD Steps 3, 4, 5, and 6 for this iteration only.
				Produce architectural elements, responsibilities, interfaces, design decisions, and one Mermaid
				or PlantUML view. Do not regenerate Iteration 1 or Iteration 2.
				%s
				""".formatted(iterationName, requirementOutput, contextBlock);
		String structureOutput = architectureDesignerAgent.call(new UserMessage(designInput)).getText();

		String viewInput = """
				%s

				ArchitectureDesignerAgent structural output:
				%s

				Execute ADD Step 6 for this iteration only. Produce exactly one Mermaid diagram and record
				numbered design decisions derived from the selected drivers.
				%s
				""".formatted(iterationName, structureOutput, contextBlock);
		String viewOutput = viewRecorderAgent.call(new UserMessage(viewInput)).getText();
		String designOutput = structureOutput + "\n\n" + viewOutput;
		conversationLogger.log(iterationName, "Architecture Designer Agent", designInput + "\n\n" + viewInput,
				designOutput);

		String reviewInput = """
				%s

				Prior knowledge:
				%s

				Selected drivers from RequirementAnalystAgent:
				%s

				Architecture design from ArchitectureDesignerAgent:
				%s
				""".formatted(iterationName, HpsPriorKnowledge.ALL_PRIOR_KNOWLEDGE, requirementOutput, designOutput);
		String reviewOutput = qualityReviewerAgent.review(reviewInput);
		conversationLogger.log(iterationName, "Quality Reviewer Agent", reviewInput, reviewOutput);

		String documentationInput = """
				%s

				Prior knowledge:
				%s

				Requirement analysis output:
				%s

				Architecture design output:
				%s

				Quality review output:
				%s
				""".formatted(iterationName, HpsPriorKnowledge.ALL_PRIOR_KNOWLEDGE, requirementOutput, designOutput,
				reviewOutput);
		String documentationOutput = documentationAgent.document(documentationInput);
		conversationLogger.log(iterationName, "Documentation Agent", documentationInput, documentationOutput);

		appendReportSection(iterationName, documentationOutput);
		return new PartCIterationResult(iterationNumber, iterationName, requirementOutput, designOutput, reviewOutput,
				documentationOutput);
	}

	private String loadPreviousContext(int iterationNumber) {
		try {
			IterationContext context = contextService.loadPreviousIterationOutput(iterationNumber);
			if (StringUtils.hasText(context.content())) {
				return "Previous iteration output loaded from "
						+ (context.sourceFile() == null ? "local context" : context.sourceFile())
						+ ":\n" + context.content();
			}
		}
		catch (IOException | IllegalStateException ex) {
			return "No local Iteration 2 output was found. Continue with the fixed prior knowledge only.";
		}
		return "";
	}

	private String buildContextBlock(String previousContext) {
		if (!StringUtils.hasText(previousContext)) {
			return "";
		}
		return "\nPrevious context. Use it only as prior ADD output; do not treat it as new requirements:\n"
				+ previousContext + "\n";
	}

	private void appendReportSection(String iterationName, String documentationOutput) throws IOException {
		String section = """
				## %s

				%s

				""".formatted(iterationName, documentationOutput);
		Files.writeString(REPORT_FILE, section, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}
}
