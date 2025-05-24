package pl.xsd.pokertable.developer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.xsd.pokertable.pokertable.PokerTable;

import java.util.HashSet;
import java.util.Set;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Developer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Name cannot be empty")
	@Column(nullable = false)
	private String name;

	@NotBlank(message = "Email cannot be empty")
	@Email(message = "Email should be valid")
	@Column(nullable = false, unique = true)
	private String email;

	@JsonIgnore
	@NotBlank(message = "Password cannot be empty")
	@Size(min = 6, message = "Password must be at least 6 characters long")
	@Column(nullable = false)
	private String password;

	@ManyToOne
	@JoinColumn(name = "poker_table_id")
	@JsonIgnore
	private PokerTable pokerTable;

	@Column
	private Integer vote;
	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(
			name = "developer_past_tables",
			joinColumns = @JoinColumn(name = "developer_id"),
			inverseJoinColumns = @JoinColumn(name = "poker_table_id")
	)
	@JsonIgnore
	private Set<PokerTable> pastTables = new HashSet<>();

	public Developer(Long id, String name, String email, String password, PokerTable pokerTable) {
		this.id = id;
		this.name = name;
		this.email = email;
		this.password = password;
		this.pokerTable = pokerTable;
	}

	public boolean hasVoted() {
		return vote == null;
	}

	public Developer(String name, String email, String password) {
		this.name = name;
		this.email = email;
		this.password = password;
	}
	public void addPastTable(PokerTable table) {
		this.pastTables.add(table);
		table.getPastParticipants().add(this);
	}
	public void removePastTable(PokerTable table) {
		this.pastTables.remove(table);
		table.getPastParticipants().remove(this);
	}
}
