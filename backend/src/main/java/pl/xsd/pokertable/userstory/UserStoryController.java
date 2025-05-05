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
	public ResponseEntity<UserStory> createUserStory(@RequestParam Long pokerTableId, @RequestBody UserStory userStory) {
		UserStory createdUserStory = userStoryService.createUserStory(pokerTableId, userStory);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdUserStory);
	}

	@GetMapping("/{storyId}")
	public ResponseEntity<UserStory> getUserStoryById(@PathVariable Long storyId) {
		UserStory userStory = userStoryService.getUserStoryById(storyId);
		return ResponseEntity.ok(userStory);
	}

	@GetMapping("/table/{tableId}")
	public ResponseEntity<Set<UserStory>> getUserStoriesForTable(@PathVariable Long tableId) {
		Set<UserStory> userStories = userStoryService.getUserStoriesForTable(tableId);
		return ResponseEntity.ok(userStories);
	}

	@PutMapping("/{storyId}")
	public ResponseEntity<UserStory> updateUserStory(@PathVariable Long storyId, @RequestBody UserStory userStory) {
		UserStory updatedStory = userStoryService.updateUserStory(storyId, userStory);
		return ResponseEntity.ok(updatedStory);
	}

	@DeleteMapping("/{storyId}")
	public ResponseEntity<Void> deleteUserStory(@PathVariable Long storyId) {
		userStoryService.deleteUserStory(storyId);
		return ResponseEntity.noContent().build();
	}
}