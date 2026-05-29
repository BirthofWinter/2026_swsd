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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Spring service that owns the four {@link IterationPipeline} instances and
 * exposes a unified {@link #runIteration(int)} entry point.
 *
 * <p>Each pipeline shares the same four specialist agent beans (stateless between
 * calls because no {@code MemorySaver} is attached to those agents). Pipelines are
 * run sequentially; the context of one iteration is not automatically passed to the
 * next — the Supervisor is responsible for providing accumulated context as needed.
 */
@Service
public class IterationPipelineService {

	private final IterationPipeline[] pipelines;

	public IterationPipelineService(
			@Qualifier("driverAnalystAgent") ReactAgent driverAnalyst,
			@Qualifier("structureDesignerAgent") ReactAgent structureDesigner,
			@Qualifier("viewRecorderAgent") ReactAgent viewRecorder,
			@Qualifier("qualityValidatorAgent") ReactAgent qualityValidator,
			ConversationLogService conversationLogService) {

		this.pipelines = new IterationPipeline[] {
			new IterationPipeline(1, "Establishing an Overall System Structure",
					driverAnalyst, structureDesigner, viewRecorder, qualityValidator, conversationLogService),
			new IterationPipeline(2, "Identifying Structures to Support Primary Functionality",
					driverAnalyst, structureDesigner, viewRecorder, qualityValidator, conversationLogService),
			new IterationPipeline(3, "Addressing Reliability and Availability Quality Attributes",
					driverAnalyst, structureDesigner, viewRecorder, qualityValidator, conversationLogService),
			new IterationPipeline(4, "Addressing Development and Operations",
					driverAnalyst, structureDesigner, viewRecorder, qualityValidator, conversationLogService),
		};
	}

	/**
	 * Runs the full four-stage pipeline for the given iteration number (1–4).
	 *
	 * @param number 1-based iteration index
	 * @return consolidated {@link IterationResult}
	 * @throws IllegalArgumentException if number is outside [1, 4]
	 * @throws Exception                if any agent call fails
	 */
	public IterationResult runIteration(int number) throws Exception {
		return runIteration(number, "");
	}

	public IterationResult runIteration(int number, String previousIterationContext) throws Exception {
		if (number < 1 || number > 4) {
			throw new IllegalArgumentException("Iteration number must be 1–4, got: " + number);
		}
		return pipelines[number - 1].run(previousIterationContext);
	}
}
