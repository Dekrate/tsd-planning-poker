import { Button, ListGroup } from 'react-bootstrap';
import { UserStory } from '../services/api';
import { UserStoryForm } from './UserStoryForm';

interface UserStoryListProps {
    userStories: UserStory[];
    onEditClick: (storyId: number) => void;
    onDeleteClick: (storyId: number) => void;
    onUpdateSubmit: (storyId: number, storyData: { title: string; description?: string; estimatedPoints?: number | null }) => void;
    onCancelEdit: () => void;
    editingStoryId: number | null;
    isSubmitting: boolean; // To disable buttons/form during submit
}

export const UserStoryList = ({
                                  userStories,
                                  onEditClick,
                                  onDeleteClick,
                                  onUpdateSubmit,
                                  onCancelEdit,
                                  editingStoryId,
                                  isSubmitting
                              }: UserStoryListProps) => {

    const sortedStories = [...userStories].sort((a, b) => (a.id || 0) - (b.id || 0)); // Sort by ID

    return (
        <div className="mb-4">
            <h3>User Stories ({userStories.length})</h3>
            {userStories.length === 0 ? (
                <p>No user stories added yet.</p>
            ) : (
                <ListGroup>
                    {sortedStories.map(story => (
                        editingStoryId === story.id ? (
                            <ListGroup.Item key={story.id} className="bg-light">
                                <UserStoryForm
                                    initialData={story}
                                    onSubmit={(data) => onUpdateSubmit(story.id, data)}
                                    onCancel={onCancelEdit}
                                    isSubmitting={isSubmitting}
                                />
                            </ListGroup.Item>
                        ) : (
                            <ListGroup.Item key={story.id} className="d-flex justify-content-between align-items-center">
                                <div>
                                    <h5>{story.title}</h5>
                                    {story.description && <p className="text-muted mb-1">{story.description}</p>}
                                    {story.estimatedPoints != null && (
                                        <span className="badge bg-info text-dark">Points: {story.estimatedPoints}</span>
                                    )}
                                </div>
                                <div>
                                    <Button
                                        variant="outline-secondary"
                                        size="sm"
                                        onClick={() => onEditClick(story.id)}
                                        className="me-2"
                                        disabled={isSubmitting}
                                    >
                                        Edit
                                    </Button>
                                    <Button
                                        variant="outline-danger"
                                        size="sm"
                                        onClick={() => onDeleteClick(story.id)}
                                        disabled={isSubmitting}
                                    >
                                        Delete
                                    </Button>
                                </div>
                            </ListGroup.Item>
                        )
                    ))}
                </ListGroup>
            )}
        </div>
    );
};