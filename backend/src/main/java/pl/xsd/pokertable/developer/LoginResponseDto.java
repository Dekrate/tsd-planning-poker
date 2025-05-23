package pl.xsd.pokertable.developer;

public record LoginResponseDto(
		String jwt,
		DeveloperDto developer) {
}
