package pl.xsd.pokertable.developer;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;
import pl.xsd.pokertable.pokertable.PokerTableService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeveloperServiceTest {

	@Mock
	private DeveloperRepository developerRepository;

	@Mock
	private PokerTableRepository pokerTableRepository;

	@InjectMocks
	private DeveloperService developerService;

	@Mock
	private PokerTableService pokerTableService;

	@Test
	void vote_developerNotFound_throwsException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> developerService.vote(1L, 1L, 5));
	}

	@Test
	void vote_pokerTableNotFound_throwsException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(new Developer()));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> developerService.vote(1L, 1L, 5));
	}

	@Test
	void vote_developerNotInTable_throwsException() {
		Developer developer = new Developer();
		developer.setPokerTable(new PokerTable());
		PokerTable table = new PokerTable();

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));

		assertThrows(IllegalArgumentException.class, () -> developerService.vote(1L, 1L, 5));
	}

	@Test
	void vote_invalidVoteValue_throwsException() {
		assertThrows(IllegalArgumentException.class, () -> developerService.vote(1L, 1L, 0));
		assertThrows(IllegalArgumentException.class, () -> developerService.vote(1L, 1L, 14));
	}

	@Test
	void vote_validParameters_updatesVote() {
		PokerTable table = new PokerTable();
		Developer developer = new Developer();
		developer.setPokerTable(table);

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));

		developerService.vote(1L, 1L, 5);

		assertThat(developer.getVote()).isEqualTo(5);
		verify(developerRepository).save(developer);
	}

	@Test
	void getDeveloper_exists_returnsDeveloper() {
		Developer developer = new Developer();
		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		assertThat(developerService.getDeveloper(1L)).isEqualTo(developer);
	}

	@Test
	void hasVoted_developerExists_returnsCorrectStatus() {
		Developer devWithVote = new Developer();
		devWithVote.setVote(5);

		Developer devWithoutVote = new Developer();
		devWithoutVote.setVote(null);

		when(developerRepository.findById(1L)).thenReturn(Optional.of(devWithVote));
		when(developerRepository.findById(2L)).thenReturn(Optional.of(devWithoutVote));

		assertThat(developerService.hasVoted(1L)).isFalse();
		assertThat(developerService.hasVoted(2L)).isTrue();
	}

	@Test
	void getDevelopersForPokerTable_validTable_returnsDevelopers() {
		PokerTable table = new PokerTable();
		Set<Developer> developers = Set.of(new Developer());
		table.setDevelopers(developers);

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		assertThat(developerService.getDevelopersForPokerTable(1L)).isEqualTo(developers);
	}

	@Test
	void createDeveloper_validTable_savesDeveloper() {
		PokerTable table = new PokerTable();
		Developer developer = new Developer();

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(developerRepository.save(any())).thenReturn(developer);

		Developer result = developerService.createDeveloper(1L, developer);
		assertThat(result.getPokerTable()).isEqualTo(table);
	}

	@Test
	void createDeveloper_invalidTable_throwsException() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class, () -> developerService.createDeveloper(1L, new Developer()));
	}

	@Test
	void vote_nullVote_throwsException() {
		// Given
		Long developerId = 1L;
		Long tableId = 1L;
		Integer nullVote = null;

		// When & Then
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> developerService.vote(developerId, tableId, nullVote)
		);

		assertThat(exception.getMessage()).isEqualTo("Vote cannot be null");

		verifyNoInteractions(developerRepository);
		verifyNoInteractions(pokerTableRepository);
	}
	@Test
	void joinTable_newDeveloper_createsNewDeveloper() {
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("session123");
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setName("Test Table");
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.of(table));

		Developer newDev = new Developer("session123", "NewDev");
		newDev.setId(1L);
		when(developerRepository.findBySessionId("session123")).thenReturn(Optional.empty());
		when(developerRepository.save(any())).thenReturn(newDev);
		when(pokerTableRepository.findByIsClosedFalse())
				.thenReturn(Optional.of(table));

		Map<String, Object> result = developerService.joinTable("NewDev", session);

		assertThat(result.get("developer")).isNotNull();
		verify(developerRepository).save(any());
	}

	@Test
	void joinTable_existingDeveloper_returnsExisting() {
		// 1. Przygotowanie danych
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("existingSession");

		PokerTable table = new PokerTable(); // Dodajemy aktywny stół
		table.setId(1L);
		table.setName("Test Table");

		Developer existingDev = new Developer("existingSession", "ExistingDev");
		existingDev.setId(1L);
		existingDev.setPokerTable(table);

		// 2. Mockowanie zachowań
		when(developerRepository.findBySessionId("existingSession"))
				.thenReturn(Optional.of(existingDev));
		when(pokerTableRepository.findByIsClosedFalse())
				.thenReturn(Optional.of(table));

		// 3. Wywołanie metody
		Map<String, Object> result = developerService.joinTable("ExistingDev", session);

		// 4. Weryfikacja
		assertThat(result.get("developer")).isNotNull();
		verify(developerRepository, never()).save(any());
	}

	@Test
	void getActiveTable_noActiveTable_createsNew() {
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty());
		PokerTable newTable = new PokerTable();
		when(pokerTableService.createPokerTable("Blank")).thenReturn(newTable);

		PokerTable result = developerService.getActiveTable();

		assertThat(result).isEqualTo(newTable);
		verify(pokerTableService).createPokerTable("Blank");
	}

	@Test
	void vote_validMinVote_accepts() {
		PokerTable table = new PokerTable();
		Developer developer = new Developer();
		developer.setPokerTable(table);

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));

		developerService.vote(1L, 1L, 1);
		assertThat(developer.getVote()).isEqualTo(1);
	}

	@Test
	void vote_validMaxVote_accepts() {
		PokerTable table = new PokerTable();
		Developer developer = new Developer();
		developer.setPokerTable(table);

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));

		developerService.vote(1L, 1L, 13);
		assertThat(developer.getVote()).isEqualTo(13);
	}
}