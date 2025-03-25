package pl.xsd.pokertable.pokertable;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.xsd.pokertable.developer.Developer;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@NoArgsConstructor
@Setter
@Getter

public class PokerTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private Boolean isClosed;

	@OneToMany(mappedBy = "pokerTable", fetch = FetchType.EAGER)
	@JsonBackReference // ← Ważna adnotacja
	private Set<Developer> developers;

	public PokerTable(Long id, String name) {
		this.id = id;
		this.name = name;
		this.createdAt = LocalDateTime.now();
		this.isClosed = false;
	}

	public PokerTable(Long id, String name, Boolean isClosed) {
		this.id = id;
		this.name = name;
		this.createdAt = LocalDateTime.now();
		this.isClosed = isClosed;
	}
}
