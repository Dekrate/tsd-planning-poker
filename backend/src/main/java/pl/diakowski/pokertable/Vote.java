package pl.diakowski.pokertable;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import pl.diakowski.pokertable.developer.Developer;
import pl.diakowski.pokertable.pokertable.PokerTable;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
public class Vote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String cardValue;

	@Column(nullable = false)
	private LocalDateTime votedAt;

	@ManyToOne(optional = false)
	@JoinColumn(name = "developer_id")
	private Developer developer;

	@ManyToOne(optional = false)
	@JoinColumn(name = "poker_table_id")
	private PokerTable pokerTable;

	public Vote(String cardValue, Developer developer, PokerTable pokerTable) {
		this.cardValue = cardValue;
		this.votedAt = LocalDateTime.now();
		this.developer = developer;
		this.pokerTable = pokerTable;
	}
}
