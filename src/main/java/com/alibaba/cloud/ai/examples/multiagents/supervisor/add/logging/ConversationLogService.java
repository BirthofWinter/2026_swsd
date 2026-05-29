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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Writes agent inputs and outputs to one timestamped Markdown log per application run.
 */
@Service
public class ConversationLogService implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(ConversationLogService.class);

	private final Path logFile;

	public ConversationLogService() throws IOException {
		Path logDir = Path.of("logs");
		Files.createDirectories(logDir);
		String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
				.format(OffsetDateTime.now());
		this.logFile = logDir.resolve("conversation-" + timestamp + ".md");
		append("# HPS ADD 3.0 Conversation Log\n\n");
		append("Started: " + OffsetDateTime.now() + "\n\n");
	}

	public synchronized void record(String actor, String direction, String content) {
		String safeContent = content == null ? "" : content;
		append("## " + OffsetDateTime.now() + " - " + actor + " - " + direction + "\n\n");
		append("```text\n" + safeContent + "\n```\n\n");
	}

	public Path getLogFile() {
		return logFile;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		record("Application", "READY", "Conversation log is active: " + logFile.toAbsolutePath());
		log.info("Conversation log is active: {}", logFile.toAbsolutePath());
	}

	private void append(String text) {
		try {
			Files.writeString(logFile, text, StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write conversation log: " + logFile, ex);
		}
	}
}
