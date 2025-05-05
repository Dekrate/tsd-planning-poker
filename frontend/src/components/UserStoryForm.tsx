import { useState, useEffect } from 'react';
import { Button, Form } from 'react-bootstrap';
import { UserStory } from '../services/api';

interface UserStoryFormProps {
    onSubmit: (storyData: { title: string; description?: string; estimatedPoints?: number | null }) => void;
    initialData?: UserStory | null;
    isSubmitting: boolean;
    onCancel?: () => void;
}

export const UserStoryForm = ({ onSubmit, initialData, isSubmitting, onCancel }: UserStoryFormProps) => {
    const [title, setTitle] = useState(initialData?.title || '');
    const [description, setDescription] = useState(initialData?.description || '');
    const [estimatedPoints, setEstimatedPoints] = useState<string>(initialData?.estimatedPoints?.toString() || '');

    useEffect(() => {
        setTitle(initialData?.title || '');
        setDescription(initialData?.description || '');
        setEstimatedPoints(initialData?.estimatedPoints?.toString() || '');
    }, [initialData]);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (!title.trim()) return;

        const storyData = {
            title: title.trim(),
            description: description.trim() || undefined,
            estimatedPoints: estimatedPoints.trim() === '' ? null : parseInt(estimatedPoints.trim(), 10)
        };
        onSubmit(storyData);
    };

    return (
        <Form onSubmit={handleSubmit} className="mb-4 p-3 border rounded">
            <h5>{initialData ? 'Edit User Story' : 'Add New User Story'}</h5>
            <Form.Group className="mb-3" controlId="formStoryTitle">
                <Form.Label>Title</Form.Label>
                <Form.Control
                    type="text"
                    placeholder="Enter story title"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    required
                    disabled={isSubmitting}
                />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formStoryDescription">
                <Form.Label>Description (Optional)</Form.Label>
                <Form.Control
                    as="textarea"
                    rows={3}
                    placeholder="Enter description"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    disabled={isSubmitting}
                />
            </Form.Group>

            <Form.Group className="mb-3" controlId="formStoryPoints">
                <Form.Label>Estimated Points (Optional)</Form.Label>
                <Form.Control
                    type="number"
                    placeholder="e.g. 5"
                    value={estimatedPoints}
                    onChange={(e) => setEstimatedPoints(e.target.value)}
                    disabled={isSubmitting}
                    min="0"
                />
            </Form.Group>

            <Button variant="primary" type="submit" disabled={isSubmitting}>
                {isSubmitting ? (initialData ? 'Saving...' : 'Adding...') : (initialData ? 'Save Changes' : 'Add Story')}
            </Button>
            {initialData && onCancel && (
                <Button variant="secondary" onClick={onCancel} className="ms-2" disabled={isSubmitting}>
                    Cancel
                </Button>
            )}
        </Form>
    );
};