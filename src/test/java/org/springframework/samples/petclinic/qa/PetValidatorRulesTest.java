/*
 * QA practice INF-433 (Unit II-2): JUnit Jupiter unit tests for a real business rule of
 * the existing Spring PetClinic project (Apache License 2.0).
 */
package org.springframework.samples.petclinic.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetValidator;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * Unit tests for the business rule implemented by {@link PetValidator}:
 * <ul>
 * <li>the name is required and has at most 30 characters,</li>
 * <li>a new pet must have a type,</li>
 * <li>the birth date is required.</li>
 * </ul>
 * Isolation: {@code PetValidator} has no repository, network or Spring context
 * dependency, so the unit is exercised directly with plain objects. A
 * {@link BeanPropertyBindingResult} collects the errors exactly as Spring MVC does, which
 * makes every test deterministic and independent of the database.
 */
@Tag("unit")
class PetValidatorRulesTest {

	private static final int MAX_NAME_LENGTH = 30;

	private final PetValidator validator = new PetValidator();

	private static Pet validPet() {
		PetType dog = new PetType();
		dog.setName("dog");
		Pet pet = new Pet();
		pet.setName("Buddy");
		pet.setType(dog);
		pet.setBirthDate(LocalDate.of(2020, 5, 17));
		return pet;
	}

	private Errors validate(Pet pet) {
		Errors errors = new BeanPropertyBindingResult(pet, "pet");
		this.validator.validate(pet, errors);
		return errors;
	}

	@Test
	@DisplayName("U-01 valid: a pet with name, type and birth date has no errors")
	void validPetHasNoErrors() {
		Errors errors = validate(validPet());

		assertFalse(errors.hasErrors(), "A complete pet must pass validation");
		assertEquals(0, errors.getErrorCount());
	}

	@Test
	@DisplayName("U-02 limit: a name of exactly 30 characters is accepted")
	void nameWithExactlyMaximumLengthIsAccepted() {
		Pet pet = validPet();
		pet.setName("A".repeat(MAX_NAME_LENGTH));

		Errors errors = validate(pet);

		assertFalse(errors.hasFieldErrors("name"), "30 characters is the largest allowed name");
	}

	@Test
	@DisplayName("U-03 limit/invalid: a name of 31 characters is rejected with the size code")
	void nameOneCharacterOverTheLimitIsRejected() {
		Pet pet = validPet();
		pet.setName("A".repeat(MAX_NAME_LENGTH + 1));

		Errors errors = validate(pet);

		FieldError error = errors.getFieldError("name");
		assertNotNull(error, "A 31-character name must produce a field error");
		assertEquals("size", error.getCode());
		assertEquals(1, errors.getErrorCount(), "Only the name is wrong, so only one error is expected");
	}

	@Test
	@DisplayName("U-04 invalid: a blank name is rejected as required")
	void blankNameIsRejectedAsRequired() {
		Pet pet = validPet();
		pet.setName("   ");

		Errors errors = validate(pet);

		FieldError error = errors.getFieldError("name");
		assertNotNull(error, "A name made only of spaces must be rejected");
		assertEquals("required", error.getCode());
	}

	@Test
	@DisplayName("U-05 invalid: a new pet without a type is rejected")
	void newPetWithoutTypeIsRejected() {
		Pet pet = validPet();
		pet.setType(null);

		Errors errors = validate(pet);

		assertTrue(pet.isNew(), "Precondition: a pet without id is new");
		assertTrue(errors.hasFieldErrors("type"));
		assertEquals("required", errors.getFieldError("type").getCode());
	}

	@Test
	@DisplayName("U-06 limit: an already persisted pet may omit the type")
	void persistedPetWithoutTypeIsAccepted() {
		Pet pet = validPet();
		pet.setId(7);
		pet.setType(null);

		Errors errors = validate(pet);

		assertFalse(pet.isNew(), "Precondition: a pet with id is not new");
		assertFalse(errors.hasFieldErrors("type"), "The type is only mandatory for new pets");
	}

	@Test
	@DisplayName("U-07 invalid: a missing birth date is rejected")
	void missingBirthDateIsRejected() {
		Pet pet = validPet();
		pet.setBirthDate(null);

		Errors errors = validate(pet);

		assertTrue(errors.hasFieldErrors("birthDate"));
		assertEquals("required", errors.getFieldError("birthDate").getCode());
	}

}
