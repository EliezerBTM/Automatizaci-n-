/*
 * QA practice INF-433 (Unit II-2): Selenium WebDriver UI tests, run with JUnit Jupiter,
 * against the existing Spring PetClinic web application (Apache License 2.0).
 */
package org.springframework.samples.petclinic.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * UI tests for the "Add owner" flow of Spring PetClinic. The application is started by
 * Spring Boot on a random port with its in-memory H2 database, so the suite is
 * self-contained and needs no manual start-up, which is what makes it usable in Jenkins.
 * <p>
 * Requirement under test: an owner is created only when the form data satisfies the
 * validation rules of {@code Owner} (all fields required, telephone with exactly 10
 * digits).
 * <ul>
 * <li>S-01 (positive): valid data creates the owner, confirms it and makes it findable.</li>
 * <li>S-02 (negative): an invalid telephone is rejected and nothing is stored.</li>
 * </ul>
 * Every representative step is saved as a PNG under {@code target/screenshots}.
 */
@Tag("selenium")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class OwnerFlowSeleniumTest {

	private static final Path SCREENSHOT_ROOT = Path.of("target", "screenshots");

	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	/**
	 * URL of an owner details page, e.g. {@code /owners/11}. Tomcat may append
	 * {@code ;jsessionid=...} to the first redirect of a browser that has no session
	 * cookie yet, so that optional suffix must be accepted.
	 */
	private static final String OWNER_DETAILS_URL = ".*/owners/\\d+(;jsessionid=[^?#]*)?$";

	@LocalServerPort
	private int port;

	private WebDriver driver;

	private WebDriverWait wait;

	private ScreenshotRecorder screenshots;

	@BeforeEach
	void openBrowser() {
		this.driver = newChromeDriver();
		this.wait = new WebDriverWait(this.driver, TIMEOUT);
	}

	@AfterEach
	void closeBrowser() {
		if (this.driver != null) {
			this.driver.quit();
		}
	}

	@Test
	@DisplayName("S-01 positive: valid data creates the owner and it can be found afterwards")
	void createOwnerWithValidData() {
		this.screenshots = new ScreenshotRecorder(this.driver, SCREENSHOT_ROOT, "S-01-valid-owner");
		String lastName = uniqueLastName();

		// Precondition: no owner with this unique last name exists yet.
		this.driver.get(baseUrl() + "/owners?lastName=" + lastName);
		WebElement notFound = this.wait
			.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".help-inline p")));
		assertTrue(notFound.getText().toLowerCase().contains("not"), "Precondition: the owner must not exist yet");
		this.screenshots.capture("precondition-owner-not-found");

		// Open the creation form through the UI (Find owners -> Add Owner).
		this.driver.get(baseUrl() + "/owners/find");
		this.wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("a[href='/owners/new']"))).click();
		this.wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("add-owner-form")));
		this.screenshots.capture("empty-form");

		// Enter valid data.
		fillOwnerForm("Ana", lastName, "Calle Duarte 15", "Santiago", "8095551234");
		this.screenshots.capture("form-filled");

		// Execute the action and wait for the redirect to the owner details page.
		this.driver.findElement(By.cssSelector("#add-owner-form button[type='submit']")).click();
		this.wait.until(ExpectedConditions.urlMatches(OWNER_DETAILS_URL));

		// Assertions: confirmation message and the persisted data shown on the page.
		// textContent is used because the page hides the message with JavaScript after 3 seconds.
		WebElement message = this.wait.until(ExpectedConditions.presenceOfElementLocated(By.id("success-message")));
		assertEquals("New Owner Created", message.getDomProperty("textContent").trim());
		assertEquals("Ana " + lastName, this.driver.findElement(By.cssSelector("table.table-striped b")).getText());
		assertEquals("8095551234", cellFor("Telephone").getText());
		assertEquals("Santiago", cellFor("City").getText());
		this.screenshots.capture("owner-created");

		// Observable effect: searching by the unique last name now finds exactly this owner.
		this.driver.get(baseUrl() + "/owners?lastName=" + lastName);
		this.wait.until(ExpectedConditions.urlMatches(OWNER_DETAILS_URL));
		assertEquals("Ana " + lastName, this.driver.findElement(By.cssSelector("table.table-striped b")).getText());
		this.screenshots.capture("owner-found-by-search");
	}

	@Test
	@DisplayName("S-02 negative: an invalid telephone is rejected and no owner is stored")
	void rejectOwnerWithInvalidTelephone() {
		this.screenshots = new ScreenshotRecorder(this.driver, SCREENSHOT_ROOT, "S-02-invalid-telephone");
		String lastName = uniqueLastName();

		// Precondition: open the creation form.
		this.driver.get(baseUrl() + "/owners/new");
		this.wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("add-owner-form")));
		this.screenshots.capture("empty-form");

		// Enter data whose telephone has only 5 digits (the rule requires exactly 10).
		fillOwnerForm("Beto", lastName, "Calle Sol 3", "Santiago", "12345");
		this.screenshots.capture("form-filled-invalid-telephone");

		// Execute the action and wait for the validation message.
		this.driver.findElement(By.cssSelector("#add-owner-form button[type='submit']")).click();
		this.wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".has-error .help-inline"),
				"10-digit"));

		// Assertions: the form is shown again with the error and no redirect happened.
		assertTrue(this.driver.getCurrentUrl().endsWith("/owners/new"), "The user must stay on the creation form");
		assertTrue(this.driver.findElement(By.cssSelector(".has-error .help-inline")).getText().contains("10-digit"));
		assertFalse(this.driver.findElements(By.id("success-message")).size() > 0, "No success message is expected");
		this.screenshots.capture("validation-error");

		// No unauthorized record: the owner cannot be found afterwards.
		this.driver.get(baseUrl() + "/owners?lastName=" + lastName);
		WebElement notFound = this.wait
			.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".help-inline p")));
		assertTrue(notFound.getText().toLowerCase().contains("not"), "The rejected owner must not have been stored");
		assertFalse(this.driver.getCurrentUrl().matches(OWNER_DETAILS_URL),
				"A stored owner would redirect to details");
		this.screenshots.capture("owner-not-stored");
	}

	private void fillOwnerForm(String firstName, String lastName, String address, String city, String telephone) {
		type("firstName", firstName);
		type("lastName", lastName);
		type("address", address);
		type("city", city);
		type("telephone", telephone);
	}

	private void type(String fieldId, String value) {
		WebElement field = this.wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(fieldId)));
		field.clear();
		field.sendKeys(value);
	}

	private WebElement cellFor(String header) {
		return this.driver.findElement(By.xpath("//table[contains(@class,'table-striped')]//th[normalize-space()='"
				+ header + "']/following-sibling::td"));
	}

	private String baseUrl() {
		return "http://localhost:" + this.port;
	}

	private static String uniqueLastName() {
		return "Qa" + System.currentTimeMillis();
	}

	private static WebDriver newChromeDriver() {
		ChromeOptions options = new ChromeOptions();
		String binary = firstNonBlank(System.getProperty("chrome.bin"), System.getenv("CHROME_BIN"));
		if (binary != null) {
			options.setBinary(binary);
		}
		String driverPath = firstNonBlank(System.getProperty("webdriver.chrome.driver"), System.getenv("CHROMEDRIVER"));
		if (driverPath != null) {
			System.setProperty("webdriver.chrome.driver", driverPath);
		}
		if (Boolean.parseBoolean(System.getProperty("selenium.headless", "true"))) {
			options.addArguments("--headless=new");
		}
		options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--window-size=1280,1000", "--lang=en-US");
		return new ChromeDriver(options);
	}

	private static String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		return (second != null && !second.isBlank()) ? second : null;
	}

}
