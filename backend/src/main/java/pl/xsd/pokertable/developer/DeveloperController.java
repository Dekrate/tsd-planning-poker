package pl.xsd.pokertable.developer;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.xsd.pokertable.participation.Participation;

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
	public ResponseEntity<Developer> createDeveloper(@RequestParam Long pokerTableId, @RequestBody Developer developer) {
		Developer createdDeveloper = developerService.createDeveloper(pokerTableId, developer);
		return ResponseEntity.ok(createdDeveloper);
	}

	@GetMapping("/{developerId}/has-voted")
	public ResponseEntity<Boolean> hasVoted(@PathVariable Long developerId) {
		boolean hasVoted = developerService.hasVoted(developerId);
		return ResponseEntity.ok(hasVoted);
	}

	@GetMapping("/{developerId}")
	public ResponseEntity<Developer> getDeveloper(@PathVariable Long developerId) {
		Developer developer = developerService.getDeveloper(developerId);
		return ResponseEntity.ok(developer);
	}

	@GetMapping("/poker-table/{tableId}")
	public ResponseEntity<Set<Developer>> getAllDevelopers(@PathVariable Long tableId) {
		Set<Developer> developers = developerService.getDevelopersForPokerTable(tableId);
		return ResponseEntity.ok(developers);
	}

	@PostMapping("/join")
	public ResponseEntity<Map<String, Object>> joinTable(@RequestParam String name, @RequestParam Long tableId, HttpSession session) {
		return ResponseEntity.ok(developerService.joinTable(name, tableId, session));
	}

	@PostMapping("/register")
	public ResponseEntity<Developer> registerDeveloper(@RequestBody @Valid NewDeveloperDto developerDto) {
		Developer registeredDeveloper = developerService.registerDeveloper(developerDto.name(), developerDto.email(), developerDto.password());
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
		return ResponseEntity.ok(new LoginResponseDto(jwt, developer));
	}

	@GetMapping("/{developerId}/history")
	public ResponseEntity<Set<Participation>> getDeveloperHistory(@PathVariable Long developerId) {
		Set<Participation> history = developerService.getDeveloperParticipationHistory(developerId);
		return ResponseEntity.ok(history);

	}
	@PostMapping("/logout")
	public ResponseEntity<Void> logout() {
		return ResponseEntity.ok().build();
	}
}
