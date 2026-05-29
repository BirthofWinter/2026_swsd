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
package com.alibaba.cloud.ai.examples.multiagents.supervisor.add.designer;

import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.logging.ConversationLogService;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.pipeline.IterationPipelineService;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.pipeline.IterationResult;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.workflow.LocalIterationContextService;
import com.alibaba.cloud.ai.examples.multiagents.supervisor.add.workflow.LocalIterationContextService.IterationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * B-student endpoint for running only ADD Iteration 2 with Iteration 1 as context.
 */
@RestController
@RequestMapping("/api/add/designer")
public class DesignerIteration2Controller {

	private final IterationPipelineService pipelineService;
	private final ConversationLogService conversationLogService;
	private final LocalIterationContextService localIterationContextService;

	public DesignerIteration2Controller(
			IterationPipelineService pipelineService,
			ConversationLogService conversationLogService,
			LocalIterationContextService localIterationContextService) {
		this.pipelineService = pipelineService;
		this.conversationLogService = conversationLogService;
		this.localIterationContextService = localIterationContextService;
	}

	@GetMapping("/iteration2")
	public Map<String, Object> describeEndpoint() {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("purpose", "Run only ADD Iteration 2 with Iteration 1 output as previous context.");
		body.put("note", "Compatibility endpoint. Prefer /api/add/workflow for the unified workflow API.");
		body.put("method", "POST");
		body.put("path", "/api/add/designer/iteration2");
		body.put("autoPath", "/api/add/designer/iteration2/from-local");
		body.put("requiredBodyField", "previousIterationOutput");
		body.put("conversationLog", conversationLogService.getLogFile().toString());
		return body;
	}

	@PostMapping("/iteration2")
	public ResponseEntity<?> runIteration2(@RequestBody Iteration2Request request) throws Exception {
		if (request == null || !StringUtils.hasText(request.previousIterationOutput())) {
			return ResponseEntity.badRequest().body(Map.of(
					"error", "previousIterationOutput is required",
					"example", Map.of("previousIterationOutput", "Paste Iteration 1 output here")));
		}

		String context = request.previousIterationOutput();
		if (StringUtils.hasText(request.additionalInstruction())) {
			context = context + "\n\nAdditional B-student instruction:\n" + request.additionalInstruction();
		}

		return ResponseEntity.ok(toResponseBody(pipelineService.runIteration(2, context), null));
	}

	@PostMapping(value = "/iteration2", consumes = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<?> runIteration2FromPlainText(@RequestBody String previousIterationOutput) throws Exception {
		if (!StringUtils.hasText(previousIterationOutput)) {
			return ResponseEntity.badRequest().body(Map.of(
					"error", "Request body must contain Iteration 1 output"));
		}
		return ResponseEntity.ok(toResponseBody(pipelineService.runIteration(2, previousIterationOutput), null));
	}

	@PostMapping("/iteration2/from-local")
	public ResponseEntity<?> runIteration2FromLocalFile() throws Exception {
		IterationContext contextFile = localIterationContextService.loadPreviousIterationOutput(2);
		conversationLogService.record("Designer Iteration 2 Controller", "LOCAL_CONTEXT_FILE",
				contextFile.sourceFile().toString());
		IterationResult result = pipelineService.runIteration(2, contextFile.content());
		return ResponseEntity.ok(toResponseBody(result, contextFile.sourceFile().toString()));
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

	public record Iteration2Request(
			String previousIterationOutput,
			String additionalInstruction
	) {
	}
}
