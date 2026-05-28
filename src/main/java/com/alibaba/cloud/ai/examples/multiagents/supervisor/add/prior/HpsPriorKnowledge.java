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
package com.alibaba.cloud.ai.examples.multiagents.supervisor.add.prior;

/**
 * Unified prior knowledge for the HPS ADD 3.0 architecture design.
 * ALL agent system prompts must draw decisions exclusively from the constants defined here.
 * No agent is allowed to introduce domain knowledge not present in this class.
 */
public final class HpsPriorKnowledge {

	private HpsPriorKnowledge() {
	}

	public static final String ADD_METHODOLOGY = """
			ADD 3.0 (Attribute-Driven Design) Method:
			Step 1 - Review Inputs: Review all requirements; identify which will be architectural drivers.
			Step 2 - Establish Iteration Goal: Select the subset of drivers to address in this iteration.
			Step 3 - Choose Elements to Refine: Select system elements to decompose or refine \
			(for a greenfield system, start from the system context or the whole system).
			Step 4 - Choose Design Concepts: Identify patterns and tactics that satisfy the iteration goal; \
			evaluate trade-offs and select.
			Step 5 - Instantiate Architectural Elements: Create elements, assign responsibilities, \
			define interfaces, and establish relationships so elements can collaborate.
			Step 6 - Sketch Views and Record Design Decisions: Document structural views using Mermaid; \
			record design decisions with rationale citing the driver IDs that motivated each decision.
			Step 7 - Perform Analysis: Check whether the current design satisfies the iteration goal; \
			determine whether more iterations are needed.
			""";

	public static final String HPS_FUNCTIONALITY = """
			HPS Primary Functionality:
			HPS-1 Log In: Business users or admins enter credentials; system calls cloud identity service to verify; \
			after login users can only query or modify hotels they are authorized for.
			HPS-2 Change Prices: User selects hotel and date, modifies base rate or fixed rate; \
			system auto-calculates all related prices, supports simulation of changes, \
			then publishes to Channel Management System (CMS) and allows external query.
			HPS-3 Query Prices: Users or external systems query hotel prices via the UI or a Query API.
			HPS-4 Manage Hotels: Admin adds, modifies, or edits hotel information including tax rates, \
			room types, and available rates.
			HPS-5 Manage Rates: Admin adds or modifies rates; defines price calculation rules.
			HPS-6 Manage Users: Admin modifies user permissions.
			""";

	public static final String HPS_QUALITY_ATTRIBUTES = """
			Quality Attributes:
			QA-1 Performance: After modifying a base price, all derived prices must be published and \
			queryable within 100 ms.
			QA-2 Reliability: 100% of price modifications must be successfully published and delivered \
			to the Channel Management System.
			QA-3 Availability: The price query subsystem must achieve an SLA of 99.9% \
			(excluding scheduled maintenance windows).
			QA-4 Scalability: The system must support 100,000 queries/day initially and scale to \
			1,000,000 queries/day with no more than 20% average latency increase.
			QA-5 Security: After login, a user can only access the features and data they are authorized for.
			QA-6 Modifiability: Future support for non-REST protocols (e.g. gRPC) must not require \
			modifying core components.
			QA-7 Deployability: Moving the application between non-production environments must require \
			no code changes.
			QA-8 Monitorability: Operations staff must be able to monitor performance and publishing \
			reliability; 100% metrics collection is required.
			QA-9 Testability: Every system module must support independent integration testing.
			""";

	public static final String HPS_CONSTRAINTS = """
			Constraints:
			CON-1: Users must access the system via web browser; support Windows, OSX, Linux, and \
			multiple device types.
			CON-2: User management must use a cloud identity service and cloud-hosted resources.
			CON-3: Code must be hosted on the company's existing Git platform.
			CON-4: System must be live within 6 months; MVP demo within 2 months.
			CON-5: The system initially interacts with the legacy system via REST API; \
			other protocols may be added in the future.
			CON-6: Cloud-native architecture is preferred.
			""";

	public static final String HPS_CONCERNS = """
			Architectural Concerns:
			CRN-1: Establish the initial overall system structure.
			CRN-2: Leverage the team's existing technical expertise: Java, Angular, Kafka.
			CRN-3: Support task allocation among the development team.
			CRN-4: Avoid technical debt.
			CRN-5: Establish a continuous deployment infrastructure.
			""";

	public static final String ITERATION_PLAN = """
			Four-Iteration ADD 3.0 Design Plan:
			Iteration 1 - Establishing an Overall System Structure: \
			Primary drivers: CRN-1, CRN-2, CRN-3, CON-1, CON-2, CON-6.
			Iteration 2 - Identifying Structures to Support Primary Functionality: \
			Primary drivers: HPS-1, HPS-2, HPS-3, HPS-4, HPS-5, HPS-6, QA-5.
			Iteration 3 - Addressing Reliability and Availability Quality Attributes: \
			Primary drivers: QA-2, QA-3, QA-1, QA-4.
			Iteration 4 - Addressing Development and Operations: \
			Primary drivers: QA-7, QA-8, QA-9, QA-6, CRN-4, CRN-5.
			""";

	/** Combined constant used as the single source of truth in all system prompts. */
	public static final String ALL_PRIOR_KNOWLEDGE = ADD_METHODOLOGY
			+ "\n" + HPS_FUNCTIONALITY
			+ "\n" + HPS_QUALITY_ATTRIBUTES
			+ "\n" + HPS_CONSTRAINTS
			+ "\n" + HPS_CONCERNS
			+ "\n" + ITERATION_PLAN;
}
