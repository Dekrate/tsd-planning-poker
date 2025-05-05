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
import pl.xsd.pokertable.pokertable.PokerTableService; // Potrzebne, bo getActiveTable go używa

import java.util.HashSet;
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

	@Mock // DeveloperService używa PokerTableService w getActiveTable
	private PokerTableService pokerTableService;

	@InjectMocks
	private DeveloperService developerService;


	// --- Testy dla vote ---
	@Test
	void vote_developerNotFound_throwsException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> developerService.vote(1L, 1L, 5));

		verify(developerRepository).findById(1L);
		verifyNoInteractions(pokerTableRepository); // Nie powinno szukać tabeli
	}

	@Test
	void vote_pokerTableNotFound_throwsException() {
		Developer developer = new Developer();
		// Nie musimy ustawiać tabeli dla developera na tym etapie testu
		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> developerService.vote(1L, 1L, 5));

		verify(developerRepository).findById(1L);
		verify(pokerTableRepository).findById(1L);
	}

	@Test
	void vote_developerNotInTable_throwsException() {
		// Arrange
		PokerTable devTable = new PokerTable();
		devTable.setId(10L); // Developer jest w tabeli 10
		Developer developer = new Developer();
		developer.setPokerTable(devTable);

		PokerTable targetTable = new PokerTable();
		targetTable.setId(20L); // Próbujemy zagłosować w tabeli 20

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(targetTable));

		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> developerService.vote(1L, 20L, 5));

		// Verify
		verify(developerRepository).findById(1L);
		verify(pokerTableRepository).findById(20L);
		// verify nie jest potrzebne dla getterów/setterów, tylko dla interakcji z mockami
	}

	@Test
	void vote_nullVote_throwsException() {
		// Arrange
		Long developerId = 1L;
		Long tableId = 1L;
		Integer nullVote = null;

		// Act & Then
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> developerService.vote(developerId, tableId, nullVote)
		);

		// Assert
		assertThat(exception.getMessage()).isEqualTo("Vote cannot be null");

		// Verify - Walidacja następuje PRZED wywołaniem repozytoriów
		verifyNoInteractions(developerRepository); // Upewnij się, że nie było interakcji z repozytorium developerów
		verifyNoInteractions(pokerTableRepository); // Upewnij się, że nie było interakcji z repozytorium tabel
	}

	@Test
	void vote_voteLessThanOne_throwsException() {
		// Arrange
		Long developerId = 1L;
		Long tableId = 1L;
		Integer invalidVote = 0; // Wartość < 1

		// Act & Then
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> developerService.vote(developerId, tableId, invalidVote)
		);

		// Assert
		assertThat(exception.getMessage()).isEqualTo("Vote must be between 1 and 13"); // Wiadomość z serwisu

		// Verify - Walidacja następuje PRZED wywołaniem repozytoriów
		verifyNoInteractions(developerRepository);
		verifyNoInteractions(pokerTableRepository);
	}

	@Test
	void vote_voteGreaterThanThirteen_throwsException() {
		// Arrange
		Long developerId = 1L;
		Long tableId = 1L;
		Integer invalidVote = 14; // Wartość > 13

		// Act & Then
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> developerService.vote(developerId, tableId, invalidVote)
		);

		// Assert
		assertThat(exception.getMessage()).isEqualTo("Vote must be between 1 and 13"); // Wiadomość z serwisu

		// Verify - Walidacja następuje PRZED wywołaniem repozytoriów
		verifyNoInteractions(developerRepository);
		verifyNoInteractions(pokerTableRepository);
	}


	@Test
	void vote_validParameters_updatesVote() {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		Developer developer = new Developer();
		developer.setPokerTable(table); // Ustaw developera w docelowej tabeli

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		// Mock save żeby zwrócił ten sam obiekt (opcjonalne, ale dobra praktyka)
		when(developerRepository.save(any(Developer.class))).thenReturn(developer);

		// Act
		developerService.vote(1L, 1L, 5);

		// Assert
		assertThat(developer.getVote()).isEqualTo(5);

		// Verify
		verify(developerRepository).findById(1L);
		verify(pokerTableRepository).findById(1L);
		verify(developerRepository).save(developer); // Sprawdzenie, że save został wywołany
	}

	@Test
	void vote_validMinVote_accepts() {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		Developer developer = new Developer();
		developer.setPokerTable(table); // Ustaw developera w docelowej tabeli

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(developerRepository.save(any(Developer.class))).thenReturn(developer);

		// Act
		developerService.vote(1L, 1L, 1);

		// Assert
		assertThat(developer.getVote()).isEqualTo(1);
		verify(developerRepository).save(developer);
	}

	@Test
	void vote_validMaxVote_accepts() {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		Developer developer = new Developer();
		developer.setPokerTable(table); // Ustaw developera w docelowej tabeli

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(developerRepository.save(any(Developer.class))).thenReturn(developer);

		// Act
		developerService.vote(1L, 1L, 13);

		// Assert
		assertThat(developer.getVote()).isEqualTo(13);
		verify(developerRepository).save(developer);
	}


	// --- Testy dla getDeveloper ---
	@Test
	void getDeveloper_exists_returnsDeveloper() {
		// Arrange
		Developer developer = new Developer();
		developer.setId(1L);
		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));

		// Act
		Developer result = developerService.getDeveloper(1L);

		// Assert
		assertThat(result).isEqualTo(developer);

		// Verify
		verify(developerRepository).findById(1L);
	}

	@Test
	void getDeveloper_notFound_throwsException() {
		// Arrange
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(NotFoundException.class, () -> developerService.getDeveloper(999L));

		// Verify
		verify(developerRepository).findById(999L);
	}


	// --- Testy dla hasVoted ---
	// hasVoted() zwraca true gdy vote == null
	@Test
	void hasVoted_developerExists_returnsCorrectStatus() {
		// Arrange
		Developer devWithVoteNonNull = new Developer();
		devWithVoteNonNull.setId(1L);
		devWithVoteNonNull.setVote(5); // Non-null vote

		Developer devWithVoteZero = new Developer();
		devWithVoteZero.setId(2L);
		devWithVoteZero.setVote(0); // Vote is 0

		Developer devWithoutVote = new Developer();
		devWithoutVote.setId(3L);
		devWithoutVote.setVote(null); // Vote is null

		when(developerRepository.findById(1L)).thenReturn(Optional.of(devWithVoteNonNull));
		when(developerRepository.findById(2L)).thenReturn(Optional.of(devWithVoteZero));
		when(developerRepository.findById(3L)).thenReturn(Optional.of(devWithoutVote));

		// Act & Assert
		assertThat(developerService.hasVoted(1L)).isFalse(); // false bo vote nie jest null
		assertThat(developerService.hasVoted(2L)).isFalse(); // false bo vote nie jest null (jest 0)
		assertThat(developerService.hasVoted(3L)).isTrue();  // true bo vote jest null

		// Verify
		verify(developerRepository).findById(1L);
		verify(developerRepository).findById(2L);
		verify(developerRepository).findById(3L);
	}

	@Test
	void hasVoted_developerNotFound_throwsException() {
		// Arrange
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(NotFoundException.class, () -> developerService.hasVoted(999L));

		// Verify
		verify(developerRepository).findById(999L);
	}


	// --- Testy dla getDevelopersForPokerTable ---
	@Test
	void getDevelopersForPokerTable_validTable_returnsDevelopers() {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		Set<Developer> developers = Set.of(new Developer(), new Developer());
		table.setDevelopers(developers);

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));

		// Act
		Set<Developer> result = developerService.getDevelopersForPokerTable(1L);

		// Assert
		assertThat(result).isEqualTo(developers);

		// Verify
		verify(pokerTableRepository).findById(1L);
	}

	@Test
	void getDevelopersForPokerTable_tableNotFound_throwsException() {
		// Arrange
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(NotFoundException.class, () -> developerService.getDevelopersForPokerTable(999L));

		// Verify
		verify(pokerTableRepository).findById(999L);
	}


	// --- Testy dla createDeveloper ---
	// Ta metoda może być uznana za przestarzałą po zmianie joinTable, ale testujemy ją, bo istnieje
	@Test
	void createDeveloper_validTable_savesDeveloper() {
		// Arrange
		PokerTable table = new PokerTable();
		table.setId(1L);
		Developer developer = new Developer(); // Nowy developer do zapisania

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(developerRepository.save(any(Developer.class))).thenReturn(developer);

		// Act
		Developer result = developerService.createDeveloper(1L, developer);

		// Assert
		assertThat(result).isEqualTo(developer);
		assertThat(result.getPokerTable()).isEqualTo(table); // Sprawdzamy, czy tabela została poprawnie ustawiona
		assertThat(result.getVote()).isNull(); // Sprawdzamy, czy vote jest null (zgodnie ze zmianą w serwisie)

		// Verify
		verify(pokerTableRepository).findById(1L);
		verify(developerRepository).save(developer);
	}

	@Test
	void createDeveloper_invalidTable_throwsException() {
		// Arrange
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		// Act & Assert
		assertThrows(IllegalArgumentException.class, () -> developerService.createDeveloper(1L, new Developer()));

		// Verify
		verify(pokerTableRepository).findById(1L);
		verify(developerRepository, never()).save(any()); // Save nie powinno być wywołane
	}


	// --- Testy dla zaktualizowanego joinTable(String name, Long tableId, HttpSession session) ---

	@Test
	void joinTable_newDeveloper_createsNewDeveloperAndAssociatesWithTable() {
		// Arrange
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("newSession123");
		Long targetTableId = 10L;

		PokerTable table = new PokerTable();
		table.setId(targetTableId);
		table.setName("New Table");

		Developer newDev = new Developer("newSession123", "NewDev"); // Developer stworzony przez logikę serwisu
		newDev.setId(1L); // ID ustawione po zapisie

		when(pokerTableRepository.findById(eq(targetTableId))).thenReturn(Optional.of(table));
		when(developerRepository.findBySessionId(eq("newSession123"))).thenReturn(Optional.empty());
		// Mockujemy save, aby zwrócił obiekt developera z ustawionym ID, tabelą i vote=null
		when(developerRepository.save(any(Developer.class))).thenAnswer(invocation -> {
			Developer devToSave = invocation.getArgument(0);
			devToSave.setId(1L); // Symulacja ustawienia ID przez JPA
			devToSave.setPokerTable(table); // Symulacja ustawienia tabeli
			devToSave.setVote(null); // Symulacja ustawienia domyślnego głosu (null)
			return devToSave;
		});

		// Act
		Map<String, Object> result = developerService.joinTable("NewDev", targetTableId, session);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result).containsKey("developer");
		assertThat(result).containsKey("table");

		Map<String, Object> developerMap = (Map<String, Object>) result.get("developer");
		assertThat(developerMap).containsKey("id").containsValue(1L);
		assertThat(developerMap).containsKey("name").containsValue("NewDev");
		assertThat(developerMap).containsKey("sessionId").containsValue("newSession123");
		// vote nie jest zwracany w mapie w joinTable w DeveloperService, więc nie testujemy go w mapie wynikowej

		Map<String, Object> tableMap = (Map<String, Object>) result.get("table");
		assertThat(tableMap).containsKey("id").containsValue(targetTableId);
		assertThat(tableMap).containsKey("name").containsValue("New Table");

		// Verify
		verify(session).getId(); // Upewnij się, że pobrano ID sesji
		verify(pokerTableRepository).findById(targetTableId); // Upewnij się, że znaleziono tabelę docelową
		verify(developerRepository).findBySessionId("newSession123"); // Upewnij się, że sprawdzono istniejącego developera
		verify(developerRepository).save(any(Developer.class)); // Upewnij się, że zapisano nowego developera
		verify(pokerTableRepository, never()).findByIsClosedFalse(); // Upewnij się, że nie użyto getActiveTable
	}

	@Test
	void joinTable_existingDeveloperOnSameTable_returnsExistingDeveloper() {
		// Arrange
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("existingSession456");
		Long targetTableId = 30L;

		PokerTable table = new PokerTable();
		table.setId(targetTableId);
		table.setName("Existing Table");

		Developer existingDev = new Developer("existingSession456", "ExistingDev");
		existingDev.setId(5L);
		existingDev.setPokerTable(table); // Developer jest już w tej tabeli
		existingDev.setVote(5); // Miał już jakiś głos

		when(pokerTableRepository.findById(eq(targetTableId))).thenReturn(Optional.of(table));
		when(developerRepository.findBySessionId(eq("existingSession456"))).thenReturn(Optional.of(existingDev));

		// Act
		Map<String, Object> result = developerService.joinTable("ExistingDev", targetTableId, session); // Imię może być takie samo lub inne

		// Assert
		assertThat(result).isNotNull();
		Map<String, Object> developerMap = (Map<String, Object>) result.get("developer");
		assertThat(developerMap).containsKey("id").containsValue(5L); // Zwrócono istniejące ID
		assertThat(developerMap).containsKey("name").containsValue("ExistingDev"); // Zwrócono istniejące imię
		// Vote i tabela developera nie powinny być zmienione
		assertThat(existingDev.getPokerTable().getId()).isEqualTo(targetTableId);
		assertThat(existingDev.getVote()).isEqualTo(5);


		// Verify
		verify(session).getId();
		verify(pokerTableRepository).findById(targetTableId);
		verify(developerRepository).findBySessionId("existingSession456");
		verify(developerRepository, never()).save(any()); // Upewnij się, że nie zapisano (nie zmieniono) developera
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}

	@Test
	void joinTable_existingDeveloperOnDifferentTable_updatesDeveloperTableAndResetsVote() {
		// Arrange
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionToMove");
		Long oldTableId = 100L;
		Long newTableId = 200L;

		PokerTable oldTable = new PokerTable();
		oldTable.setId(oldTableId);
		oldTable.setName("Old Table");

		PokerTable newTable = new PokerTable();
		newTable.setId(newTableId);
		newTable.setName("New Table");


		Developer existingDev = new Developer("sessionToMove", "MovingDev");
		existingDev.setId(10L);
		existingDev.setPokerTable(oldTable); // Developer jest w starej tabeli
		existingDev.setVote(8); // Miał jakiś głos w starej tabeli


		when(pokerTableRepository.findById(eq(newTableId))).thenReturn(Optional.of(newTable)); // Szukamy nowej tabeli
		when(developerRepository.findBySessionId(eq("sessionToMove"))).thenReturn(Optional.of(existingDev)); // Znaleziono developera po sesji
		when(developerRepository.save(any(Developer.class))).thenReturn(existingDev); // Mock zapisu

		// Act
		Map<String, Object> result = developerService.joinTable("MovingDev", newTableId, session); // Dołącza do nowej tabeli

		// Assert
		assertThat(result).isNotNull();
		Map<String, Object> developerMap = (Map<String, Object>) result.get("developer");
		assertThat(developerMap).containsKey("id").containsValue(10L);
		assertThat(developerMap).containsKey("name").containsValue("MovingDev"); // Imię może być zaktualizowane w logice serwisu, ale testujemy co zwrócono

		// Sprawdź, czy developer został zaktualizowany w bazie
		assertThat(existingDev.getPokerTable().getId()).isEqualTo(newTableId); // Tabela powinna być zmieniona
		assertThat(existingDev.getVote()).isNull(); // Głos powinien być zresetowany do null

		// Verify
		verify(session).getId();
		verify(pokerTableRepository).findById(newTableId); // Szukano nowej tabeli
		verify(developerRepository).findBySessionId("sessionToMove"); // Znaleziono developera
		verify(developerRepository).save(existingDev); // Upewnij się, że zapisano zaktualizowanego developera
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}


	@Test
	void joinTable_tableNotFound_throwsException() {
		// Arrange
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionError");
		Long nonExistentTableId = 999L;

		when(pokerTableRepository.findById(eq(nonExistentTableId))).thenReturn(Optional.empty()); // Tabela nie istnieje

		// Act & Assert
		assertThrows(NotFoundException.class, () -> developerService.joinTable("Test", nonExistentTableId, session));

		// Verify
		verify(session).getId();
		verify(pokerTableRepository).findById(nonExistentTableId);
		verify(developerRepository, never()).findBySessionId(anyString()); // Nie powinno szukać developera
		verify(developerRepository, never()).save(any());
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}
// Inside DeveloperServiceTest

	@Test
	void joinTable_existingDeveloperWithNullTable_updatesDeveloperTableAndResetsVote() {
		// Arrange
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionWithNullTable");
		Long targetTableId = 50L;

		PokerTable targetTable = new PokerTable();
		targetTable.setId(targetTableId);
		targetTable.setName("Target Table");

		Developer existingDevWithNullTable = new Developer("sessionWithNullTable", "DevWithNullTable");
		existingDevWithNullTable.setId(15L);
		// existingDevWithNullTable.setPokerTable(null); // pokerTable is null by default for new objects
		existingDevWithNullTable.setVote(7); // Give it a vote to ensure it's reset


		when(pokerTableRepository.findById(eq(targetTableId))).thenReturn(Optional.of(targetTable)); // Szukamy docelowej tabeli
		when(developerRepository.findBySessionId(eq("sessionWithNullTable"))).thenReturn(Optional.of(existingDevWithNullTable)); // Znaleziono developera po sesji, ale ma null table
		// Mock zapisu - powinien otrzymać obiekt existingDevWithNullTable po jego modyfikacji przez serwis
		when(developerRepository.save(any(Developer.class))).thenAnswer(invocation -> invocation.getArgument(0));


		// Act
		Map<String, Object> result = developerService.joinTable("DevWithNullTable", targetTableId, session); // Dołącza do targetTable

		// Assert
		assertThat(result).isNotNull();
		Map<String, Object> developerMap = (Map<String, Object>) result.get("developer");
		assertThat(developerMap).containsKey("id").containsValue(15L);
		assertThat(developerMap).containsKey("name").containsValue("DevWithNullTable");

		// Sprawdź, czy developer został zaktualizowany w bazie (czyli obiekt, który został zapisany)
		assertThat(existingDevWithNullTable.getPokerTable().getId()).isEqualTo(targetTableId); // Tabela powinna być ustawiona na docelową
		assertThat(existingDevWithNullTable.getVote()).isNull(); // Głos powinien być zresetowany do null

		// Verify
		verify(session).getId();
		verify(pokerTableRepository).findById(targetTableId); // Szukano docelowej tabeli
		verify(developerRepository).findBySessionId("sessionWithNullTable"); // Znaleziono developera
		verify(developerRepository).save(existingDevWithNullTable); // Upewnij się, że zapisano ZAKTUALIZOWANEGO developera
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}

	// --- Testy dla getActiveTable ---
	@Test
	void getActiveTable_noActiveTable_createsNew() {
		// Arrange
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty());

		PokerTable newTable = new PokerTable(); // Obiekt, który zostanie zwrócony przez mockowany serwis
		newTable.setId(1L);
		newTable.setName("Blank"); // Nazwa powinna być zgodna z argumentem w DeveloperService

		// Mockujemy wywołanie metody z drugiego serwisu
		// Zmieniamy oczekiwany argument na "Blank"
		when(pokerTableService.createPokerTable("Blank")).thenReturn(newTable);

		// Act
		PokerTable result = developerService.getActiveTable();

		// Assert
		assertThat(result).isEqualTo(newTable);
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Blank"); // Sprawdzamy nazwę

		// Verify
		verify(pokerTableRepository).findByIsClosedFalse(); // Sprawdzenie, że szukano aktywnej
		// Zmieniamy oczekiwany argument w weryfikacji na "Blank"
		verify(pokerTableService).createPokerTable("Blank"); // Sprawdzenie, że wywołano metodę tworzenia z poprawnym argumentem
		verifyNoMoreInteractions(pokerTableService); // Upewnij się, że nic więcej nie było wywołane
		verifyNoInteractions(developerRepository); // Upewnij się, że repozytorium developera nie było użyte
	}



	@Test
	void getActiveTable_activeTableExists_returnsExisting() {
		// Arrange
		PokerTable existingTable = new PokerTable();
		existingTable.setId(10L);
		existingTable.setName("Existing Active");
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.of(existingTable));

		// Act
		PokerTable result = developerService.getActiveTable();

		// Assert
		assertThat(result).isEqualTo(existingTable);
		assertThat(result.getId()).isEqualTo(10L);

		// Verify
		verify(pokerTableRepository).findByIsClosedFalse();
		verifyNoInteractions(pokerTableService); // Upewnij się, że nie tworzono nowej tabeli
	}

}