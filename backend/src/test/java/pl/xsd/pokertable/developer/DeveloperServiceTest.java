package pl.xsd.pokertable.developer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pl.xsd.pokertable.config.JwtUtil;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;
import pl.xsd.pokertable.pokertable.PokerTableService;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
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
		testPokerTable = new PokerTable(1L, "Test Table", false);
		testDeveloper = new Developer(1L, "Test Dev", "test@example.com", "password", testPokerTable);
		testPokerTable.getDevelopers().add(testDeveloper);
	}

	@Test
	void getActiveTable_shouldReturnExistingActiveTable() {
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.of(testPokerTable));

		PokerTable result = developerService.getActiveTable();

		assertNotNull(result);
		assertEquals(testPokerTable.getId(), result.getId());
		verify(pokerTableRepository, times(1)).findByIsClosedFalse();
		verify(pokerTableService, never()).createPokerTable(anyString()); // Nie powinien tworzyć nowego stołu
	}

	@Test
	void getActiveTable_shouldCreateNewTableIfNoneActive() {
		// Symuluj, że nie ma aktywnego stołu
		when(pokerTableRepository.findByIsClosedFalse()).thenReturn(Optional.empty());
		// Symuluj, że pokerTableService tworzy nowy stół
		when(pokerTableService.createPokerTable(anyString())).thenReturn(new PokerTable(2L, "New Table", false));

		PokerTable result = developerService.getActiveTable();

		assertNotNull(result);
		assertEquals(2L, result.getId());
		verify(pokerTableRepository, times(1)).findByIsClosedFalse();
		verify(pokerTableService, times(1)).createPokerTable(anyString()); // Powinien stworzyć nowy stół
	}

	@Test
	void vote_shouldUpdateDeveloperVote() {
		Integer voteValue = 5;
		testDeveloper.setPokerTable(testPokerTable); // Upewnij się, że deweloper jest przypisany do stołu

		when(developerRepository.findById(testDeveloper.getId())).thenReturn(Optional.of(testDeveloper));
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		when(developerRepository.save(any(Developer.class))).thenReturn(testDeveloper);

		developerService.vote(testDeveloper.getId(), testPokerTable.getId(), voteValue);

		assertEquals(voteValue, testDeveloper.getVote());
		verify(developerRepository, times(1)).save(testDeveloper);
	}

	@Test
	void vote_shouldThrowExceptionForNullVote() {
		assertThrows(IllegalArgumentException.class, () ->
				developerService.vote(testDeveloper.getId(), testPokerTable.getId(), null));
	}

	@Test
	void vote_shouldThrowExceptionForInvalidVoteRangeTooLow() {
		assertThrows(IllegalArgumentException.class, () ->
				developerService.vote(testDeveloper.getId(), testPokerTable.getId(), 0));
	}

	@Test
	void vote_shouldThrowExceptionForInvalidVoteRangeTooHigh() {
		assertThrows(IllegalArgumentException.class, () ->
				developerService.vote(testDeveloper.getId(), testPokerTable.getId(), 14));
	}

	@Test
	void vote_shouldThrowNotFoundExceptionForDeveloper() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				developerService.vote(99L, testPokerTable.getId(), 5));
	}

	@Test
	void vote_shouldThrowNotFoundExceptionForPokerTable() {
		when(developerRepository.findById(testDeveloper.getId())).thenReturn(Optional.of(testDeveloper));
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				developerService.vote(testDeveloper.getId(), 99L, 5));
	}

	@Test
	void vote_shouldThrowIllegalArgumentExceptionIfDeveloperNotInTable() {
		PokerTable otherTable = new PokerTable(2L, "Other Table", false);
		testDeveloper.setPokerTable(otherTable); // Deweloper jest przy innym stole

		when(developerRepository.findById(testDeveloper.getId())).thenReturn(Optional.of(testDeveloper));
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));

		assertThrows(IllegalArgumentException.class, () ->
				developerService.vote(testDeveloper.getId(), testPokerTable.getId(), 5));
	}

	@Test
	void getDeveloper_shouldReturnDeveloperDto() {
		when(developerRepository.findById(testDeveloper.getId())).thenReturn(Optional.of(testDeveloper));

		DeveloperDto result = developerService.getDeveloper(testDeveloper.getId());

		assertNotNull(result);
		assertEquals(testDeveloper.getId(), result.getId());
		assertEquals(testDeveloper.getName(), result.getName());
		assertEquals(testDeveloper.getEmail(), result.getEmail());
		assertEquals(testDeveloper.getVote(), result.getVote());
	}

	@Test
	void getDeveloper_shouldThrowNotFoundException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				developerService.getDeveloper(99L));
	}

	@Test
	void hasVoted_shouldReturnFalseIfVoted() {
		testDeveloper.setVote(8);
		when(developerRepository.findById(testDeveloper.getId())).thenReturn(Optional.of(testDeveloper));

		assertFalse(developerService.hasVoted(testDeveloper.getId()));
	}

	@Test
	void hasVoted_shouldReturnTrueIfNoVote() {
		testDeveloper.setVote(null);
		when(developerRepository.findById(testDeveloper.getId())).thenReturn(Optional.of(testDeveloper));

		assertTrue(developerService.hasVoted(testDeveloper.getId()));
	}

	@Test
	void hasVoted_shouldThrowNotFoundException() {
		when(developerRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				developerService.hasVoted(99L));
	}

	@Test
	void getDevelopersForPokerTable_shouldReturnSetOfDeveloperDtos() {
		Developer dev2 = new Developer(2L, "Dev2", "dev2@example.com", "pass2", testPokerTable);
		testPokerTable.getDevelopers().add(dev2);

		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));

		Set<DeveloperDto> result = developerService.getDevelopersForPokerTable(testPokerTable.getId());

		assertNotNull(result);
		assertEquals(2, result.size());
		assertTrue(result.stream().anyMatch(d -> d.getId().equals(testDeveloper.getId())));
		assertTrue(result.stream().anyMatch(d -> d.getId().equals(dev2.getId())));
	}

	@Test
	void getDevelopersForPokerTable_shouldThrowNotFoundException() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				developerService.getDevelopersForPokerTable(99L));
	}

	@Test
	void createDeveloper_shouldCreateAndReturnDeveloper() {
		Developer newDeveloper = new Developer(null, "New Dev", "new@example.com", "newpass", null);
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		when(developerRepository.save(any(Developer.class))).thenReturn(new Developer(2L, "New Dev", "new@example.com", "newpass", testPokerTable));

		Developer createdDeveloper = developerService.createDeveloper(testPokerTable.getId(), newDeveloper);

		assertNotNull(createdDeveloper);
		assertEquals(2L, createdDeveloper.getId());
		assertEquals(testPokerTable.getId(), createdDeveloper.getPokerTable().getId());
		verify(developerRepository, times(1)).save(newDeveloper);
	}

	@Test
	void createDeveloper_shouldThrowIllegalArgumentExceptionIfTableNotFound() {
		Developer newDeveloper = new Developer(null, "New Dev", "new@example.com", "newpass", null);
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () ->
				developerService.createDeveloper(99L, newDeveloper));
	}

	@Test
	void joinTable_shouldJoinDeveloperToTable() {
		Developer devToJoin = new Developer(2L, "Joiner", "join@example.com", "pass", null);
		PokerTable targetTable = new PokerTable(2L, "Target Table", false);

		when(pokerTableRepository.findById(targetTable.getId())).thenReturn(Optional.of(targetTable));
		when(developerRepository.findByEmail(devToJoin.getEmail())).thenReturn(Optional.of(devToJoin));
		when(developerRepository.save(any(Developer.class))).thenReturn(devToJoin);

		JoinResponseDto result = developerService.joinTable(devToJoin.getEmail(), targetTable.getId());

		assertNotNull(result);
		assertEquals(devToJoin.getId(), result.getDeveloper().getId());
		assertEquals(targetTable.getId(), result.getTable().getId());
		assertEquals(targetTable.getId(), devToJoin.getPokerTable().getId());
		assertNull(devToJoin.getVote()); // Głos powinien być zresetowany
		verify(developerRepository, times(1)).save(devToJoin);
	}

	@Test
	void joinTable_shouldNotChangeTableIfAlreadyJoined() {
		testDeveloper.setPokerTable(testPokerTable); // Deweloper już przy stole
		when(pokerTableRepository.findById(testPokerTable.getId())).thenReturn(Optional.of(testPokerTable));
		when(developerRepository.findByEmail(testDeveloper.getEmail())).thenReturn(Optional.of(testDeveloper));

		JoinResponseDto result = developerService.joinTable(testDeveloper.getEmail(), testPokerTable.getId());

		assertNotNull(result);
		assertEquals(testDeveloper.getId(), result.getDeveloper().getId());
		assertEquals(testPokerTable.getId(), result.getTable().getId());
		verify(developerRepository, never()).save(any(Developer.class)); // Nie powinien zapisywać
	}

	@Test
	void joinTable_shouldThrowNotFoundExceptionForTable() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				developerService.joinTable("test@example.com", 99L));
	}

	@Test
	void joinTable_shouldThrowNotFoundExceptionForDeveloper() {
		when(pokerTableRepository.findById(anyLong())).thenReturn(Optional.of(testPokerTable));
		when(developerRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				developerService.joinTable("nonexistent@example.com", testPokerTable.getId()));
	}

	@Test
	void registerDeveloper_shouldCreateAndReturnDeveloperDto() {
		String name = "Reg User";
		String email = "reg@example.com";
		String password = "rawPassword";
		String encodedPassword = "encodedPassword";

		when(developerRepository.findByEmail(email)).thenReturn(Optional.empty());
		when(bCryptPasswordEncoder.encode(password)).thenReturn(encodedPassword);
		when(developerRepository.save(any(Developer.class))).thenAnswer(invocation -> {
			Developer dev = invocation.getArgument(0);
			dev.setId(3L); // Symuluj zapis z ID
			return dev;
		});

		DeveloperDto result = developerService.registerDeveloper(name, email, password);

		assertNotNull(result);
		assertEquals(3L, result.getId());
		assertEquals(name, result.getName());
		assertEquals(email, result.getEmail());
		verify(bCryptPasswordEncoder, times(1)).encode(password);
		verify(developerRepository, times(1)).save(any(Developer.class));
	}

	@Test
	void registerDeveloper_shouldThrowIllegalArgumentExceptionIfEmailExists() {
		when(developerRepository.findByEmail(anyString())).thenReturn(Optional.of(testDeveloper));

		assertThrows(IllegalArgumentException.class, () ->
				developerService.registerDeveloper("Name", "test@example.com", "password"));
	}

	@Test
	void loginDeveloper_shouldReturnJwtToken() {
		String email = "test@example.com";
		String password = "password";
		UserDetails userDetails = mock(UserDetails.class);
		String jwtToken = "mocked.jwt.token";

		when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
		when(jwtUtil.generateToken(userDetails)).thenReturn(jwtToken);

		String result = developerService.loginDeveloper(email, password);

		assertEquals(jwtToken, result);
		verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
		verify(userDetailsService, times(1)).loadUserByUsername(email);
		verify(jwtUtil, times(1)).generateToken(userDetails);
	}

	@Test
	void loginDeveloper_shouldThrowIllegalArgumentExceptionForInvalidCredentials() {
		String email = "test@example.com";
		String password = "wrongpassword";

		when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
				.thenThrow(new RuntimeException("Bad credentials"));

		assertThrows(IllegalArgumentException.class, () ->
				developerService.loginDeveloper(email, password));
	}

	@Test
	void getDeveloperByEmail_shouldReturnDeveloper() {
		when(developerRepository.findByEmail(testDeveloper.getEmail())).thenReturn(Optional.of(testDeveloper));

		Developer result = developerService.getDeveloperByEmail(testDeveloper.getEmail());

		assertNotNull(result);
		assertEquals(testDeveloper.getId(), result.getId());
		assertEquals(testDeveloper.getEmail(), result.getEmail());
	}

	@Test
	void getDeveloperByEmail_shouldThrowNotFoundException() {
		when(developerRepository.findByEmail(anyString())).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () ->
				developerService.getDeveloperByEmail("nonexistent@example.com"));
	}
}
