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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Optional startup runner for only Part C.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "part-c.run", havingValue = "true")
public class PartCWorkflowRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(PartCWorkflowRunner.class);

	private final PartCWorkflowService workflowService;

	public PartCWorkflowRunner(PartCWorkflowService workflowService) {
		this.workflowService = workflowService;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("Starting Part C workflow: Iteration 3 and Iteration 4 only.");
		List<PartCIterationResult> results = workflowService.runPartC();
		log.info("Part C workflow completed. Iterations generated: {}", results.size());
		log.info("Part C conversation log: {}", workflowService.getConversationLogFile().toAbsolutePath());
		log.info("Part C report draft: {}", workflowService.getReportFile().toAbsolutePath());
	}
}
