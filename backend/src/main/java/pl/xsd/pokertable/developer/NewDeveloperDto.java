package pl.xsd.pokertable.developer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NewDeveloperDto(
		@NotBlank
		String name,
		@Email
		String email,
		@NotBlank
		String password
) {
}
