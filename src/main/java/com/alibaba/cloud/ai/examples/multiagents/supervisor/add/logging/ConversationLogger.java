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
package com.alibaba.cloud.ai.examples.multiagents.supervisor.add.logging;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

/**
 * Markdown conversation log for the Part C deliverable.
 */
@Component
public class ConversationLogger {

	private static final Path OUTPUT_DIR = Path.of("outputs");
	private static final Path LOG_FILE = OUTPUT_DIR.resolve("conversation-log.md");

	public ConversationLogger() throws IOException {
		Files.createDirectories(OUTPUT_DIR);
	}

	public synchronized void reset() {
		write("# Part C Conversation Log\n\n", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	public synchronized void log(String iterationName, String agentName, String inputPrompt, String output) {
		String entry = """
				---

				## %s

				### [%s] %s

				#### Input

				```text
				%s
				```

				#### Output

				%s

				""".formatted(
				iterationName,
				OffsetDateTime.now(),
				agentName,
				inputPrompt == null ? "" : inputPrompt,
				output == null ? "" : output);
		write(entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}

	public Path getLogFile() {
		return LOG_FILE;
	}

	private void write(String text, StandardOpenOption... options) {
		try {
			Files.writeString(LOG_FILE, text, StandardCharsets.UTF_8, options);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write conversation log: " + LOG_FILE, ex);
		}
	}
}
