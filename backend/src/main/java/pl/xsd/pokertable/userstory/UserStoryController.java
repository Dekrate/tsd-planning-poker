package pl.xsd.pokertable.userstory;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/user-stories")
@AllArgsConstructor
public class UserStoryController {

	private final UserStoryService userStoryService;

	@PostMapping
	public ResponseEntity<UserStoryDto> createUserStory(@RequestParam Long pokerTableId, @RequestBody UserStory userStory) { // Zmieniono zwracany typ na UserStoryDto
		UserStoryDto createdStory = userStoryService.createUserStory(pokerTableId, userStory); // Serwis już zwraca DTO
		return ResponseEntity.status(HttpStatus.CREATED).body(createdStory);
	}

	@GetMapping("/{storyId}")
	public ResponseEntity<UserStoryDto> getUserStoryById(@PathVariable Long storyId) { // Zmieniono zwracany typ na UserStoryDto
		UserStoryDto userStory = userStoryService.getUserStoryById(storyId); // Serwis już zwraca DTO
		return ResponseEntity.ok(userStory);
	}

	@GetMapping("/table/{tableId}")
	public ResponseEntity<Set<UserStoryDto>> getUserStoriesForTable(@PathVariable Long tableId) { // Zmieniono zwracany typ na Set<UserStoryDto>
		Set<UserStoryDto> userStories = userStoryService.getUserStoriesForTable(tableId); // Serwis już zwraca Set<UserStoryDto>
		return ResponseEntity.ok(userStories);
	}

	@PutMapping("/{storyId}")
	public ResponseEntity<UserStoryDto> updateUserStory(@PathVariable Long storyId, @RequestBody UserStory userStory) { // Zmieniono zwracany typ na UserStoryDto
		UserStoryDto updatedStory = userStoryService.updateUserStory(storyId, userStory); // Serwis już zwraca DTO
		return ResponseEntity.ok(updatedStory);
	}

	@DeleteMapping("/{storyId}")
	public ResponseEntity<Void> deleteUserStory(@PathVariable Long storyId) {
		userStoryService.deleteUserStory(storyId);
		return ResponseEntity.noContent().build();
	}
}
