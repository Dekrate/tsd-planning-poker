package pl.diakowski.pokertable.developer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.diakowski.pokertable.pokertable.PokerTable;
import pl.diakowski.pokertable.pokertable.PokerTableRepository;

import java.util.Set;

@Service
public class DeveloperService {

	private final DeveloperRepository developerRepository;
	private final PokerTableRepository pokerTableRepository;

	public DeveloperService(DeveloperRepository developerRepository, PokerTableRepository pokerTableRepository) {
		this.developerRepository = developerRepository;
		this.pokerTableRepository = pokerTableRepository;
	}

	@Transactional
	public void vote(Long developerId, Long tableId, int vote) {
		if (vote != 1 && vote != 0) {
			throw new IllegalArgumentException("Vote can be either 1 (yes) or 0 (no).");
		}

		Developer developer = developerRepository.findById(developerId)
				.orElseThrow(() -> new IllegalArgumentException("Developer not found"));

		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new IllegalArgumentException("Poker table not found"));

		if (!developer.getPokerTable().equals(pokerTable)) {
			throw new IllegalArgumentException("Developer does not belong to this poker table.");
		}

		developer.setVote(vote);
		developerRepository.save(developer);
	}

	public Developer getDeveloper(Long developerId) {
		return developerRepository.findById(developerId)
				.orElseThrow(() -> new IllegalArgumentException("Developer not found"));
	}

	public boolean hasVoted(Long developerId) {
		Developer developer = getDeveloper(developerId);
		return developer.hasVoted();
	}

	public long countVotes(Long tableId, int voteValue) {
		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new IllegalArgumentException("Poker table not found"));

		return pokerTable.getDevelopers().stream()
				.filter(developer -> developer.hasVoted() && developer.getVote() == voteValue)
				.count();
	}

	public Set<Developer> getDevelopersForPokerTable(Long tableId) {
		PokerTable pokerTable = pokerTableRepository.findById(tableId)
				.orElseThrow(() -> new IllegalArgumentException("Poker table not found"));

		return pokerTable.getDevelopers();
	}

	@Transactional
	public Developer createDeveloper(Long pokerTableId, Developer developer) {
		PokerTable pokerTable = pokerTableRepository.findById(pokerTableId)
				.orElseThrow(() -> new IllegalArgumentException("Tablica pokerowa o podanym ID nie istnieje"));

		developer.setPokerTable(pokerTable);
		return developerRepository.save(developer);
	}

}
