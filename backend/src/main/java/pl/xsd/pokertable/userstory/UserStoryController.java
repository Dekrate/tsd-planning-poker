package pl.xsd.pokertable.userstory;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/user-stories")
@AllArgsConstructor
public class UserStoryController {

	private final UserStoryService userStoryService;

	@PostMapping
	public ResponseEntity<UserStoryDto> createUserStory(@RequestParam Long pokerTableId, @RequestBody UserStory userStory) {
		UserStory createdStory = userStoryService.createUserStory(pokerTableId, userStory);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdStory.toDto());
	}

	@GetMapping("/{storyId}")
	public ResponseEntity<UserStoryDto> getUserStoryById(@PathVariable Long storyId) {
		UserStory userStory = userStoryService.getUserStoryById(storyId);
		return ResponseEntity.ok(userStory.toDto());
	}

	@GetMapping("/table/{tableId}")
	public ResponseEntity<Set<UserStoryDto>> getUserStoriesForTable(@PathVariable Long tableId) {
		Set<UserStory> userStories = userStoryService.getUserStoriesForTable(tableId);
		return ResponseEntity.ok(userStories.stream().map(UserStory::toDto).collect(Collectors.toUnmodifiableSet()));
	}

	@PutMapping("/{storyId}")
	public ResponseEntity<UserStoryDto> updateUserStory(@PathVariable Long storyId, @RequestBody UserStory userStory) {
		UserStory updatedStory = userStoryService.updateUserStory(storyId, userStory);
		return ResponseEntity.ok(updatedStory.toDto());
	}

	@DeleteMapping("/{storyId}")
	public ResponseEntity<Void> deleteUserStory(@PathVariable Long storyId) {
		userStoryService.deleteUserStory(storyId);
		return ResponseEntity.noContent().build();
	}
}
