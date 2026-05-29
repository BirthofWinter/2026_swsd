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

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finds local output from a previous ADD iteration and loads it as context.
 */
@Service
public class LocalIterationContextService {

	private final Environment environment;

	public LocalIterationContextService(Environment environment) {
		this.environment = environment;
	}

	public IterationContext loadPreviousIterationOutput(int iterationNumber) throws IOException {
		if (iterationNumber <= 1) {
			return new IterationContext(null, "");
		}

		int previousIterationNumber = iterationNumber - 1;
		Path file = findPreviousIterationOutput(previousIterationNumber)
				.orElseThrow(() -> new IllegalStateException(
						"No local output file found for Iteration " + previousIterationNumber
								+ ". Set ITERATION" + previousIterationNumber + "_OUTPUT_FILE"
								+ " or create inputs/iteration" + previousIterationNumber + "-output.md"
								+ " or logs/iteration" + previousIterationNumber + "-*.log."));

		return new IterationContext(file, Files.readString(file, StandardCharsets.UTF_8));
	}

	private Optional<Path> findPreviousIterationOutput(int iterationNumber) throws IOException {
		String configured = environment.getProperty("ITERATION" + iterationNumber + "_OUTPUT_FILE");
		if (!StringUtils.hasText(configured)) {
			configured = environment.getProperty("add.iteration" + iterationNumber + ".output-file");
		}
		if (StringUtils.hasText(configured)) {
			Path configuredPath = Path.of(configured).toAbsolutePath().normalize();
			if (Files.isRegularFile(configuredPath)) {
				return Optional.of(configuredPath);
			}
		}

		List<Path> candidates = new ArrayList<>();
		addIfFile(candidates, Path.of("iteration" + iterationNumber + "_test_logs"));
		addIfFile(candidates, Path.of("../iteration" + iterationNumber + "_test_logs"));
		addIfFile(candidates, Path.of("inputs/iteration" + iterationNumber + "-output.md"));
		addIfFile(candidates, Path.of("../inputs/iteration" + iterationNumber + "-output.md"));
		addMatching(candidates, Path.of("logs"), "iteration" + iterationNumber);
		addMatching(candidates, Path.of("../logs"), "iteration" + iterationNumber);
		addConversationLogsContaining(candidates, Path.of("logs"), "Iteration " + iterationNumber);
		addConversationLogsContaining(candidates, Path.of("../logs"), "Iteration " + iterationNumber);

		return candidates.stream().max(Comparator.comparingLong(this::lastModified));
	}

	private void addIfFile(List<Path> candidates, Path path) {
		Path normalized = path.toAbsolutePath().normalize();
		if (Files.isRegularFile(normalized)) {
			candidates.add(normalized);
		}
	}

	private void addMatching(List<Path> candidates, Path directory, String namePart) throws IOException {
		Path normalizedDirectory = directory.toAbsolutePath().normalize();
		if (!Files.isDirectory(normalizedDirectory)) {
			return;
		}
		try (Stream<Path> stream = Files.list(normalizedDirectory)) {
			stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().contains(namePart))
					.forEach(candidates::add);
		}
	}

	private void addConversationLogsContaining(List<Path> candidates, Path directory, String contentPart)
			throws IOException {
		Path normalizedDirectory = directory.toAbsolutePath().normalize();
		if (!Files.isDirectory(normalizedDirectory)) {
			return;
		}
		try (Stream<Path> stream = Files.list(normalizedDirectory)) {
			List<Path> logs = stream.filter(Files::isRegularFile)
					.filter(path -> path.getFileName().toString().startsWith("conversation-"))
					.filter(path -> contains(path, contentPart))
					.toList();
			candidates.addAll(logs);
		}
	}

	private boolean contains(Path path, String contentPart) {
		try {
			return Files.readString(path, StandardCharsets.UTF_8).contains(contentPart);
		}
		catch (IOException ex) {
			return false;
		}
	}

	private long lastModified(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis();
		}
		catch (IOException ex) {
			return 0L;
		}
	}

	public record IterationContext(Path sourceFile, String content) {
	}
}
