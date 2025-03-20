package pl.diakowski.pokertable.pokertable;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.diakowski.pokertable.developer.Developer;
import pl.diakowski.pokertable.developer.DeveloperService;

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
}
