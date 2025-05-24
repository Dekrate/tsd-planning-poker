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

import java.util.Collections;
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

	private PokerTable testPokerTable;
	private UserStory testUserStory;

	@BeforeEach
	void setUp() {
		testPokerTable = new PokerTable(1L, "Test Table", false);
		testUserStory = new UserStory(100L, "Test Story", "Description", 5, testPokerTable);
	}

	@Test
	void createUserStory_shouldCreateAndReturnUserStory() {
		UserStory newUserStory = new UserStory(null, "New Story", "New Desc", 8, null);
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		when(userStoryRepository.save(any(UserStory.class))).thenAnswer(invocation -> {
			UserStory us = invocation.getArgument(0);
			us.setId(101L);
			return us;
		});

		UserStory result = userStoryService.createUserStory(testPokerTable.getId(), newUserStory);

		assertNotNull(result);
		assertEquals(101L, result.getId());
		assertEquals("New Story", result.getTitle());
		assertEquals(8, result.getEstimatedPoints());
		assertEquals(testPokerTable, result.getPokerTable());
		verify(userStoryRepository, times(1)).save(newUserStory);
	}

	@Test
	void createUserStory_shouldThrowNotFoundExceptionIfTableNotFound() {
		UserStory newUserStory = new UserStory(null, "New Story", "New Desc", 8, null);
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				userStoryService.createUserStory(99L, newUserStory));
	}

	@Test
	void getUserStoryById_shouldReturnUserStory() {
		when(userStoryRepository.findById(testUserStory.getId())).thenReturn(Optional.of(testUserStory));

		UserStory result = userStoryService.getUserStoryById(testUserStory.getId());

		assertNotNull(result);
		assertEquals(testUserStory.getId(), result.getId());
		assertEquals(testUserStory.getTitle(), result.getTitle());
	}

	@Test
	void getUserStoryById_shouldThrowNotFoundException() {
		when(userStoryRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				userStoryService.getUserStoryById(99L));
	}

	@Test
	void getUserStoriesForTable_shouldReturnSetOfUserStories() {
		UserStory story2 = new UserStory(101L, "Story 2", "Desc 2", 13, testPokerTable);
		Set<UserStory> stories = new HashSet<>(Set.of(testUserStory, story2));

		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		when(userStoryRepository.findByPokerTable(testPokerTable)).thenReturn(stories);

		Set<UserStory> result = userStoryService.getUserStoriesForTable(testPokerTable.getId());

		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.stream().anyMatch(s -> s.getId().equals(testUserStory.getId())));
		assertTrue(result.stream().anyMatch(s -> s.getId().equals(story2.getId())));
	}

	@Test
	void getUserStoriesForTable_shouldReturnEmptySetIfNoStories() {
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		when(userStoryRepository.findByPokerTable(testPokerTable)).thenReturn(Collections.emptySet());

		Set<UserStory> result = userStoryService.getUserStoriesForTable(testPokerTable.getId());

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getUserStoriesForTable_shouldThrowNotFoundExceptionIfTableNotFound() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				userStoryService.getUserStoriesForTable(99L));
	}

	@Test
	void updateUserStory_shouldUpdateAndReturnUserStory() {
		UserStory updatedDetails = new UserStory(null, "Updated Title", "Updated Desc", 21, null);
		when(userStoryRepository.findById(testUserStory.getId())).thenReturn(Optional.of(testUserStory));
		when(userStoryRepository.save(any(UserStory.class))).thenReturn(testUserStory);

		UserStory result = userStoryService.updateUserStory(testUserStory.getId(), updatedDetails);

		assertNotNull(result);
		assertEquals(testUserStory.getId(), result.getId());
		assertEquals("Updated Title", result.getTitle());
		assertEquals("Updated Desc", result.getDescription());
		assertEquals(21, result.getEstimatedPoints());
		verify(userStoryRepository, times(1)).save(testUserStory);
		// Usunięto weryfikację wysyłania wiadomości WebSocket
		// verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/table/1/user-stories"), any(UserStoryDto.class));
	}

	@Test
	void updateUserStory_shouldThrowNotFoundExceptionIfStoryNotFound() {
		UserStory updatedDetails = new UserStory(null, "Updated Title", "Updated Desc", 21, null);
		when(userStoryRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				userStoryService.updateUserStory(99L, updatedDetails));
	}

	@Test
	void deleteUserStory_shouldDeleteStory() {
		when(userStoryRepository.findById(testUserStory.getId())).thenReturn(Optional.of(testUserStory));
		doNothing().when(userStoryRepository).delete(testUserStory);

		userStoryService.deleteUserStory(testUserStory.getId());

		verify(userStoryRepository, times(1)).delete(testUserStory);
	}

	@Test
	void deleteUserStory_shouldThrowNotFoundExceptionIfStoryNotFound() {
		when(userStoryRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				userStoryService.deleteUserStory(99L));
	}
}
