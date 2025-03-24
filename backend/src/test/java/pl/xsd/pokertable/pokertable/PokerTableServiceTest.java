package pl.xsd.pokertable.pokertable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.exception.NotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PokerTableServiceTest {

	@Mock
	private PokerTableRepository pokerTableRepository;

	@InjectMocks
	private PokerTableService pokerTableService;

	@Test
	void createPokerTable_noActiveTable_createsNew() {
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty());
		when(pokerTableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		PokerTable result = pokerTableService.createPokerTable("Test");

		assertThat(result.getName()).isEqualTo("Test");
		assertThat(result.getIsClosed()).isFalse();
	}

	@Test
	void createPokerTable_activeTableExists_throwsException() {
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.of(new PokerTable()));
		assertThrows(IllegalStateException.class, () -> pokerTableService.createPokerTable("Test"));
	}

	@Test
	void closePokerTable_allVoted_closesTable() {
		PokerTable table = new PokerTable();
		table.setIsClosed(false);

		Developer dev1 = new Developer();
		dev1.setVote(null); // hasVoted() = true
		Developer dev2 = new Developer();
		dev2.setVote(null);
		table.setDevelopers(Set.of(dev1, dev2));

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));

		pokerTableService.closePokerTable(1L);

		assertThat(table.getIsClosed()).isTrue();
		verify(pokerTableRepository).save(table);
	}

	@Test
	void closePokerTable_notAllVoted_throwsException() {
		PokerTable table = new PokerTable();
		Developer dev1 = new Developer();
		dev1.setVote(5); // hasVoted() = false
		Developer dev2 = new Developer();
		dev2.setVote(null);
		table.setDevelopers(Set.of(dev1, dev2));

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));

		assertThrows(IllegalStateException.class, () -> pokerTableService.closePokerTable(1L));
	}

	@Test
	void closePokerTable_tableNotFound_throwsException() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> pokerTableService.closePokerTable(1L));
	}
}