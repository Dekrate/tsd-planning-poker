package pl.xsd.pokertable.userstory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserStoryServiceTest {

	@Mock
	private UserStoryRepository userStoryRepository;

	@Mock
	private PokerTableRepository pokerTableRepository;

	@InjectMocks
	private UserStoryService userStoryService;

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
		userStory.setPokerTable(pokerTable);
	}

	@Test
	void createUserStory_shouldReturnSavedUserStory_whenPokerTableExists() {
		UserStory newUserStory = new UserStory("New Story", "Details");
		Long tableId = pokerTable.getId();

		when(pokerTableRepository.findById(tableId)).thenReturn(Optional.of(pokerTable));
		when(userStoryRepository.save(any(UserStory.class))).thenReturn(newUserStory);
		UserStory createdStory = userStoryService.createUserStory(tableId, newUserStory);
		assertNotNull(createdStory);
		assertEquals(newUserStory.getTitle(), createdStory.getTitle());
		assertEquals(newUserStory.getDescription(), createdStory.getDescription());
		assertEquals(pokerTable, createdStory.getPokerTable());
		verify(pokerTableRepository).findById(tableId);
		verify(userStoryRepository).save(newUserStory);
	}

	@Test
	void createUserStory_shouldThrowNotFoundException_whenPokerTableDoesNotExist() {
		UserStory newUserStory = new UserStory("New Story", "Details");
		Long nonExistentTableId = 99L;

		when(pokerTableRepository.findById(nonExistentTableId)).thenReturn(Optional.empty());
		NotFoundException exception = assertThrows(NotFoundException.class,
				() -> userStoryService.createUserStory(nonExistentTableId, newUserStory));
		assertEquals("Poker table not found with ID: " + nonExistentTableId, exception.getMessage());
		verify(pokerTableRepository).findById(nonExistentTableId);
		verify(userStoryRepository, never()).save(any(UserStory.class));
	}

	@Test
	void getUserStoryById_shouldReturnUserStory_whenStoryExists() {
		Long storyId = userStory.getId();
		when(userStoryRepository.findById(storyId)).thenReturn(Optional.of(userStory));
		UserStory foundStory = userStoryService.getUserStoryById(storyId);
		assertNotNull(foundStory);
		assertEquals(userStory.getId(), foundStory.getId());
		assertEquals(userStory.getTitle(), foundStory.getTitle());
		assertEquals(userStory.getEstimatedPoints(), foundStory.getEstimatedPoints());
		verify(userStoryRepository).findById(storyId);
	}

	@Test
	void getUserStoryById_shouldThrowNotFoundException_whenStoryDoesNotExist() {
		Long nonExistentStoryId = 99L;
		when(userStoryRepository.findById(nonExistentStoryId)).thenReturn(Optional.empty());
		NotFoundException exception = assertThrows(NotFoundException.class,
				() -> userStoryService.getUserStoryById(nonExistentStoryId));
		assertEquals("User story not found with ID: " + nonExistentStoryId, exception.getMessage());
		verify(userStoryRepository).findById(nonExistentStoryId);
	}

	@Test
	void getUserStoriesForTable_shouldReturnSetOfUserStories_whenTableHasStories() {
		Long tableId = pokerTable.getId();
		Set<UserStory> stories = new HashSet<>();
		stories.add(userStory);

		when(userStoryRepository.findByPokerTableId(tableId)).thenReturn(stories);
		Set<UserStory> foundStories = userStoryService.getUserStoriesForTable(tableId);
		assertNotNull(foundStories);
		assertFalse(foundStories.isEmpty());
		assertEquals(1, foundStories.size());
		assertTrue(foundStories.contains(userStory));
		verify(userStoryRepository).findByPokerTableId(tableId);
	}

	@Test
	void getUserStoriesForTable_shouldReturnEmptySet_whenTableHasNoStories() {
		Long tableId = pokerTable.getId();
		Set<UserStory> stories = new HashSet<>();

		when(userStoryRepository.findByPokerTableId(tableId)).thenReturn(stories);
		Set<UserStory> foundStories = userStoryService.getUserStoriesForTable(tableId);
		assertNotNull(foundStories);
		assertTrue(foundStories.isEmpty());
		verify(userStoryRepository).findByPokerTableId(tableId);
	}

	@Test
	void updateUserStory_shouldReturnUpdatedUserStory_whenStoryExists() {
		Long storyId = userStory.getId();
		UserStory updatedDetails = new UserStory();
		updatedDetails.setTitle("Updated Title");
		updatedDetails.setDescription("Updated Description");
		updatedDetails.setEstimatedPoints(8);

		when(userStoryRepository.findById(storyId)).thenReturn(Optional.of(userStory));
		when(userStoryRepository.save(any(UserStory.class))).thenReturn(userStory);
		UserStory resultStory = userStoryService.updateUserStory(storyId, updatedDetails);
		assertNotNull(resultStory);
		assertEquals(storyId, resultStory.getId());
		assertEquals("Updated Title", resultStory.getTitle());
		assertEquals("Updated Description", resultStory.getDescription());
		assertEquals(8, resultStory.getEstimatedPoints());
		assertEquals(pokerTable, resultStory.getPokerTable());
		verify(userStoryRepository).findById(storyId);
		verify(userStoryRepository).save(userStory);
	}

	@Test
	void updateUserStory_shouldThrowNotFoundException_whenStoryDoesNotExist() {
		Long nonExistentStoryId = 99L;
		UserStory updatedDetails = new UserStory();
		updatedDetails.setTitle("Updated Title");

		when(userStoryRepository.findById(nonExistentStoryId)).thenReturn(Optional.empty());
		NotFoundException exception = assertThrows(NotFoundException.class,
				() -> userStoryService.updateUserStory(nonExistentStoryId, updatedDetails));
		assertEquals("User story not found with ID: " + nonExistentStoryId, exception.getMessage());
		verify(userStoryRepository).findById(nonExistentStoryId);
		verify(userStoryRepository, never()).save(any(UserStory.class));
	}

	@Test
	void deleteUserStory_shouldCallDeleteById_whenStoryExists() {
		Long storyId = userStory.getId();
		when(userStoryRepository.existsById(storyId)).thenReturn(true);
		doNothing().when(userStoryRepository).deleteById(storyId);
		userStoryService.deleteUserStory(storyId);
		verify(userStoryRepository).existsById(storyId);
		verify(userStoryRepository).deleteById(storyId);
	}

	@Test
	void deleteUserStory_shouldThrowNotFoundException_whenStoryDoesNotExist() {
		Long nonExistentStoryId = 99L;
		when(userStoryRepository.existsById(nonExistentStoryId)).thenReturn(false);
		NotFoundException exception = assertThrows(NotFoundException.class,
				() -> userStoryService.deleteUserStory(nonExistentStoryId));
		assertEquals("User story not found with ID: " + nonExistentStoryId, exception.getMessage());
		verify(userStoryRepository).existsById(nonExistentStoryId);
		verify(userStoryRepository, never()).deleteById(anyLong());
	}
}
