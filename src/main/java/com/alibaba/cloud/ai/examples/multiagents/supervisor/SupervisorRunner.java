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

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Drives the complete HPS ADD 3.0 four-iteration architecture design via the Supervisor
 * when {@code supervisor.run-examples=true}.
 *
 * <p>The Supervisor is given a single high-level request and decides (via its system
 * instructions) to call {@code run_iteration_1} through {@code run_iteration_4} in
 * order. Each tool call triggers the internal four-stage pipeline:
 * Driver Analyst → Structure Designer → View Recorder → Quality Validator.
 *
 * <p>All log lines are timestamped by Spring Boot's default log format, providing the
 * complete interaction log required for the assignment deliverables.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "supervisor.run-examples", havingValue = "true")
public class SupervisorRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SupervisorRunner.class);

	private final ReactAgent supervisorAgent;

	public SupervisorRunner(@Qualifier("supervisorAgent") ReactAgent supervisorAgent) {
		this.supervisorAgent = supervisorAgent;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("╔══════════════════════════════════════════════════════════════╗");
		log.info("║  HPS ADD 3.0 Multi-Agent Architecture Design — START         ║");
		log.info("║  Paradigm : Multi-agent (Supervisor + 4-stage pipeline)      ║");
		log.info("║  Model    : gpt-5.4                                          ║");
		log.info("╚══════════════════════════════════════════════════════════════╝");

		String request =
				"Run the complete ADD 3.0 architecture design for the Hotel Pricing System. "
				+ "Execute all four iterations in order using the available tools: "
				+ "(1) run_iteration_1 for Establishing an Overall System Structure, "
				+ "(2) run_iteration_2 for Identifying Structures to Support Primary Functionality, "
				+ "(3) run_iteration_3 for Addressing Reliability and Availability, "
				+ "(4) run_iteration_4 for Addressing Development and Operations. "
				+ "After all four iterations, synthesize a final architecture summary.";

		log.info("──────────────────────────────────────────────────────────────");
		log.info("[User → Supervisor] {}", request);
		log.info("──────────────────────────────────────────────────────────────");

		AssistantMessage response = supervisorAgent.call(new UserMessage(request));

		log.info("──────────────────────────────────────────────────────────────");
		log.info("[Supervisor → User]\n{}", response.getText());
		log.info("╔══════════════════════════════════════════════════════════════╗");
		log.info("║  HPS ADD 3.0 Multi-Agent Architecture Design — COMPLETE      ║");
		log.info("╚══════════════════════════════════════════════════════════════╝");
	}
}
