package pl.xsd.pokertable.pokertable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PokerTableRepository extends JpaRepository<PokerTable, Long> {
	Optional<PokerTable> findByIsClosedFalse();

	Set<PokerTable> findAllByIsClosedFalse();
}
