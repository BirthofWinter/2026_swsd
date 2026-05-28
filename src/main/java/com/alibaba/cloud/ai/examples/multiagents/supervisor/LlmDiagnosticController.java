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

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Browser-friendly LLM connectivity check.
 * Open http://localhost:8080/api/diagnostics/llm after startup.
 */
@RestController
@RequestMapping("/api/diagnostics")
public class LlmDiagnosticController {

	private final Environment environment;

	public LlmDiagnosticController(Environment environment) {
		this.environment = environment;
	}

	@GetMapping("/llm")
	public ResponseEntity<Map<String, Object>> checkLlm() {
		String apiKey = environment.getProperty("spring.ai.openai.api-key");
		String baseUrl = environment.getProperty("spring.ai.openai.base-url", "https://api.openai.com");
		String model = environment.getProperty("spring.ai.openai.chat.options.model", "unknown");

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", model);
		body.put("baseUrl", baseUrl);
		body.put("apiKeyConfigured", StringUtils.hasText(apiKey));
		body.put("apiKeyLength", apiKey != null ? apiKey.length() : 0);

		if (!StringUtils.hasText(apiKey)) {
			body.put("ok", false);
			body.put("httpStatus", 0);
			body.put("message", "OPENAI_API_KEY is empty. Use export OPENAI_API_KEY=... or application-local.yml");
			body.put("fix", "cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml");
			return ResponseEntity.ok(body);
		}

		String modelsUrl = resolveModelsUrl(baseUrl);
		body.put("probeUrl", modelsUrl);

		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(modelsUrl))
					.timeout(Duration.ofSeconds(20))
					.header("Authorization", "Bearer " + apiKey)
					.GET()
					.build();
			HttpResponse<String> response = HttpClient.newHttpClient()
					.send(request, HttpResponse.BodyHandlers.ofString());

			int status = response.statusCode();
			body.put("httpStatus", status);
			body.put("ok", status >= 200 && status < 300);
			if (status == 401) {
				body.put("message", "401 Unauthorized — this key is rejected by " + baseUrl);
				body.put("fix", "Wrong key, expired key, or wrong base-url. Keys like sk_m2* often need a proxy base-url from your provider, not api.openai.com.");
			}
			else if (status >= 200 && status < 300) {
				body.put("message", "API key accepted. Chat UI should work after restart.");
			}
			else {
				body.put("message", "Unexpected response: " + truncate(response.body(), 200));
			}
			return ResponseEntity.ok(body);
		}
		catch (Exception ex) {
			body.put("ok", false);
			body.put("httpStatus", -1);
			body.put("message", "Connection failed: " + ex.getMessage());
			body.put("fix", "Check network, base-url, and firewall.");
			return ResponseEntity.ok(body);
		}
	}

	private static String resolveModelsUrl(String baseUrl) {
		String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		if (base.endsWith("/v1")) {
			return base + "/models";
		}
		return base + "/v1/models";
	}

	private static String truncate(String s, int max) {
		if (s == null) {
			return "";
		}
		return s.length() <= max ? s : s.substring(0, max) + "...";
	}
}
