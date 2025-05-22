package pl.xsd.pokertable.participation;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.xsd.pokertable.developer.Developer;
import pl.xsd.pokertable.pokertable.PokerTable;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Participation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "developer_id", nullable = false)
	private Developer developer;

	@ManyToOne
	@JoinColumn(name = "poker_table_id", nullable = false)
	private PokerTable pokerTable;

	@Column
	private Integer vote;

	public Participation(Developer developer, PokerTable pokerTable, Integer vote) {
		this.developer = developer;
		this.pokerTable = pokerTable;
		this.vote = vote;
	}
}
