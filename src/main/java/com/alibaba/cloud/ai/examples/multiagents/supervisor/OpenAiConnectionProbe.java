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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Probes OpenAI-compatible API on startup and prints a clear PASS/FAIL banner.
 */
@Component
public class OpenAiConnectionProbe implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(OpenAiConnectionProbe.class);

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		Environment env = event.getApplicationContext().getEnvironment();
		String apiKey = env.getProperty("spring.ai.openai.api-key");
		String baseUrl = env.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
		String model = env.getProperty("spring.ai.openai.chat.options.model", "(default)");

		if (!StringUtils.hasText(apiKey)) {
			log.error("""
					╔══════════════════════════════════════════════════════════════════╗
					║  LLM CHECK: FAIL — API key is empty                              ║
					║  Chat UI will show NO reply.                                     ║
					║  Fix: application-local.yml OR export OPENAI_API_KEY             ║
					║  Diagnose: http://localhost:8080/api/diagnostics/llm             ║
					╚══════════════════════════════════════════════════════════════════╝
					""");
			return;
		}

		String modelsUrl = resolveModelsUrl(baseUrl);
		try {
			var response = RestClient.create()
					.get()
					.uri(modelsUrl)
					.header("Authorization", "Bearer " + apiKey)
					.retrieve()
					.toEntity(String.class);

			int status = response.getStatusCode().value();
			if (status >= 200 && status < 300) {
				log.info("""
						╔══════════════════════════════════════════════════════════════════╗
						║  LLM CHECK: PASS (HTTP {})                                        ║
						║  base-url={}  model={}                       ║
						║  Chat UI: http://localhost:8080/chatui/index.html                ║
						╚══════════════════════════════════════════════════════════════════╝
						""", status, baseUrl, model);
			}
			else {
				log.error("""
						╔══════════════════════════════════════════════════════════════════╗
						║  LLM CHECK: FAIL (HTTP {})                                        ║
						║  Chat UI will show NO assistant message.                         ║
						║  Diagnose: http://localhost:8080/api/diagnostics/llm             ║
						╚══════════════════════════════════════════════════════════════════╝
						""", status);
			}
		}
		catch (Exception ex) {
			String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
			if (msg.contains("401")) {
				log.error("""
						╔══════════════════════════════════════════════════════════════════╗
						║  LLM CHECK: FAIL — 401 Unauthorized                              ║
						║  Your key is NOT valid for: {}                    ║
						║  If key is from a school/proxy, set spring.ai.openai.base-url    ║
						║  in application-local.yml (see application-local.yml.example)    ║
						║  Diagnose: http://localhost:8080/api/diagnostics/llm             ║
						╚══════════════════════════════════════════════════════════════════╝
						""", baseUrl);
			}
			else {
				log.error("LLM CHECK: FAIL — {}. Diagnose: http://localhost:8080/api/diagnostics/llm", msg);
			}
		}
	}

	private static String resolveModelsUrl(String baseUrl) {
		String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		if (base.endsWith("/v1")) {
			return base + "/models";
		}
		return base + "/v1/models";
	}
}
