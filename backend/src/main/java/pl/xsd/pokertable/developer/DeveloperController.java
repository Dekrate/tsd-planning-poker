package pl.xsd.pokertable.developer;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/developers")
@AllArgsConstructor
public class DeveloperController {

	private final DeveloperService developerService;

	@PatchMapping("/{developerId}/vote")
	public ResponseEntity<Void> vote(@PathVariable Long developerId, @RequestParam Long tableId, @RequestParam int vote) {
		try {
			developerService.vote(developerId, tableId, vote);
			return ResponseEntity.noContent().build();
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
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
	public ResponseEntity<Map<String, Object>> joinTable(@RequestParam String name, HttpSession session) {
		Map<String, Object> result = developerService.joinTable(name, session);
		return ResponseEntity.ok(result);
	}
}
