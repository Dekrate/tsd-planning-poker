package pl.xsd.pokertable.pokertable;

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
import pl.xsd.pokertable.participation.Participation;
import pl.xsd.pokertable.participation.ParticipationRepository;

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

	@Mock
	private DeveloperRepository developerRepository;

	@Mock
	private ParticipationRepository participationRepository;

	@InjectMocks
	private PokerTableService pokerTableService;

	private PokerTable testPokerTable;
	private Developer testDeveloper1;
	private Developer testDeveloper2;

	@BeforeEach
	void setUp() {
		reset(pokerTableRepository, developerRepository, participationRepository);

		testPokerTable = new PokerTable(1L, "Test Table");
		testDeveloper1 = new Developer("session1", "Dev1");
		testDeveloper1.setId(101L);
		testDeveloper1.setPokerTable(testPokerTable);
		testDeveloper1.setVote(5);

		testDeveloper2 = new Developer("session2", "Dev2");
		testDeveloper2.setId(102L);
		testDeveloper2.setPokerTable(testPokerTable);
		testDeveloper2.setVote(8);

		Set<Developer> developers = new HashSet<>();
		developers.add(testDeveloper1);
		developers.add(testDeveloper2);
		testPokerTable.setDevelopers(developers);
	}


	@Test
	void createPokerTable_createsNew() {
		when(pokerTableRepository.save(any(PokerTable.class))).thenAnswer(inv -> inv.getArgument(0));

		PokerTable result = pokerTableService.createPokerTable("Test");

		assertThat(result).isNotNull();
		assertThat(result.getName()).isEqualTo("Test");
		assertThat(result.getIsClosed()).isFalse();
		assertThat(result.getCreatedAt()).isNotNull();

		verify(pokerTableRepository).save(any(PokerTable.class));
	}


	@Test
	void closePokerTable_allVoted_closesTableAndResetsVotes() {
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		when(pokerTableRepository.save(any(PokerTable.class))).thenAnswer(inv -> inv.getArgument(0));
		when(developerRepository.save(any(Developer.class))).thenAnswer(inv -> inv.getArgument(0));
		when(participationRepository.save(any(Participation.class))).thenAnswer(inv -> inv.getArgument(0));
		pokerTableService.closePokerTable(testPokerTable.getId());
		assertThat(testPokerTable.getIsClosed()).isTrue();
		assertThat(testDeveloper1.getVote()).isNull();
		assertThat(testDeveloper2.getVote()).isNull();
		verify(pokerTableRepository).findById(testPokerTable.getId());
		verify(pokerTableRepository).save(testPokerTable);
		verify(participationRepository, times(2)).save(any(Participation.class));
		verify(developerRepository, times(2)).save(any(Developer.class));
	}

	@Test
	void closePokerTable_notAllVotedNull_throwsException() {
		testDeveloper2.setVote(null);
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		NotEveryoneVotedException exception = assertThrows(NotEveryoneVotedException.class, () -> {
			pokerTableService.closePokerTable(testPokerTable.getId());
		});

		assertThat(exception.getMessage()).isEqualTo("Not all developers have submitted a vote yet, or there are no developers at the table.");
		verify(pokerTableRepository).findById(testPokerTable.getId());
		verify(pokerTableRepository, never()).save(any());
		verifyNoInteractions(participationRepository);
		verifyNoInteractions(developerRepository);
		assertThat(testPokerTable.getIsClosed()).isFalse();
	}

	@Test
	void closePokerTable_emptyTable_throwsException() {
		testPokerTable.setDevelopers(new HashSet<>());
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		NotEveryoneVotedException exception = assertThrows(NotEveryoneVotedException.class, () -> pokerTableService.closePokerTable(testPokerTable.getId()));

		assertThat(exception.getMessage()).isEqualTo("Not all developers have submitted a vote yet, or there are no developers at the table.");
		verify(pokerTableRepository).findById(testPokerTable.getId());
		verify(pokerTableRepository, never()).save(any());
		verifyNoInteractions(participationRepository);
		verifyNoInteractions(developerRepository);
		assertThat(testPokerTable.getIsClosed()).isFalse();
	}


	@Test
	void closePokerTable_tableNotFound_throwsException() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> pokerTableService.closePokerTable(1L));
		verify(pokerTableRepository).findById(1L);
		verify(pokerTableRepository, never()).save(any());
		verifyNoInteractions(participationRepository);
		verifyNoInteractions(developerRepository);
	}


	@Test
	void getActiveTable_noActiveTable_createsNew() {
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty());
		PokerTable newTable = new PokerTable();
		newTable.setId(1L);
		newTable.setName("Default Table");
		when(pokerTableRepository.save(any(PokerTable.class))).thenAnswer(inv -> {
			PokerTable tableToSave = inv.getArgument(0);
			tableToSave.setId(1L);
			return tableToSave;
		});
		PokerTable result = pokerTableService.getActiveTable();
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Default Table");
		assertThat(result.getIsClosed()).isFalse();
		verify(pokerTableRepository).findByIsClosedFalse();
		verify(pokerTableRepository).save(any(PokerTable.class));
	}

	@Test
	void getActiveTable_activeTableExists_returnsExisting() {
		PokerTable existingTable = new PokerTable();
		existingTable.setId(10L);
		existingTable.setName("Existing Active");
		existingTable.setIsClosed(false);

		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.of(existingTable));
		PokerTable result = pokerTableService.getActiveTable();
		assertThat(result).isEqualTo(existingTable);
		assertThat(result.getId()).isEqualTo(10L);
		verify(pokerTableRepository).findByIsClosedFalse();
		verifyNoMoreInteractions(pokerTableRepository);
	}

	@Test
	void getTableById_exists_returnsTable() {
		Long tableId = 123L;
		PokerTable table = new PokerTable();
		table.setId(tableId);
		table.setName("Specific Table");

		when(pokerTableRepository.findById(tableId)).thenReturn(Optional.of(table));
		PokerTable result = pokerTableService.getTableById(tableId);
		assertThat(result).isEqualTo(table);
		assertThat(result.getId()).isEqualTo(tableId);
		verify(pokerTableRepository).findById(tableId);
	}

	@Test
	void getTableById_notFound_throwsException() {
		Long tableId = 999L;

		when(pokerTableRepository.findById(tableId)).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> pokerTableService.getTableById(tableId));
		verify(pokerTableRepository).findById(tableId);
	}
}
