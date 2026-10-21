/*
 * QA practice INF-433 (Unit II-2): helper that stores one numbered PNG per test step.
 */
package org.springframework.samples.petclinic.qa;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

/**
 * Saves screenshots as {@code <root>/<scenario>/<NN>-<step>.png}. The numbering keeps
 * the evidence in execution order, which is what the report figures and the Jenkins
 * archived artifacts rely on.
 */
final class ScreenshotRecorder {

	private final TakesScreenshot camera;

	private final Path directory;

	private int counter;

	ScreenshotRecorder(WebDriver driver, Path root, String scenario) {
		this.camera = (TakesScreenshot) driver;
		this.directory = root.resolve(scenario);
		try {
			Files.createDirectories(this.directory);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot create screenshot directory " + this.directory, ex);
		}
	}

	/**
	 * Captures the current browser viewport.
	 * @param step short description used in the file name, e.g. "form-filled"
	 * @return the path of the PNG file that was written
	 */
	Path capture(String step) {
		this.counter++;
		Path target = this.directory.resolve("%02d-%s.png".formatted(this.counter, step));
		try {
			Path source = this.camera.getScreenshotAs(OutputType.FILE).toPath();
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot store screenshot " + target, ex);
		}
		return target;
	}

}
