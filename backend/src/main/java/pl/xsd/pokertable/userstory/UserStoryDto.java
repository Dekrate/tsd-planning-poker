package pl.xsd.pokertable.userstory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStoryDto {
	private Long id;
	private String title;
	private String description;
	private Integer estimatedPoints;
	// Do not include pokerTable to avoid lazy initialization issues

	public UserStoryDto(UserStory userStory) {
		this.id = userStory.getId();
		this.title = userStory.getTitle();
		this.description = userStory.getDescription();
		this.estimatedPoints = userStory.getEstimatedPoints();
	}
}
