package pl.xsd.pokertable.developer;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pl.xsd.pokertable.config.JwtUtil;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.participation.Participation;
import pl.xsd.pokertable.participation.ParticipationRepository;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;
import pl.xsd.pokertable.pokertable.PokerTableService;

import java.time.LocalDateTime;
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

	@Mock
	private PokerTableService pokerTableService;

	@Mock
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Mock
	private ParticipationRepository participationRepository;

	@Mock
	private AuthenticationManager authenticationManager;

	@Mock
	private UserDetailsService userDetailsService;

	@Mock
	private JwtUtil jwtUtil;

	@InjectMocks
	private DeveloperService developerService;

	private Developer testDeveloper;
	private PokerTable testPokerTable;

	@BeforeEach
	void setUp() {
		reset(developerRepository, pokerTableRepository, pokerTableService, bCryptPasswordEncoder,
				participationRepository, authenticationManager, userDetailsService, jwtUtil);

		testDeveloper = new Developer("testSessionId", "Test Developer");
		testDeveloper.setId(1L);
		testDeveloper.setEmail("test@example.com");
		testDeveloper.setPassword("hashedPassword");

		testPokerTable = new PokerTable(10L, "Test Table");
		testPokerTable.setCreatedAt(LocalDateTime.now());
	}


	@Test
	void vote_developerNotFound_throwsException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> developerService.vote(1L, 1L, 5));

		verify(developerRepository).findById(1L);
		verifyNoInteractions(pokerTableRepository);
	}

	@Test
	void vote_pokerTableNotFound_throwsException() {
		Developer developer = new Developer();
		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> developerService.vote(1L, 1L, 5));

		verify(developerRepository).findById(1L);
		verify(pokerTableRepository).findById(1L);
	}

	@Test
	void vote_developerNotInTable_throwsException() {
		PokerTable devTable = new PokerTable();
		devTable.setId(10L);
		devTable.setCreatedAt(LocalDateTime.now());
		Developer developer = new Developer();
		developer.setPokerTable(devTable);

		PokerTable targetTable = new PokerTable();
		targetTable.setId(20L);
		targetTable.setCreatedAt(LocalDateTime.now());

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(targetTable));
		assertThrows(IllegalArgumentException.class, () -> developerService.vote(1L, 20L, 5));
		verify(developerRepository).findById(1L);
		verify(pokerTableRepository).findById(20L);
	}

	@Test
	void vote_nullVote_throwsException() {
		Long developerId = 1L;
		Long tableId = 1L;
		Integer nullVote = null;
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> developerService.vote(developerId, tableId, nullVote)
		);
		assertThat(exception.getMessage()).isEqualTo("Vote cannot be null");

		verifyNoInteractions(developerRepository);
		verifyNoInteractions(pokerTableRepository);
	}

	@Test
	void vote_voteLessThanOne_throwsException() {
		Long developerId = 1L;
		Long tableId = 1L;
		Integer invalidVote = 0;
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> developerService.vote(developerId, tableId, invalidVote)
		);
		assertThat(exception.getMessage()).isEqualTo("Vote must be between 1 and 13");

		verifyNoInteractions(developerRepository);
		verifyNoInteractions(pokerTableRepository);
	}

	@Test
	void vote_voteGreaterThanThirteen_throwsException() {
		Long developerId = 1L;
		Long tableId = 1L;
		Integer invalidVote = 14;
		IllegalArgumentException exception = assertThrows(
				IllegalArgumentException.class,
				() -> developerService.vote(developerId, tableId, invalidVote)
		);
		assertThat(exception.getMessage()).isEqualTo("Vote must be between 1 and 13");
		verifyNoInteractions(developerRepository);
		verifyNoInteractions(pokerTableRepository);
	}


	@Test
	void vote_validParameters_updatesVote() {
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setCreatedAt(LocalDateTime.now());
		Developer developer = new Developer();
		developer.setPokerTable(table);

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(developerRepository.save(any(Developer.class))).thenReturn(developer);
		developerService.vote(1L, 1L, 5);
		assertThat(developer.getVote()).isEqualTo(5);
		verify(developerRepository).findById(1L);
		verify(pokerTableRepository).findById(1L);
		verify(developerRepository).save(developer);
	}

	@Test
	void vote_validMinVote_accepts() {
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setCreatedAt(LocalDateTime.now());
		Developer developer = new Developer();
		developer.setPokerTable(table);

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(developerRepository.save(any(Developer.class))).thenReturn(developer);
		developerService.vote(1L, 1L, 1);
		assertThat(developer.getVote()).isEqualTo(1);
		verify(developerRepository).save(developer);
	}

	@Test
	void vote_validMaxVote_accepts() {
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setCreatedAt(LocalDateTime.now());
		Developer developer = new Developer();
		developer.setPokerTable(table);

		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(developerRepository.save(any(Developer.class))).thenReturn(developer);
		developerService.vote(1L, 1L, 13);
		assertThat(developer.getVote()).isEqualTo(13);
		verify(developerRepository).save(developer);
	}


	@Test
	void getDeveloper_exists_returnsDeveloper() {
		Developer developer = new Developer();
		developer.setId(1L);
		when(developerRepository.findById(anyLong())).thenReturn(Optional.of(developer));
		Developer result = developerService.getDeveloper(1L);
		assertThat(result).isEqualTo(developer);
		verify(developerRepository).findById(1L);
	}

	@Test
	void getDeveloper_notFound_throwsException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> developerService.getDeveloper(999L));
		verify(developerRepository).findById(999L);
	}


	@Test
	void hasVoted_developerExists_returnsCorrectStatus() {
		Developer devWithVoteNonNull = new Developer();
		devWithVoteNonNull.setId(1L);
		devWithVoteNonNull.setVote(5);

		Developer devWithVoteZero = new Developer();
		devWithVoteZero.setId(2L);
		devWithVoteZero.setVote(0);

		Developer devWithoutVote = new Developer();
		devWithoutVote.setId(3L);
		devWithoutVote.setVote(null);

		when(developerRepository.findById(1L)).thenReturn(Optional.of(devWithVoteNonNull));
		when(developerRepository.findById(2L)).thenReturn(Optional.of(devWithVoteZero));
		when(developerRepository.findById(3L)).thenReturn(Optional.of(devWithoutVote));
		assertThat(developerService.hasVoted(1L)).isFalse();
		assertThat(developerService.hasVoted(2L)).isFalse();
		assertThat(developerService.hasVoted(3L)).isTrue();
		verify(developerRepository).findById(1L);
		verify(developerRepository).findById(2L);
		verify(developerRepository).findById(3L);
	}

	@Test
	void hasVoted_developerNotFound_throwsException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> developerService.hasVoted(999L));
		verify(developerRepository).findById(999L);
	}


	@Test
	void getDevelopersForPokerTable_validTable_returnsDevelopers() {
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setCreatedAt(LocalDateTime.now());
		Set<Developer> developers = Set.of(new Developer(), new Developer());
		table.setDevelopers(developers);

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		Set<Developer> result = developerService.getDevelopersForPokerTable(1L);
		assertThat(result).isEqualTo(developers);
		verify(pokerTableRepository).findById(1L);
	}

	@Test
	void getDevelopersForPokerTable_tableNotFound_throwsException() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> developerService.getDevelopersForPokerTable(999L));
		verify(pokerTableRepository).findById(999L);
	}


	@Test
	void createDeveloper_validTable_savesDeveloper() {
		PokerTable table = new PokerTable();
		table.setId(1L);
		table.setCreatedAt(LocalDateTime.now());
		Developer developer = new Developer();

		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(table));
		when(developerRepository.save(any(Developer.class))).thenReturn(developer);
		Developer result = developerService.createDeveloper(1L, developer);
		assertThat(result).isEqualTo(developer);
		assertThat(result.getPokerTable()).isEqualTo(table);
		assertThat(result.getVote()).isNull();
		verify(pokerTableRepository).findById(1L);
		verify(developerRepository).save(developer);
	}

	@Test
	void createDeveloper_invalidTable_throwsException() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());
		assertThrows(IllegalArgumentException.class, () -> developerService.createDeveloper(1L, new Developer()));
		verify(pokerTableRepository).findById(1L);
		verify(developerRepository, never()).save(any());
	}


	@Test
	void joinTable_newDeveloper_createsNewDeveloperAndAssociatesWithTable() {
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("newSession123");
		Long targetTableId = 10L;

		PokerTable table = new PokerTable();
		table.setId(targetTableId);
		table.setName("New Table");
		table.setCreatedAt(LocalDateTime.now());

		Developer newDev = new Developer("newSession123", "NewDev");
		newDev.setId(1L);

		when(pokerTableRepository.findById(targetTableId)).thenReturn(Optional.of(table));
		when(developerRepository.findBySessionId("newSession123")).thenReturn(Optional.empty());
		when(developerRepository.save(any(Developer.class))).thenAnswer(invocation -> {
			Developer devToSave = invocation.getArgument(0);
			devToSave.setId(1L);
			devToSave.setPokerTable(table);
			devToSave.setVote(null);
			return devToSave;
		});
		Map<String, Object> result = developerService.joinTable("NewDev", targetTableId, session);
		assertThat(result).isNotNull();
		assertThat(result).containsKey("developer");
		assertThat(result).containsKey("table");

		Map<String, Object> developerMap = (Map<String, Object>) result.get("developer");
		assertThat(developerMap).containsKey("id").containsValue(1L);
		assertThat(developerMap).containsKey("name").containsValue("NewDev");
		assertThat(developerMap).containsKey("sessionId").containsValue("newSession123");

		Map<String, Object> tableMap = (Map<String, Object>) result.get("table");
		assertThat(tableMap).containsKey("id").containsValue(targetTableId);
		assertThat(tableMap).containsKey("name").containsValue("New Table");
		verify(session).getId();
		verify(pokerTableRepository).findById(targetTableId);
		verify(developerRepository).findBySessionId("newSession123");
		verify(developerRepository).save(any(Developer.class));
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}

	@Test
	void joinTable_existingDeveloperOnSameTable_returnsExistingDeveloper() {
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("existingSession456");
		Long targetTableId = 30L;

		PokerTable table = new PokerTable();
		table.setId(targetTableId);
		table.setName("Existing Table");
		table.setCreatedAt(LocalDateTime.now());

		Developer existingDev = new Developer("existingSession456", "ExistingDev");
		existingDev.setId(5L);
		existingDev.setPokerTable(table);
		existingDev.setVote(5);

		when(pokerTableRepository.findById(targetTableId)).thenReturn(Optional.of(table));
		when(developerRepository.findBySessionId("existingSession456")).thenReturn(Optional.of(existingDev));
		Map<String, Object> result = developerService.joinTable("ExistingDev", targetTableId, session);
		assertThat(result).isNotNull();
		Map<String, Object> developerMap = (Map<String, Object>) result.get("developer");
		assertThat(developerMap).containsKey("id").containsValue(5L);
		assertThat(developerMap).containsKey("name").containsValue("ExistingDev");
		assertThat(existingDev.getPokerTable().getId()).isEqualTo(targetTableId);
		assertThat(existingDev.getVote()).isEqualTo(5);
		verify(session).getId();
		verify(pokerTableRepository).findById(targetTableId);
		verify(developerRepository).findBySessionId("existingSession456");
		verify(developerRepository, never()).save(any());
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}

	@Test
	void joinTable_existingDeveloperOnDifferentTable_updatesDeveloperTableAndResetsVote() {
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionToMove");
		Long oldTableId = 100L;
		Long newTableId = 200L;

		PokerTable oldTable = new PokerTable();
		oldTable.setId(oldTableId);
		oldTable.setName("Old Table");
		oldTable.setCreatedAt(LocalDateTime.now());

		PokerTable newTable = new PokerTable();
		newTable.setId(newTableId);
		newTable.setName("New Table");
		newTable.setCreatedAt(LocalDateTime.now());


		Developer existingDev = new Developer("sessionToMove", "MovingDev");
		existingDev.setId(10L);
		existingDev.setPokerTable(oldTable);
		existingDev.setVote(8);


		when(pokerTableRepository.findById(newTableId)).thenReturn(Optional.of(newTable));
		when(developerRepository.findBySessionId("sessionToMove")).thenReturn(Optional.of(existingDev));
		when(developerRepository.save(any(Developer.class))).thenReturn(existingDev);
		Map<String, Object> result = developerService.joinTable("MovingDev", newTableId, session);
		assertThat(result).isNotNull();
		Map<String, Object> developerMap = (Map<String, Object>) result.get("developer");
		assertThat(developerMap).containsKey("id").containsValue(10L);
		assertThat(developerMap).containsKey("name").containsValue("MovingDev");

		assertThat(existingDev.getPokerTable().getId()).isEqualTo(newTableId);
		assertThat(existingDev.getVote()).isNull();
		verify(session).getId();
		verify(pokerTableRepository).findById(newTableId);
		verify(developerRepository).findBySessionId("sessionToMove");
		verify(developerRepository).save(existingDev);
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}


	@Test
	void joinTable_tableNotFound_throwsException() {
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionError");
		Long nonExistentTableId = 999L;

		when(pokerTableRepository.findById(nonExistentTableId)).thenReturn(Optional.empty());
		assertThrows(NotFoundException.class, () -> developerService.joinTable("Test", nonExistentTableId, session));
		verify(session).getId();
		verify(pokerTableRepository).findById(nonExistentTableId);
		verify(developerRepository, never()).findBySessionId(anyString());
		verify(developerRepository, never()).save(any());
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}

	@Test
	void joinTable_existingDeveloperWithNullTable_updatesDeveloperTableAndResetsVote() {
		HttpSession session = mock(HttpSession.class);
		when(session.getId()).thenReturn("sessionWithNullTable");
		Long targetTableId = 50L;

		PokerTable targetTable = new PokerTable();
		targetTable.setId(targetTableId);
		targetTable.setName("Target Table");
		targetTable.setCreatedAt(LocalDateTime.now());

		Developer existingDevWithNullTable = new Developer("sessionWithNullTable", "DevWithNullTable");
		existingDevWithNullTable.setId(15L);
		existingDevWithNullTable.setVote(7);


		when(pokerTableRepository.findById(targetTableId)).thenReturn(Optional.of(targetTable));
		when(developerRepository.findBySessionId("sessionWithNullTable")).thenReturn(Optional.of(existingDevWithNullTable));
		when(developerRepository.save(any(Developer.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Map<String, Object> result = developerService.joinTable("DevWithNullTable", targetTableId, session);
		assertThat(result).isNotNull();
		Map<String, Object> developerMap = (Map<String, Object>) result.get("developer");
		assertThat(developerMap).containsKey("id").containsValue(15L);
		assertThat(developerMap).containsKey("name").containsValue("DevWithNullTable");

		assertThat(existingDevWithNullTable.getPokerTable().getId()).isEqualTo(targetTableId);
		assertThat(existingDevWithNullTable.getVote()).isNull();
		verify(session).getId();
		verify(pokerTableRepository).findById(targetTableId);
		verify(developerRepository).findBySessionId("sessionWithNullTable");
		verify(developerRepository).save(existingDevWithNullTable);
		verify(pokerTableRepository, never()).findByIsClosedFalse();
	}

	@Test
	void getActiveTable_noActiveTable_createsNew() {
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty());

		PokerTable newTable = new PokerTable();
		newTable.setId(1L);
		newTable.setName("Blank");
		newTable.setCreatedAt(LocalDateTime.now());

		when(pokerTableService.createPokerTable("Blank")).thenReturn(newTable);
		PokerTable result = developerService.getActiveTable();
		assertThat(result).isEqualTo(newTable);
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getName()).isEqualTo("Blank");
		verify(pokerTableRepository).findByIsClosedFalse();
		verify(pokerTableService).createPokerTable("Blank");
		verifyNoMoreInteractions(pokerTableService);
		verifyNoInteractions(developerRepository);
	}


	@Test
	void getActiveTable_activeTableExists_returnsExisting() {
		PokerTable existingTable = new PokerTable();
		existingTable.setId(10L);
		existingTable.setName("Existing Active");
		existingTable.setCreatedAt(LocalDateTime.now());
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.of(existingTable));
		PokerTable result = developerService.getActiveTable();
		assertThat(result).isEqualTo(existingTable);
		assertThat(result.getId()).isEqualTo(10L);
		verify(pokerTableRepository).findByIsClosedFalse();
		verifyNoInteractions(pokerTableService);
	}

	@Test
	void registerDeveloper_success() {
		String name = "New Dev";
		String email = "newdev@example.com";
		String password = "rawPassword";
		String encodedPassword = "encodedPassword";

		when(developerRepository.findByEmail(email)).thenReturn(Optional.empty());
		when(bCryptPasswordEncoder.encode(password)).thenReturn(encodedPassword);
		when(developerRepository.save(any(Developer.class))).thenAnswer(invocation -> {
			Developer dev = invocation.getArgument(0);
			dev.setId(2L);
			return dev;
		});
		Developer registeredDeveloper = developerService.registerDeveloper(name, email, password);
		assertThat(registeredDeveloper).isNotNull();
		assertThat(registeredDeveloper.getName()).isEqualTo(name);
		assertThat(registeredDeveloper.getEmail()).isEqualTo(email);
		assertThat(registeredDeveloper.getPassword()).isEqualTo(encodedPassword);
		assertThat(registeredDeveloper.getId()).isEqualTo(2L);
		verify(developerRepository).findByEmail(email);
		verify(bCryptPasswordEncoder).encode(password);
		verify(developerRepository).save(any(Developer.class));
	}

	@Test
	void registerDeveloper_emailAlreadyExists_throwsException() {
		String name = "Existing Dev";
		String email = "test@example.com";
		String password = "rawPassword";

		when(developerRepository.findByEmail(email)).thenReturn(Optional.of(testDeveloper));
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> developerService.registerDeveloper(name, email, password));

		assertThat(exception.getMessage()).isEqualTo("Developer with this email already exists.");
		verify(developerRepository).findByEmail(email);
		verifyNoInteractions(bCryptPasswordEncoder);
		verify(developerRepository, never()).save(any(Developer.class));
	}

	@Test
	void loginDeveloper_success() {
		String email = "test@example.com";
		String password = "rawPassword";
		String jwtToken = "mockedJwtToken";

		UserDetails userDetails = User.withUsername(email).password("encodedPassword").roles("DEVELOPER").build();

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
		when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
		when(jwtUtil.generateToken(userDetails)).thenReturn(jwtToken);
		String resultToken = developerService.loginDeveloper(email, password);
		assertThat(resultToken).isEqualTo(jwtToken);
		verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(email, password));
		verify(userDetailsService).loadUserByUsername(email);
		verify(jwtUtil).generateToken(userDetails);
	}

	@Test
	void loginDeveloper_invalidCredentials_throwsException() {
		String email = "test@example.com";
		String password = "wrongPassword";

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new BadCredentialsException("Invalid credentials"));
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> developerService.loginDeveloper(email, password));

		assertThat(exception.getMessage()).isEqualTo("Invalid email or password.");
		verify(authenticationManager).authenticate(new UsernamePasswordAuthenticationToken(email, password));
		verifyNoInteractions(userDetailsService);
		verifyNoInteractions(jwtUtil);
	}

	@Test
	void getDeveloperParticipationHistory_developerExists_returnsHistory() {
		Long developerId = 1L;
		Set<Participation> history = new HashSet<>();
		history.add(new Participation(testDeveloper, testPokerTable, 5));
		history.add(new Participation(testDeveloper, new PokerTable(11L, "Another Table", LocalDateTime.now(), false), 8));

		when(developerRepository.findById(developerId)).thenReturn(Optional.of(testDeveloper));
		when(participationRepository.findByDeveloperId(developerId)).thenReturn(history);
		Set<Participation> result = developerService.getDeveloperParticipationHistory(developerId);
		assertThat(result).isEqualTo(history);
		assertThat(result).hasSize(2);
		verify(developerRepository).findById(developerId);
		verify(participationRepository).findByDeveloperId(developerId);
	}

	@Test
	void getDeveloperParticipationHistory_developerNotFound_throwsException() {
		Long developerId = 999L;
		when(developerRepository.findById(developerId)).thenReturn(Optional.empty());
		NotFoundException exception = assertThrows(NotFoundException.class,
				() -> developerService.getDeveloperParticipationHistory(developerId));

		assertThat(exception.getMessage()).isEqualTo("Developer not found with ID: " + developerId);
		verify(developerRepository).findById(developerId);
		verifyNoInteractions(participationRepository);
	}

	@Test
	void getDeveloperByEmail_developerExists_returnsDeveloper() {
		String email = "abc@def.pl";
		when(developerRepository.findByEmail(email)).thenReturn(Optional.of(testDeveloper));
		Developer result = developerService.getDeveloperByEmail(email);
		assertThat(result).isEqualTo(testDeveloper);
		verify(developerRepository).findByEmail(email);
	}

	@Test
	void getDeveloperByEmail_developerNotFound_throwsException() {
		String email = "def@ghi.pl";
		when(developerRepository.findByEmail(email)).thenReturn(Optional.empty());
		NotFoundException exception = assertThrows(NotFoundException.class,
				() -> developerService.getDeveloperByEmail(email));
		assertThat(exception.getMessage()).isEqualTo("Developer not found with email: " + email);
		verify(developerRepository).findByEmail(email);
	}
}
