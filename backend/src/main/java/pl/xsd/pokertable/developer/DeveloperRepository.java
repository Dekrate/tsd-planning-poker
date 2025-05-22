package pl.xsd.pokertable.developer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeveloperRepository extends JpaRepository<Developer, Long> {
	Optional<Developer> findBySessionId(String sessionId);
	Optional<Developer> findByEmail(String email);
}
