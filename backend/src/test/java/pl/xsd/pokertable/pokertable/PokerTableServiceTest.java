package pl.xsd.pokertable.pokertable;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.developer.DeveloperRepository;
import pl.xsd.pokertable.exception.NotEveryoneVotedException;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.userstory.UserStory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PokerTableServiceTest {

	@Mock
	private PokerTableRepository pokerTableRepository;
	@Mock
	private DeveloperRepository developerRepository;
	@Mock
	private ObjectMapper objectMapper;

	@InjectMocks
	private PokerTableService pokerTableService;

	private PokerTable activeTable;
	private PokerTable closedTable;
	private Developer dev1;
	private Developer dev2;

	@BeforeEach
	void setUp() {
		activeTable = new PokerTable(1L, "Active Table", false);
		closedTable = new PokerTable(2L, "Closed Table", true);

		dev1 = new Developer(10L, "Dev1", "dev1@example.com", "pass1", activeTable);
		dev2 = new Developer(11L, "Dev2", "dev2@example.com", "pass2", activeTable);

		activeTable.getDevelopers().add(dev1);
		activeTable.getDevelopers().add(dev2);

		UserStory story1 = new UserStory(100L, "Story A", "Desc A", 5, activeTable);
		activeTable.getUserStories().add(story1);
	}

	@Test
	void createPokerTable_shouldCreateAndSaveNewTable() {
		String tableName = "New Test Table";

		when(pokerTableRepository.save(any(PokerTable.class))).thenReturn(new PokerTable(3L, tableName, false));

		PokerTable createdTable = pokerTableService.createPokerTable(tableName);

		assertNotNull(createdTable);
		assertEquals(3L, createdTable.getId());
		assertEquals(tableName, createdTable.getName());
		assertFalse(createdTable.getIsClosed());
		verify(pokerTableRepository, times(1)).save(any(PokerTable.class));
	}

	@Test
	void closePokerTable_shouldCloseTableAndResetVotes() {
		dev1.setVote(5);
		dev2.setVote(8);

		when(pokerTableRepository.findById(activeTable.getId())).thenReturn(Optional.of(activeTable));
		when(developerRepository.save(any(Developer.class))).thenReturn(dev1, dev2);
		when(pokerTableRepository.save(any(PokerTable.class))).thenReturn(activeTable);

		pokerTableService.closePokerTable(activeTable.getId());

		assertTrue(activeTable.getIsClosed());
		assertNull(dev1.getVote());
		assertNull(dev2.getVote());
		assertTrue(dev1.getPastTables().contains(activeTable));
		assertTrue(dev2.getPastTables().contains(activeTable));
		verify(developerRepository, times(2)).save(any(Developer.class));
		verify(pokerTableRepository, times(1)).save(activeTable);
	}

	@Test
	void closePokerTable_shouldThrowNotFoundExceptionIfTableNotFound() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				pokerTableService.closePokerTable(99L));
	}

	@Test
	void closePokerTable_shouldThrowNotEveryoneVotedExceptionIfNotAllVoted() {
		dev1.setVote(5);
		when(pokerTableRepository.findById(activeTable.getId())).thenReturn(Optional.of(activeTable));

		assertThrows(NotEveryoneVotedException.class, () ->
				pokerTableService.closePokerTable(activeTable.getId()));
	}

	@Test
	void closePokerTable_shouldThrowNotEveryoneVotedExceptionIfNoDevelopers() {
		activeTable.getDevelopers().clear();
		when(pokerTableRepository.findById(activeTable.getId())).thenReturn(Optional.of(activeTable));

		assertThrows(NotEveryoneVotedException.class, () ->
				pokerTableService.closePokerTable(activeTable.getId()));
	}

	@Test
	void getActiveTable_shouldReturnExistingActiveTable() {
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.of(activeTable));

		PokerTable result = pokerTableService.getActiveTable();

		assertNotNull(result);
		assertEquals(activeTable.getId(), result.getId());
		assertFalse(result.getIsClosed());
	}

	@Test
	void getActiveTable_shouldCreateNewTableIfNoActiveTable() {
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty());
		when(pokerTableRepository.save(any(PokerTable.class))).thenReturn(new PokerTable(3L, "Default Table", false));

		PokerTable result = pokerTableService.getActiveTable();

		assertNotNull(result);
		assertEquals(3L, result.getId());
		assertEquals("Default Table", result.getName());
		assertFalse(result.getIsClosed());
		verify(pokerTableRepository, times(1)).save(any(PokerTable.class));
	}

	@Test
	void getTableById_shouldReturnTable() {
		when(pokerTableRepository.findById(activeTable.getId())).thenReturn(Optional.of(activeTable));

		PokerTable result = pokerTableService.getTableById(activeTable.getId());

		assertNotNull(result);
		assertEquals(activeTable.getId(), result.getId());
	}

	@Test
	void getTableById_shouldThrowNotFoundException() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				pokerTableService.getTableById(99L));
	}

	@Test
	void getAllActiveTables_shouldReturnListOfActiveTables() {
		PokerTable anotherActiveTable = new PokerTable(3L, "Another Active", false);
		when(pokerTableRepository.findAllByIsClosedFalse()).thenReturn(Set.of(activeTable, anotherActiveTable));

		Set<PokerTable> result = pokerTableService.getAllActiveTables();

		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.stream().anyMatch(t -> t.getId().equals(activeTable.getId())));
		assertTrue(result.stream().anyMatch(t -> t.getId().equals(anotherActiveTable.getId())));
	}

	@Test
	void getAllActiveTables_shouldReturnEmptyListIfNoActiveTables() {
		when(pokerTableRepository.findAllByIsClosedFalse()).thenReturn(Collections.emptySet());

		Set<PokerTable> result = pokerTableService.getAllActiveTables();

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getPastTablesForDeveloper_shouldReturnClosedTablesForDeveloper() {
		Developer devWithPastTables = new Developer(12L, "Past Dev", "past@example.com", "pass", null);
		PokerTable pastTable1 = new PokerTable(4L, "Past 1", true);
		PokerTable pastTable2 = new PokerTable(5L, "Past 2", true);
		PokerTable activeTableForDev = new PokerTable(6L, "Active For Dev", false); // Should be filtered out

		devWithPastTables.addPastTable(pastTable1);
		devWithPastTables.addPastTable(pastTable2);
		devWithPastTables.addPastTable(activeTableForDev); // This one should not be returned

		when(developerRepository.findById(devWithPastTables.getId())).thenReturn(Optional.of(devWithPastTables));

		Set<PokerTable> result = pokerTableService.getPastTablesForDeveloper(devWithPastTables.getId());

		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.stream().anyMatch(t -> t.getId().equals(pastTable1.getId())));
		assertTrue(result.stream().anyMatch(t -> t.getId().equals(pastTable2.getId())));
		assertFalse(result.stream().anyMatch(t -> t.getId().equals(activeTableForDev.getId())));
	}

	@Test
	void getPastTablesForDeveloper_shouldReturnEmptyListIfNoPastTables() {
		Developer devNoPastTables = new Developer(13L, "No Past Dev", "nopast@example.com", "pass", null);
		when(developerRepository.findById(devNoPastTables.getId())).thenReturn(Optional.of(devNoPastTables));

		Set<PokerTable> result = pokerTableService.getPastTablesForDeveloper(devNoPastTables.getId());

		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void getPastTablesForDeveloper_shouldThrowNotFoundExceptionIfDeveloperNotFound() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				pokerTableService.getPastTablesForDeveloper(99L));
	}

	@Test
	void exportUserStoriesToCsv_shouldReturnCsvBytes() {
		Set<UserStory> stories = new HashSet<>();
		stories.add(new UserStory(1L, "Story 1", "Description 1", 5, activeTable));
		stories.add(new UserStory(2L, "Story 2", "Description with \"quotes\"", 8, activeTable));
		activeTable.setUserStories(stories);

		when(pokerTableRepository.findById(activeTable.getId())).thenReturn(Optional.of(activeTable));

		byte[] csvBytes = pokerTableService.exportUserStoriesToCsv(activeTable.getId());
		String csvContent = new String(csvBytes);

		assertNotNull(csvContent);
		assertTrue(csvContent.contains("Summary,Description,Issue Type,Story point estimate"));
		assertTrue(csvContent.contains("\"Story 1\",\"Description 1\",\"Story\",5"));
		assertTrue(csvContent.contains("\"Story 2\",\"Description with \"\"quotes\"\"\",\"Story\",8"));
	}

	@Test
	void exportUserStoriesToCsv_shouldHandleNullDescriptionAndPoints() {
		Set<UserStory> stories = new HashSet<>();
		stories.add(new UserStory(1L, "Story 1", null, null, activeTable));
		activeTable.setUserStories(stories);

		when(pokerTableRepository.findById(activeTable.getId())).thenReturn(Optional.of(activeTable));

		byte[] csvBytes = pokerTableService.exportUserStoriesToCsv(activeTable.getId());
		String csvContent = new String(csvBytes);

		assertNotNull(csvContent);
		assertTrue(csvContent.contains("\"Story 1\",\"\",\"Story\",\n")); // Puste pola dla null
	}

	@Test
	void exportUserStoriesToCsv_shouldThrowNotFoundExceptionIfTableNotFound() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				pokerTableService.exportUserStoriesToCsv(99L));
	}

	@Test
	void resetAllVotes_shouldResetVotesForDevelopersInTable() {
		dev1.setVote(5);
		dev2.setVote(8);
		activeTable.setIsClosed(false); // Upewnij się, że stół jest aktywny

		when(pokerTableRepository.findById(activeTable.getId())).thenReturn(Optional.of(activeTable));
		when(developerRepository.save(any(Developer.class))).thenReturn(dev1, dev2);

		pokerTableService.resetAllVotes(activeTable.getId());

		assertNull(dev1.getVote());
		assertNull(dev2.getVote());
		verify(developerRepository, times(2)).save(any(Developer.class));
	}

	@Test
	void resetAllVotes_shouldThrowNotFoundExceptionIfTableNotFound() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				pokerTableService.resetAllVotes(99L));
	}

	@Test
	void resetAllVotes_shouldThrowIllegalStateExceptionIfTableIsClosed() {
		closedTable.getDevelopers().add(dev1); // Dodaj dewelopera do zamkniętego stołu
		dev1.setPokerTable(closedTable);

		when(pokerTableRepository.findById(closedTable.getId())).thenReturn(Optional.of(closedTable));

		assertThrows(IllegalStateException.class, () ->
				pokerTableService.resetAllVotes(closedTable.getId()));
	}
}
