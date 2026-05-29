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
package com.alibaba.cloud.ai.examples.multiagents.supervisor.add.tools;

import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.pipeline.IterationPipelineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Exposes the four ADD iteration pipelines as {@code @Tool} methods so the
 * Supervisor agent can invoke them via function-calling.
 *
 * <p>Each tool triggers the full four-stage pipeline for the corresponding iteration:
 * <pre>
 *   Driver Analyst → Structure Designer → View Recorder → Quality Validator
 * </pre>
 * The Supervisor only sees the high-level tool descriptions and the final
 * {@link com.alibaba.cloud.ai.examples.multiagents.supervisor.add.pipeline.IterationResult#toSummary()}
 * returned by each tool.
 */
@Component
public class IterationTools {

	private static final Logger log = LoggerFactory.getLogger(IterationTools.class);

	private final IterationPipelineService pipelineService;

	public IterationTools(IterationPipelineService pipelineService) {
		this.pipelineService = pipelineService;
	}

	@Tool(name = "run_iteration_1",
		  description = "Run ADD 3.0 Iteration 1: Establishing an Overall System Structure. "
				  + "Primary drivers: CRN-1, CRN-2, CRN-3, CON-1, CON-2, CON-6. "
				  + "Internally runs: Driver Analyst → Structure Designer → View Recorder → Quality Validator. "
				  + "Returns the Mermaid architecture view, design decisions, and validation result.")
	public String runIteration1(
			@ToolParam(description = "Optional additional context or focus note for this iteration")
			String context) {
		return runSafe(1, context);
	}

	@Tool(name = "run_iteration_2",
		  description = "Run ADD 3.0 Iteration 2: Identifying Structures to Support Primary Functionality. "
				  + "Primary drivers: HPS-1, HPS-2, HPS-3, HPS-4, HPS-5, HPS-6, QA-5. "
				  + "Internally runs: Driver Analyst → Structure Designer → View Recorder → Quality Validator. "
				  + "Returns the Mermaid architecture view, design decisions, and validation result.")
	public String runIteration2(
			@ToolParam(description = "Optional additional context or prior iteration summary")
			String context) {
		return runSafe(2, context);
	}

	@Tool(name = "run_iteration_3",
		  description = "Run ADD 3.0 Iteration 3: Addressing Reliability and Availability Quality Attributes. "
				  + "Primary drivers: QA-2, QA-3, QA-1, QA-4. "
				  + "Internally runs: Driver Analyst → Structure Designer → View Recorder → Quality Validator. "
				  + "Returns the Mermaid architecture view, design decisions, and validation result.")
	public String runIteration3(
			@ToolParam(description = "Optional additional context or prior iteration summary")
			String context) {
		return runSafe(3, context);
	}

	@Tool(name = "run_iteration_4",
		  description = "Run ADD 3.0 Iteration 4: Addressing Development and Operations. "
				  + "Primary drivers: QA-7, QA-8, QA-9, QA-6, CRN-4, CRN-5. "
				  + "Internally runs: Driver Analyst → Structure Designer → View Recorder → Quality Validator. "
				  + "Returns the Mermaid architecture view, design decisions, and validation result.")
	public String runIteration4(
			@ToolParam(description = "Optional additional context or prior iteration summary")
			String context) {
		return runSafe(4, context);
	}

	private String runSafe(int number, String context) {
		try {
			log.info("[Supervisor→Tool] Invoking iteration {} pipeline. Context: {}", number, context);
			return pipelineService.runIteration(number, context).toSummary();
		}
		catch (Exception e) {
			log.error("[Supervisor→Tool] Iteration {} pipeline failed", number, e);
			return "Iteration " + number + " pipeline error: " + e.getMessage();
		}
	}
}
