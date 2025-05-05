package pl.xsd.pokertable.userstory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserStoryRepository extends JpaRepository<UserStory, Long> {
	Set<UserStory> findByPokerTableId(Long pokerTableId);
}