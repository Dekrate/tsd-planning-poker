package pl.xsd.pokertable.developer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeveloperDto {
	private Long id;
	private String name;
	private String email;
	private Integer vote; // Current vote, if any

	public DeveloperDto(Developer developer) {
		this.id = developer.getId();
		this.name = developer.getName();
		this.email = developer.getEmail();
		this.vote = developer.getVote();
		// Do not include pastTables or pokerTable to avoid lazy initialization issues
	}
}
