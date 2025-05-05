package pl.xsd.pokertable.userstory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserStoryController.class)
class UserStoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserStoryService userStoryService;

	@Autowired
	private ObjectMapper objectMapper;

	private PokerTable pokerTable;
	private UserStory userStory;

	@BeforeEach
	void setUp() {
		pokerTable = new PokerTable();
		pokerTable.setId(1L);
		pokerTable.setName("Test Table");

		userStory = new UserStory();
		userStory.setId(10L);
		userStory.setTitle("As a user, I want...");
		userStory.setDescription("...");
		userStory.setEstimatedPoints(5);
		userStory.setPokerTable(pokerTable); // This won't be serialized due to @JsonIgnore
	}

	@Test
	void createUserStory_shouldReturnCreatedStory_whenSuccessful() throws Exception {
		// Arrange
		Long tableId = pokerTable.getId();
		UserStory userStoryToCreate = new UserStory("New Story", "Details");
		userStoryToCreate.setEstimatedPoints(3); // Include estimated points

		UserStory createdStory = new UserStory("New Story", "Details");
		createdStory.setId(11L);
		createdStory.setEstimatedPoints(3);


		when(userStoryService.createUserStory(eq(tableId), any(UserStory.class))).thenReturn(createdStory);

		// Act & Assert
		mockMvc.perform(post("/user-stories")
						.param("pokerTableId", tableId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(userStoryToCreate)))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id", is(11)))
				.andExpect(jsonPath("$.title", is("New Story")))
				.andExpect(jsonPath("$.description", is("Details")))
				.andExpect(jsonPath("$.estimatedPoints", is(3))); // Assert estimated points

		verify(userStoryService).createUserStory(eq(tableId), any(UserStory.class));
	}

	@Test
	void createUserStory_shouldReturnNotFound_whenPokerTableDoesNotExist() throws Exception {
		// Arrange
		Long nonExistentTableId = 99L;
		UserStory userStoryToCreate = new UserStory("New Story", "Details");
		userStoryToCreate.setEstimatedPoints(3);

		when(userStoryService.createUserStory(eq(nonExistentTableId), any(UserStory.class)))
				.thenThrow(new NotFoundException("Poker table not found with ID: " + nonExistentTableId));

		// Act & Assert
		mockMvc.perform(post("/user-stories")
						.param("pokerTableId", nonExistentTableId.toString())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(userStoryToCreate)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("Poker table not found with ID: " + nonExistentTableId))); // Verify error response format
		verify(userStoryService).createUserStory(eq(nonExistentTableId), any(UserStory.class));
	}

	@Test
	void getUserStoryById_shouldReturnUserStory_whenStoryExists() throws Exception {
		// Arrange
		Long storyId = userStory.getId();
		when(userStoryService.getUserStoryById(storyId)).thenReturn(userStory);

		// Act & Assert
		mockMvc.perform(get("/user-stories/{storyId}", storyId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id", is(storyId.intValue())))
				.andExpect(jsonPath("$.title", is("As a user, I want...")))
				.andExpect(jsonPath("$.description", is("...")))
				.andExpect(jsonPath("$.estimatedPoints", is(5))); // Assert estimated points

		verify(userStoryService).getUserStoryById(storyId);
	}

	@Test
	void getUserStoryById_shouldReturnNotFound_whenStoryDoesNotExist() throws Exception {
		// Arrange
		Long nonExistentStoryId = 99L;
		when(userStoryService.getUserStoryById(nonExistentStoryId))
				.thenThrow(new NotFoundException("User story not found with ID: " + nonExistentStoryId));

		// Act & Assert
		mockMvc.perform(get("/user-stories/{storyId}", nonExistentStoryId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("User story not found with ID: " + nonExistentStoryId))); // Verify error response format

		verify(userStoryService).getUserStoryById(nonExistentStoryId);
	}

	@Test
	void getUserStoriesForTable_shouldReturnSetOfUserStories_whenTableHasStories() throws Exception {
		// Arrange
		Long tableId = pokerTable.getId();
		Set<UserStory> stories = new HashSet<>();
		stories.add(userStory);
		stories.add(new UserStory(11L, "Another Story", "More Details", 8, pokerTable)); // Use all-args constructor (requires adding one if not present)

		when(userStoryService.getUserStoriesForTable(tableId)).thenReturn(stories);

		// Act & Assert
		mockMvc.perform(get("/user-stories/table/{tableId}", tableId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[*].id", containsInAnyOrder(10, 11)))
				.andExpect(jsonPath("$[*].title", containsInAnyOrder("As a user, I want...", "Another Story")))
				.andExpect(jsonPath("$[*].estimatedPoints", containsInAnyOrder(5, 8))); // Assert estimated points

		verify(userStoryService).getUserStoriesForTable(tableId);
	}

	@Test
	void getUserStoriesForTable_shouldReturnEmptySet_whenTableHasNoStories() throws Exception {
		// Arrange
		Long tableId = pokerTable.getId();
		Set<UserStory> stories = new HashSet<>();
		when(userStoryService.getUserStoriesForTable(tableId)).thenReturn(stories);

		// Act & Assert
		mockMvc.perform(get("/user-stories/table/{tableId}", tableId))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(0))); // Assert empty array

		verify(userStoryService).getUserStoriesForTable(tableId);
	}

	@Test
	void updateUserStory_shouldReturnUpdatedUserStory_whenSuccessful() throws Exception {
		// Arrange
		Long storyId = userStory.getId();
		UserStory updatedDetails = new UserStory();
		updatedDetails.setTitle("Title Updated");
		updatedDetails.setDescription("Description Updated");
		updatedDetails.setEstimatedPoints(13); // Update estimated points

		UserStory updatedStory = new UserStory();
		updatedStory.setId(storyId);
		updatedStory.setTitle("Title Updated");
		updatedStory.setDescription("Description Updated");
		updatedStory.setEstimatedPoints(13);
		updatedStory.setPokerTable(pokerTable);

		when(userStoryService.updateUserStory(eq(storyId), any(UserStory.class))).thenReturn(updatedStory);

		// Act & Assert
		mockMvc.perform(put("/user-stories/{storyId}", storyId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedDetails)))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id", is(storyId.intValue())))
				.andExpect(jsonPath("$.title", is("Title Updated")))
				.andExpect(jsonPath("$.description", is("Description Updated")))
				.andExpect(jsonPath("$.estimatedPoints", is(13))); // Assert updated estimated points

		verify(userStoryService).updateUserStory(eq(storyId), any(UserStory.class));
	}

	@Test
	void updateUserStory_shouldReturnNotFound_whenStoryDoesNotExist() throws Exception {
		// Arrange
		Long nonExistentStoryId = 99L;
		UserStory updatedDetails = new UserStory();
		updatedDetails.setTitle("Title Updated");

		when(userStoryService.updateUserStory(eq(nonExistentStoryId), any(UserStory.class)))
				.thenThrow(new NotFoundException("User story not found with ID: " + nonExistentStoryId));

		// Act & Assert
		mockMvc.perform(put("/user-stories/{storyId}", nonExistentStoryId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(updatedDetails)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("User story not found with ID: " + nonExistentStoryId))); // Verify error response format

		verify(userStoryService).updateUserStory(eq(nonExistentStoryId), any(UserStory.class));
	}


	@Test
	void deleteUserStory_shouldReturnNoContent_whenSuccessful() throws Exception {
		// Arrange
		Long storyId = userStory.getId();
		doNothing().when(userStoryService).deleteUserStory(storyId);

		// Act & Assert
		mockMvc.perform(delete("/user-stories/{storyId}", storyId))
				.andExpect(status().isNoContent());

		verify(userStoryService).deleteUserStory(storyId);
	}

	@Test
	void deleteUserStory_shouldReturnNotFound_whenStoryDoesNotExist() throws Exception {
		// Arrange
		Long nonExistentStoryId = 99L;
		doThrow(new NotFoundException("User story not found with ID: " + nonExistentStoryId))
				.when(userStoryService).deleteUserStory(nonExistentStoryId);

		// Act & Assert
		mockMvc.perform(delete("/user-stories/{storyId}", nonExistentStoryId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message", is("User story not found with ID: " + nonExistentStoryId))); // Verify error response format

		verify(userStoryService).deleteUserStory(nonExistentStoryId);
	}

	// Helper constructor for UserStory with ID and pokerTable (for test data setup)
	// Add this to your UserStory entity if you need it for test data setup.
	// public UserStory(Long id, String title, String description, Integer estimatedPoints, PokerTable pokerTable) {
	//     this.id = id;
	//     this.title = title;
	//     this.description = description;
	//     this.estimatedPoints = estimatedPoints;
	//     this.pokerTable = pokerTable;
	// }
}