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

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Small browser landing page for classroom runs.
 */
@RestController
public class HomeController {

	@GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
	public String home() {
		return """
				<!doctype html>
				<html lang="en">
				<head>
				  <meta charset="utf-8">
				  <meta name="viewport" content="width=device-width, initial-scale=1">
				  <title>HPS ADD 3.0 Multi-Agent</title>
				  <style>
				    body {
				      margin: 0;
				      font-family: Arial, Helvetica, sans-serif;
				      color: #1f2937;
				      background: #f7f8fa;
				    }
				    main {
				      max-width: 920px;
				      margin: 0 auto;
				      padding: 48px 24px;
				    }
				    h1 {
				      margin: 0 0 8px;
				      font-size: 32px;
				      line-height: 1.2;
				    }
				    p {
				      margin: 0 0 24px;
				      color: #4b5563;
				      line-height: 1.6;
				    }
				    .actions {
				      display: grid;
				      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
				      gap: 12px;
				    }
				    a, button {
				      display: block;
				      width: 100%;
				      box-sizing: border-box;
				      border: 1px solid #d1d5db;
				      border-radius: 8px;
				      background: #ffffff;
				      color: #111827;
				      padding: 14px 16px;
				      text-align: left;
				      text-decoration: none;
				      font: inherit;
				      cursor: pointer;
				    }
				    a:hover, button:hover {
				      border-color: #2563eb;
				      color: #1d4ed8;
				    }
				    code {
				      background: #eef2ff;
				      border-radius: 4px;
				      padding: 2px 5px;
				    }
				    .note {
				      margin-top: 24px;
				      font-size: 14px;
				    }
				  </style>
				</head>
				<body>
				  <main>
				    <h1>HPS ADD 3.0 Multi-Agent</h1>
				    <p>Spring Boot is running. Use these links for diagnostics, Studio chat, and B-student Iteration 2.</p>
				    <div class="actions">
				      <a href="/api/diagnostics/llm">Check LLM Connection</a>
				      <a href="/chatui/index.html">Open Chat UI</a>
				      <a href="/api/add/workflow">View ADD Workflow API</a>
				      <button type="button" onclick="runIteration(1)">Run Iteration 1</button>
				      <button type="button" onclick="runIteration(2)">Run Iteration 2</button>
				      <button type="button" onclick="runIteration(3)">Run Iteration 3</button>
				      <button type="button" onclick="runIteration(4)">Run Iteration 4</button>
				    </div>
				    <p class="note">
				      Iteration buttons call <code>POST /api/add/workflow/iterations/{number}/auto</code>.
				      Iteration 1 uses the built-in ADD prompt. Later iterations load the previous local output file and write a timestamped conversation log under <code>logs/</code>.
				    </p>
				  </main>
				  <script>
				    async function runIteration(number) {
				      const button = event.currentTarget;
				      button.disabled = true;
				      button.textContent = 'Running Iteration ' + number + '...';
				      try {
				        const response = await fetch('/api/add/workflow/iterations/' + number + '/auto', { method: 'POST' });
				        const text = await response.text();
				        const blob = new Blob([text], { type: 'application/json' });
				        const url = URL.createObjectURL(blob);
				        window.open(url, '_blank');
				        button.textContent = response.ok ? 'Iteration ' + number + ' Response Opened' : 'Iteration ' + number + ' Failed';
				      }
				      catch (error) {
				        button.textContent = 'Request Failed';
				        alert(error);
				      }
				      finally {
				        button.disabled = false;
				      }
				    }
				  </script>
				</body>
				</html>
				""";
	}
}
