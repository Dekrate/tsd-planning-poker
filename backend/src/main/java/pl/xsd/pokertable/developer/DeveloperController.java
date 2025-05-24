package pl.xsd.pokertable.developer;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/developers")
@AllArgsConstructor
public class DeveloperController {

	private final DeveloperService developerService;

	@PatchMapping("/{developerId}/vote")
	public ResponseEntity<Void> vote(@PathVariable Long developerId, @RequestParam Long tableId, @RequestParam Integer vote) {
		developerService.vote(developerId, tableId, vote);
		return ResponseEntity.noContent().build();
	}

	@PostMapping
	public ResponseEntity<DeveloperDto> createDeveloper(@RequestParam Long pokerTableId, @RequestBody Developer developer) {
		Developer createdDeveloper = developerService.createDeveloper(pokerTableId, developer);
		return ResponseEntity.ok(new DeveloperDto(createdDeveloper));
	}

	@GetMapping("/{developerId}/has-voted")
	public ResponseEntity<Boolean> hasVoted(@PathVariable Long developerId) {
		boolean hasVoted = developerService.hasVoted(developerId);
		return ResponseEntity.ok(hasVoted);
	}

	@GetMapping("/{developerId}")
	public ResponseEntity<DeveloperDto> getDeveloper(@PathVariable Long developerId) {
		DeveloperDto developer = developerService.getDeveloper(developerId);
		return ResponseEntity.ok(developer);
	}

	@GetMapping("/poker-table/{tableId}")
	public ResponseEntity<Set<DeveloperDto>> getAllDevelopers(@PathVariable Long tableId) {
		Set<DeveloperDto> developers = developerService.getDevelopersForPokerTable(tableId);
		return ResponseEntity.ok(developers);
	}

	@PostMapping("/join")
	public ResponseEntity<JoinResponseDto> joinTable(@RequestParam Long tableId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication != null && authentication.isAuthenticated() && !(authentication.getPrincipal() instanceof String && authentication.getPrincipal().equals("anonymousUser"))) {
			String email = authentication.getName();
			return ResponseEntity.ok(developerService.joinTable(email, tableId));
		} else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
	}

	@PostMapping("/register")
	public ResponseEntity<DeveloperDto> registerDeveloper(@RequestBody @Valid NewDeveloperDto developerDto) {
		DeveloperDto registeredDeveloper = developerService.registerDeveloper(developerDto.name(), developerDto.email(), developerDto.password());
		return ResponseEntity.status(HttpStatus.CREATED).body(registeredDeveloper);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponseDto> loginDeveloper(@RequestBody Map<String, String> loginRequest) {
		String email = loginRequest.get("email");
		String password = loginRequest.get("password");
		if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
			throw new IllegalArgumentException("Email and password must be provided");
		}

		String jwt = developerService.loginDeveloper(email, password);
		Developer developer = developerService.getDeveloperByEmail(email);
		return ResponseEntity.ok(new LoginResponseDto(jwt, new DeveloperDto(developer)));
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		SecurityContextHolder.clearContext();
		return ResponseEntity.ok().build();
	}
}
