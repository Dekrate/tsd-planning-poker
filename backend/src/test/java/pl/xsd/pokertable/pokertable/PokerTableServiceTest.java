package pl.xsd.pokertable.pokertable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.exception.NotEveryoneVotedException;
import pl.xsd.pokertable.exception.NotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PokerTableServiceTest {

	@Mock
	private PokerTableRepository pokerTableRepository;

	@InjectMocks
	private PokerTableService pokerTableService;

	@Test
	void createPokerTable_noActiveTable_createsNew() {
		when(pokerTableRepository.save(any(PokerTable.class))).thenAnswer(inv -> inv.getArgument(0));

		PokerTable result = pokerTableService.createPokerTable("Test");

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Test");
		assertThat(result.getIsClosed()).isFalse();
		assertThat(result.getCreatedAt()).isNotNull();

		verify(pokerTableRepository).save(any(PokerTable.class));
	}


	@Test
	void closePokerTable_allVotedOrZero_closesTable() {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setIsClosed(false);

		Developer dev1 = new Developer();
		dev1.setId(101L);
		dev1.setVote(5);

		Developer dev2 = new Developer();
		dev2.setId(102L);
		dev2.setVote(0);

		Set<Developer> developers = new HashSet<>(Set.of(dev1, dev2));
		table.setDevelopers(developers);

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(pokerTableRepository.save(any(PokerTable.class))).thenAnswer(inv -> inv.getArgument(0));

		// Act
		pokerTableService.closePokerTable(1L);

		// Assert
		assertThat(table.getIsClosed()).isTrue();
		verify(pokerTableRepository).save(table);
		verify(pokerTableRepository).findById(1L);
	}

	@Test
	void closePokerTable_notAllVotedNull_throwsException() {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setIsClosed(false);

		Developer dev1 = new Developer();
		dev1.setId(101L);
		dev1.setVote(5); // hasVoted() = false

		Developer dev2 = new Developer();
		dev2.setId(102L);
		dev2.setVote(null); // hasVoted() = true

		Set<Developer> developers = new HashSet<>(Set.of(dev1, dev2));
		table.setDevelopers(developers);

		when(pokerTableRepository.findById(1L)).thenReturn(Optional.of(table));


		NotEveryoneVotedException exception = assertThrows(NotEveryoneVotedException.class, () -> {
			pokerTableService.closePokerTable(1L); // <-- Tutaj wywołujemy metodę testowaną
		});

		assertThat(exception.getMessage()).isEqualTo("Not all developers have submitted a vote yet, or there are no developers at the table.");

		verify(pokerTableRepository).findById(1L);

		verify(pokerTableRepository, never()).save(any());

		assertThat(table.getIsClosed()).isFalse();
	}

	@Test
	void closePokerTable_emptyTable_throwsException() {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setIsClosed(false);
		table.setDevelopers(new HashSet<>());

		when(pokerTableRepository.findById(1L)).thenReturn(Optional.of(table));


		NotEveryoneVotedException exception = assertThrows(NotEveryoneVotedException.class, () -> {
			pokerTableService.closePokerTable(1L);
		});

		assertThat(exception.getMessage()).isEqualTo("Not all developers have submitted a vote yet, or there are no developers at the table.");


		verify(pokerTableRepository).findById(1L);

		verify(pokerTableRepository, never()).save(any());

		assertThat(table.getIsClosed()).isFalse();
	}


	@Test
	void closePokerTable_tableNotFound_throwsException() {
		// Arrange
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(NotFoundException.class, () -> pokerTableService.closePokerTable(1L));

		// Verify
		verify(pokerTableRepository).findById(1L);
		verify(pokerTableRepository, never()).save(any());
	}


	@Test
	void getActiveTable_noActiveTable_createsNew() {
		// Arrange
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty());
		PokerTable newTable = new PokerTable();
		newTable.setId(1L);
		newTable.setName("Default Table");
		when(pokerTableRepository.save(any(PokerTable.class))).thenAnswer(inv -> {
			PokerTable tableToSave = inv.getArgument(0);
			tableToSave.setId(1L);
			return tableToSave;
		});


		// Act
		PokerTable result = pokerTableService.getActiveTable();

		// Assert
		assertThat(result).isNotNull();

		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty(), Optional.empty());

		when(pokerTableRepository.save(any(PokerTable.class))).thenAnswer(inv -> {
			PokerTable tableToSave = inv.getArgument(0);
			tableToSave.setId(1L);
			return tableToSave;
		});


		// Act
		result = pokerTableService.getActiveTable();

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Default Table");
		assertThat(result.getIsClosed()).isFalse();

		// Verify
		verify(pokerTableRepository, times(2)).findByIsClosedFalse();
		verify(pokerTableRepository, times(2)).save(any(PokerTable.class));
	}

	@Test
	void getActiveTable_activeTableExists_returnsExisting() {
		// Arrange
		PokerTable existingTable = new PokerTable();
		existingTable.setId(10L);
		existingTable.setName("Existing Active");
		existingTable.setIsClosed(false);

		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.of(existingTable));

		// Act
		PokerTable result = pokerTableService.getActiveTable();

		// Assert
		assertThat(result).isEqualTo(existingTable);
		assertThat(result.getId()).isEqualTo(10L);

		// Verify
		verify(pokerTableRepository).findByIsClosedFalse();
		verifyNoMoreInteractions(pokerTableRepository);
	}

	@Test
	void getTableById_exists_returnsTable() {
		// Arrange
		Long tableId = 123L;
		PokerTable table = new PokerTable();
		table.setId(tableId);
		table.setName("Specific Table");

		when(pokerTableRepository.findById(tableId)).thenReturn(Optional.of(table));

		// Act
		PokerTable result = pokerTableService.getTableById(tableId);

		// Assert
		assertThat(result).isEqualTo(table);
		assertThat(result.getId()).isEqualTo(tableId);

		// Verify
		verify(pokerTableRepository).findById(tableId);
	}

	@Test
	void getTableById_notFound_throwsException() {
		// Arrange
		Long tableId = 999L;

		when(pokerTableRepository.findById(eq(tableId))).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(NotFoundException.class, () -> pokerTableService.getTableById(tableId));

		// Verify
		verify(pokerTableRepository).findById(tableId);
	}

}