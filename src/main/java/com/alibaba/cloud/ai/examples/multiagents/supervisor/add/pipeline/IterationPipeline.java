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
package com.alibaba.cloud.ai.examples.multiagents.supervisor.add.pipeline;

import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.logging.ConversationLogService;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.util.StringUtils;

/**
 * Executes the fixed four-stage ADD pipeline for a single design iteration:
 *
 * <pre>
 * Stage 1 — Driver Analyst    (ADD Steps 1-2): select architectural drivers
 * Stage 2 — Structure Designer(ADD Steps 3-5): elements, concepts, instantiation
 * Stage 3 — View Recorder     (ADD Step  6  ): Mermaid diagram + decision log
 * Stage 4 — Quality Validator (ADD Step  7  ): validate; may trigger one revision round
 * </pre>
 *
 * If the Validator returns {@code REVISION_NEEDED}, Stages 2-4 are repeated once
 * (MAX_REVISION_ROUNDS = 1) with the validator's feedback fed back into the designer.
 */
public class IterationPipeline {

	private static final Logger log = LoggerFactory.getLogger(IterationPipeline.class);
	private static final int MAX_REVISION_ROUNDS = 1;

	private final int iterationNumber;
	private final String iterationGoal;
	private final ReactAgent driverAnalyst;
	private final ReactAgent structureDesigner;
	private final ReactAgent viewRecorder;
	private final ReactAgent qualityValidator;
	private final ConversationLogService conversationLogService;

	public IterationPipeline(
			int iterationNumber,
			String iterationGoal,
			ReactAgent driverAnalyst,
			ReactAgent structureDesigner,
			ReactAgent viewRecorder,
			ReactAgent qualityValidator,
			ConversationLogService conversationLogService) {
		this.iterationNumber = iterationNumber;
		this.iterationGoal = iterationGoal;
		this.driverAnalyst = driverAnalyst;
		this.structureDesigner = structureDesigner;
		this.viewRecorder = viewRecorder;
		this.qualityValidator = qualityValidator;
		this.conversationLogService = conversationLogService;
	}

	/**
	 * Runs the full pipeline and returns the consolidated {@link IterationResult}.
	 */
	public IterationResult run() throws Exception {
		return run("");
	}

	/**
	 * Runs the full pipeline with optional previous-iteration context.
	 */
	public IterationResult run(String previousIterationContext) throws Exception {
		log.info("╔══ Iteration {} Pipeline Start — {} ══╗", iterationNumber, iterationGoal);
		String contextBlock = buildContextBlock(previousIterationContext);

		// ── Stage 1: Driver Analyst ──────────────────────────────────────────────
		String driverInput = "Iteration " + iterationNumber + " goal: " + iterationGoal + "\n"
				+ contextBlock
				+ "Execute ADD Step 1 and Step 2. "
				+ "Review the prior knowledge and select the architectural drivers for this iteration.";
		log.info("[Iter-{}] Stage 1/4 — Driver Analyst", iterationNumber);
		conversationLogService.record("Driver Analyst", "INPUT", driverInput);
		String driversOutput = driverAnalyst.call(new UserMessage(driverInput)).getText();
		conversationLogService.record("Driver Analyst", "OUTPUT", driversOutput);
		log.info("[Iter-{}] Drivers identified:\n{}", iterationNumber, driversOutput);

		String designOutput = "";
		String viewOutput = "";
		String validationOutput = "";
		boolean approved = false;

		for (int round = 0; round <= MAX_REVISION_ROUNDS; round++) {
			if (round > 0) {
				log.info("[Iter-{}] Revision round {} — feeding validator feedback back to designer",
						iterationNumber, round);
			}

			// ── Stage 2: Structure Designer ─────────────────────────────────────
			String designInput = "Iteration " + iterationNumber + " — Selected Drivers:\n" + driversOutput
					+ contextBlock
					+ "\n\nExecute ADD Steps 3, 4, and 5. "
					+ "Propose architectural elements and design concepts."
					+ (round > 0 ? "\n\nREVISION REQUESTED — Validator feedback:\n" + validationOutput : "");
			log.info("[Iter-{}] Stage 2/4 — Structure Designer (round {})", iterationNumber, round + 1);
			conversationLogService.record("Structure Designer", "INPUT", designInput);
			designOutput = structureDesigner.call(new UserMessage(designInput)).getText();
			conversationLogService.record("Structure Designer", "OUTPUT", designOutput);
			log.info("[Iter-{}] Design:\n{}", iterationNumber, designOutput);

			// ── Stage 3: View Recorder ───────────────────────────────────────────
			String viewInput = "Iteration " + iterationNumber + " — Architectural Structure:\n" + designOutput
					+ contextBlock
					+ "\n\nExecute ADD Step 6. "
					+ "Produce the Mermaid diagram and record numbered design decisions.";
			log.info("[Iter-{}] Stage 3/4 — View Recorder", iterationNumber);
			conversationLogService.record("View Recorder", "INPUT", viewInput);
			viewOutput = viewRecorder.call(new UserMessage(viewInput)).getText();
			conversationLogService.record("View Recorder", "OUTPUT", viewOutput);
			log.info("[Iter-{}] View:\n{}", iterationNumber, viewOutput);

			// ── Stage 4: Quality Validator ───────────────────────────────────────
			String validateInput = "Iteration " + iterationNumber
					+ " — Architecture View and Design Decisions:\n" + viewOutput
					+ contextBlock
					+ "\n\nExecute ADD Step 7. "
					+ "Validate this design against the iteration drivers and quality requirements.";
			log.info("[Iter-{}] Stage 4/4 — Quality Validator", iterationNumber);
			conversationLogService.record("Quality Validator", "INPUT", validateInput);
			validationOutput = qualityValidator.call(new UserMessage(validateInput)).getText();
			conversationLogService.record("Quality Validator", "OUTPUT", validationOutput);
			log.info("[Iter-{}] Validation:\n{}", iterationNumber, validationOutput);

			if (!validationOutput.contains("REVISION_NEEDED")) {
				approved = true;
				log.info("[Iter-{}] ✓ APPROVED (round {})", iterationNumber, round + 1);
				break;
			}
			if (round == MAX_REVISION_ROUNDS) {
				log.warn("[Iter-{}] Max revision rounds reached; proceeding with best-effort result.",
						iterationNumber);
			}
		}

		log.info("╚══ Iteration {} Pipeline Complete — {} ══╝", iterationNumber, iterationGoal);
		return new IterationResult(iterationNumber, iterationGoal,
				driversOutput, designOutput, viewOutput, validationOutput, approved);
	}

	private String buildContextBlock(String previousIterationContext) {
		if (!StringUtils.hasText(previousIterationContext)) {
			return "\n";
		}
		return "\n\nPrevious iteration context supplied by the caller. Use it only as prior ADD output; "
				+ "do not treat it as new requirements:\n"
				+ previousIterationContext
				+ "\n\n";
	}
}
