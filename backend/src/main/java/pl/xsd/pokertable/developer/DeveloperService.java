package pl.xsd.pokertable.developer;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.xsd.pokertable.config.JwtUtil;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableDto;
import pl.xsd.pokertable.pokertable.PokerTableRepository;
import pl.xsd.pokertable.pokertable.PokerTableService;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DeveloperService {

	private final DeveloperRepository developerRepository;
	private final PokerTableRepository pokerTableRepository;
	private final PokerTableService pokerTableService;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;
	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final JwtUtil jwtUtil;

	public DeveloperService(DeveloperRepository developerRepository, PokerTableRepository pokerTableRepository, PokerTableService pokerTableService, BCryptPasswordEncoder bCryptPasswordEncoder, AuthenticationManager authenticationManager, UserDetailsService userDetailsService, JwtUtil jwtUtil) {
		this.developerRepository = developerRepository;
		this.pokerTableRepository = pokerTableRepository;
		this.pokerTableService = pokerTableService;
		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtUtil = jwtUtil;
	}

	public PokerTable getActiveTable() {
		return pokerTableRepository.findByIsClosedFalse()
				.orElseGet(() -> pokerTableService.createPokerTable("Blank"));
	}

	@Transactional
	public void vote(Long developerId, Long tableId, Integer vote) {
		if (vote == null) {
			throw new IllegalArgumentException("Vote cannot be null");
		}

		if (vote < 1 || vote > 13) {
			throw new IllegalArgumentException("Vote must be between 1 and 13");
		}

		Developer developer = developerRepository.findById(developerId)
				.orElseThrow(() -> new NotFoundException("Developer not found"));

		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found"));

		if (!developer.getPokerTable().getId().equals(pokerTable.getId())) {
			throw new IllegalArgumentException("Developer does not belong to this poker table.");
		}

		developer.setVote(vote);
		developerRepository.save(developer);
	}

	public DeveloperDto getDeveloper(Long developerId) {
		Developer developer = developerRepository.findById(developerId)
				.orElseThrow(() -> new NotFoundException("Developer not found"));
		return new DeveloperDto(developer);
	}

	public boolean hasVoted(Long developerId) {
		Developer developer = developerRepository.findById(developerId)
				.orElseThrow(() -> new NotFoundException("Developer not found"));
		return developer.hasVoted();
	}

	@Transactional
	public Set<DeveloperDto> getDevelopersForPokerTable(Long tableId) {
		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found"));
		return pokerTable.getDevelopers().stream()
				.map(DeveloperDto::new)
				.collect(Collectors.toSet());
	}

	@Transactional
	public Developer createDeveloper(Long pokerTableId, Developer developer) {
		PokerTable pokerTable = pokerTableRepository.findById(pokerTableId)
				.orElseThrow(() -> new IllegalArgumentException("Tablica pokerowa o podanym ID nie istnieje"));

		developer.setPokerTable(pokerTable);
		return developerRepository.save(developer);
	}

	@Transactional
	public JoinResponseDto joinTable(String email, Long tableId) {
		PokerTable table = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found with ID: " + tableId));

		Developer developer = developerRepository.findByEmail(email)
				.orElseThrow(() -> new NotFoundException("Developer not found with email: " + email));

		if (developer.getPokerTable() == null || !developer.getPokerTable().getId().equals(tableId)) {
			developer.setPokerTable(table);
			developer.setVote(null);
			developerRepository.save(developer);
		}

		return new JoinResponseDto(new DeveloperDto(developer), new PokerTableDto(table));
	}

	@Transactional
	public DeveloperDto registerDeveloper(String name, String email, String password) {
		if (developerRepository.findByEmail(email).isPresent()) {
			throw new IllegalArgumentException("Developer with this email already exists.");
		}

		String encodedPassword = bCryptPasswordEncoder.encode(password);
		Developer newDeveloper = new Developer(name, email, encodedPassword);
		Developer savedDeveloper = developerRepository.save(newDeveloper);
		return new DeveloperDto(savedDeveloper);
	}

	@Transactional
	public String loginDeveloper(String email, String password) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(email, password)
			);
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid email or password.", e);
		}
		final UserDetails userDetails = userDetailsService.loadUserByUsername(email);
		return jwtUtil.generateToken(userDetails);
	}

	public Developer getDeveloperByEmail(String email) {
		return developerRepository.findByEmail(email)
				.orElseThrow(() -> new NotFoundException("Developer not found with email: " + email));
	}
}
