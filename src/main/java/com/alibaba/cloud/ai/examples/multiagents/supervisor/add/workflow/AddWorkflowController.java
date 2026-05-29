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
package com.alibaba.cloud.ai.examples.multiagents.supervisor.add.workflow;

import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.logging.ConversationLogService;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.pipeline.IterationPipelineService;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.pipeline.IterationResult;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.workflow.LocalIterationContextService.IterationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified ADD workflow API for classroom runs.
 */
@RestController
@RequestMapping("/api/add/workflow")
public class AddWorkflowController {

	private final IterationPipelineService pipelineService;
	private final LocalIterationContextService contextService;
	private final ConversationLogService conversationLogService;

	public AddWorkflowController(
			IterationPipelineService pipelineService,
			LocalIterationContextService contextService,
			ConversationLogService conversationLogService) {
		this.pipelineService = pipelineService;
		this.contextService = contextService;
		this.conversationLogService = conversationLogService;
	}

	@GetMapping
	public Map<String, Object> describeWorkflow() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("name", "HPS ADD 3.0 unified workflow");
		body.put("conversationLog", conversationLogService.getLogFile().toString());
		body.put("runIterationWithBody", "POST /api/add/workflow/iterations/{number}");
		body.put("runIterationWithLocalContext", "POST /api/add/workflow/iterations/{number}/from-local-context");
		body.put("runIterationAuto", "POST /api/add/workflow/iterations/{number}/auto");
		body.put("validIterations", Map.of(
				"1", "Establishing an Overall System Structure",
				"2", "Identifying Structures to Support Primary Functionality",
				"3", "Addressing Reliability and Availability Quality Attributes",
				"4", "Addressing Development and Operations"));
		return body;
	}

	@PostMapping("/iterations/{number}")
	public ResponseEntity<?> runIteration(
			@PathVariable int number,
			@RequestBody(required = false) WorkflowRunRequest request) throws Exception {
		validateIterationNumber(number);
		String context = request != null && StringUtils.hasText(request.previousIterationOutput())
				? request.previousIterationOutput()
				: "";
		if (request != null && StringUtils.hasText(request.additionalInstruction())) {
			context = context + "\n\nAdditional workflow instruction:\n" + request.additionalInstruction();
		}
		IterationResult result = pipelineService.runIteration(number, context);
		return ResponseEntity.ok(toResponseBody(result, null));
	}

	@PostMapping(value = "/iterations/{number}", consumes = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> runIterationFromText(
			@PathVariable int number,
			@RequestBody(required = false) String previousIterationOutput) throws Exception {
		validateIterationNumber(number);
		IterationResult result = pipelineService.runIteration(number, previousIterationOutput);
		return ResponseEntity.ok(toResponseBody(result, null));
	}

	@PostMapping("/iterations/{number}/from-local-context")
	public ResponseEntity<?> runIterationFromLocalContext(@PathVariable int number) throws Exception {
		validateIterationNumber(number);
		IterationContext context = contextService.loadPreviousIterationOutput(number);
		if (context.sourceFile() != null) {
			conversationLogService.record("ADD Workflow Controller", "LOCAL_CONTEXT_FILE",
					context.sourceFile().toString());
		}
		IterationResult result = pipelineService.runIteration(number, context.content());
		return ResponseEntity.ok(toResponseBody(result,
				context.sourceFile() != null ? context.sourceFile().toString() : null));
	}

	@PostMapping("/iterations/{number}/auto")
	public ResponseEntity<?> runIterationWithBuiltInPrompt(@PathVariable int number) throws Exception {
		validateIterationNumber(number);
		String context = "";
		String previousIterationFile = null;
		if (number > 1) {
			IterationContext localContext;
			try {
				localContext = contextService.loadPreviousIterationOutput(number);
			}
			catch (IllegalStateException ex) {
				return ResponseEntity.badRequest().body(Map.of(
						"error", ex.getMessage(),
						"iteration", number,
						"conversationLog", conversationLogService.getLogFile().toString()));
			}
			context = localContext.content();
			previousIterationFile = localContext.sourceFile() != null ? localContext.sourceFile().toString() : null;
			conversationLogService.record("ADD Workflow Controller", "AUTO_LOCAL_CONTEXT_FILE",
					previousIterationFile);
		}

		conversationLogService.record("ADD Workflow Controller", "BUILT_IN_RUN",
				"Run Iteration " + number + " using the built-in ADD workflow prompt. "
						+ "Do not run other iterations and do not synthesize the final report.");
		IterationResult result = pipelineService.runIteration(number, context);
		return ResponseEntity.ok(toResponseBody(result, previousIterationFile));
	}

	private Map<String, Object> toResponseBody(IterationResult result, String previousIterationFile) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("iteration", result.iterationNumber());
		body.put("goal", result.iterationGoal());
		body.put("approved", result.approved());
		if (previousIterationFile != null) {
			body.put("previousIterationFile", previousIterationFile);
		}
		body.put("driversOutput", result.driversOutput());
		body.put("designOutput", result.designOutput());
		body.put("viewOutput", result.viewOutput());
		body.put("validationOutput", result.validationOutput());
		body.put("conversationLog", conversationLogService.getLogFile().toString());
		return body;
	}

	private void validateIterationNumber(int number) {
		if (number < 1 || number > 4) {
			throw new IllegalArgumentException("Iteration number must be 1-4, got: " + number);
		}
	}

	public record WorkflowRunRequest(
			String previousIterationOutput,
			String additionalInstruction
	) {
	}
}
