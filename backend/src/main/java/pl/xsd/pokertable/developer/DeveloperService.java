package pl.xsd.pokertable.developer;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.xsd.pokertable.exception.NotFoundException;
import pl.xsd.pokertable.pokertable.PokerTable;
import pl.xsd.pokertable.pokertable.PokerTableRepository;
import pl.xsd.pokertable.pokertable.PokerTableService;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DeveloperService {

	private final DeveloperRepository developerRepository;
	private final PokerTableRepository pokerTableRepository;
	private final PokerTableService pokerTableService;

	public DeveloperService(DeveloperRepository developerRepository, PokerTableRepository pokerTableRepository, PokerTableService pokerTableService) {
		this.developerRepository = developerRepository;
		this.pokerTableRepository = pokerTableRepository;
		this.pokerTableService = pokerTableService;
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

		if (!developer.getPokerTable().equals(pokerTable)) {
			throw new IllegalArgumentException("Developer does not belong to this poker table.");
		}

		developer.setVote(vote);
		developerRepository.save(developer);
	}

	public Developer getDeveloper(Long developerId) {
		return developerRepository.findById(developerId)
				.orElseThrow(() -> new NotFoundException("Developer not found"));
	}

	public boolean hasVoted(Long developerId) {
		Developer developer = getDeveloper(developerId);
		return developer.hasVoted();
	}

	public Set<Developer> getDevelopersForPokerTable(Long tableId) {
		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new NotFoundException("Poker table not found"));

		return pokerTable.getDevelopers();
	}

	@Transactional
	public Developer createDeveloper(Long pokerTableId, Developer developer) {
		PokerTable pokerTable = pokerTableRepository.findById(pokerTableId)
				.orElseThrow(() -> new IllegalArgumentException("Tablica pokerowa o podanym ID nie istnieje"));

		developer.setPokerTable(pokerTable);
		return developerRepository.save(developer);
	}

	public Map<String, Object> joinTable(String name, HttpSession session) {
		PokerTable table = getActiveTable();
		String sessionId = session.getId();

		Developer developer = developerRepository.findBySessionId(sessionId)
				.orElseGet(() -> {
					Developer newDev = new Developer(sessionId, name);
					newDev.setPokerTable(table);
					newDev.setVote(0); // Domyślna wartość
					return developerRepository.save(newDev);
				});

		return Map.of(
				"developer", Map.of(
						"id", developer.getId(),
						"name", developer.getName(),
						"sessionId", developer.getSessionId()
				),
				"table", Map.of(
						"id", table.getId(),
						"name", table.getName(),
						"createdAt", table.getCreatedAt()
				)
		);
	}
}
