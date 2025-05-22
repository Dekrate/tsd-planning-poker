package pl.xsd.pokertable.developer;

public record LoginResponseDto(
		String jwt,
		Developer developer) {
}
