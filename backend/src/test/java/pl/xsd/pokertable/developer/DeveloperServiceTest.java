package pl.xsd.pokertable.developer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;

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

	@Test
	void vote_developerNotFound_throwsException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class, () -> developerService.vote(1L, 1L, 5));
	}

	@Test
	void vote_pokerTableNotFound_throwsException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(new Developer()));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class, () -> developerService.vote(1L, 1L, 5));
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
}