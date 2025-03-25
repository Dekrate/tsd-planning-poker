import { useState } from 'react';
import { Button, Form } from 'react-bootstrap';

export const DeveloperForm = ({ onJoin }: { onJoin: (name: string) => void }) => {
  const [name, setName] = useState('');
  const [isJoining, setIsJoining] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    
    try {
      setIsJoining(true);
      await onJoin(name.trim());
    } finally {
      setIsJoining(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit} className="mb-3">
      <Form.Group className="mb-3" controlId="formName">
        <Form.Label>Enter your name to join the table</Form.Label>
        <Form.Control
          type="text"
          placeholder="Your name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
      </Form.Group>
      <Button 
        variant="success" 
        type="submit" 
        disabled={isJoining}
      >
        {isJoining ? 'Joining...' : 'Join Table'}
      </Button>
    </Form>
  );
};