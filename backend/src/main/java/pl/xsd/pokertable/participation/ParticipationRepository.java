package pl.xsd.pokertable.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
	Set<Participation> findByDeveloperId(Long developerId);
}
