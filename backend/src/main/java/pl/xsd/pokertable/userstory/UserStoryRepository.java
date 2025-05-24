package pl.xsd.pokertable.userstory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.xsd.pokertable.pokertable.PokerTable;

import java.util.Set;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, Long> {

	Set<UserStory> findByPokerTable(PokerTable pokerTable);
}