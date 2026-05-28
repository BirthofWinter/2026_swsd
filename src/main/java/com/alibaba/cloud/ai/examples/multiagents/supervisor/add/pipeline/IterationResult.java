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

/**
 * Immutable record holding the complete output of one ADD iteration pipeline run.
 *
 * @param iterationNumber  1-4
 * @param iterationGoal    human-readable goal for this iteration
 * @param driversOutput    output of the Driver Analyst stage (ADD Steps 1-2)
 * @param designOutput     output of the Structure Designer stage (ADD Steps 3-5)
 * @param viewOutput       output of the View Recorder stage (ADD Step 6) — includes Mermaid
 * @param validationOutput output of the Quality Validator stage (ADD Step 7)
 * @param approved         true if the Validator returned APPROVED (or max retries exhausted)
 */
public record IterationResult(
		int iterationNumber,
		String iterationGoal,
		String driversOutput,
		String designOutput,
		String viewOutput,
		String validationOutput,
		boolean approved
) {

	/**
	 * Compact human-readable summary used as the tool return value to the Supervisor.
	 */
	public String toSummary() {
		return """
				╔══ Iteration %d: %s ══╗
				Status: %s

				## Selected Drivers
				%s

				## Architecture View & Design Decisions
				%s

				## Quality Validation
				%s
				╚══ End of Iteration %d ══╝
				""".formatted(
						iterationNumber, iterationGoal,
						approved ? "APPROVED" : "NEEDS_REVIEW",
						driversOutput,
						viewOutput,
						validationOutput,
						iterationNumber);
	}
}
