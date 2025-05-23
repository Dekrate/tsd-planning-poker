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
	private PokerTable pokerTable; // Represents the CURRENT active table for the developer

	@Column
	private Integer vote;

	// NEW Many-to-Many relationship to track past tables participated in
	@ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinTable(
			name = "developer_past_tables", // Name of the join table
			joinColumns = @JoinColumn(name = "developer_id"), // Column for Developer
			inverseJoinColumns = @JoinColumn(name = "poker_table_id") // Column for PokerTable
	)
	@JsonIgnore // We don't want this in JSON responses to avoid circular references
	private Set<PokerTable> pastTables = new HashSet<>(); // Set of tables the developer has participated in

	public boolean hasVoted() {
		return vote == null;
	}

	public Developer(String name, String email, String password) {
		this.name = name;
		this.email = email;
		this.password = password;
	}

	// Helper method to add a table to past tables
	public void addPastTable(PokerTable table) {
		this.pastTables.add(table);
		table.getPastParticipants().add(this); // Ensure bidirectional relationship is maintained
	}

	// Helper method to remove a table from past tables
	public void removePastTable(PokerTable table) {
		this.pastTables.remove(table);
		table.getPastParticipants().remove(this);
	}
}
