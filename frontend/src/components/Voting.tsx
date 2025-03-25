import { useState } from 'react';
import { Button } from 'react-bootstrap';
import { vote } from '../services/api';

const fibNumbers = [1, 2, 3, 5, 8, 13];

export const Voting = ({ 
  developerId,
  tableId,
  onVoteSuccess
}: { 
  developerId: number;
  tableId: number;
  onVoteSuccess: () => void;
}) => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleVote = async (value: number) => {
    try {
      setIsSubmitting(true);
      await vote(developerId, tableId, value);
      onVoteSuccess();
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div>
      <h4 className="mb-3">Select your estimate:</h4>
      <div className="d-flex flex-wrap gap-2">
        {fibNumbers.map((num) => (
          <Button 
            key={num}
            variant="outline-primary"
            onClick={() => handleVote(num)}
            disabled={isSubmitting}
            style={{ width: '60px' }}
          >
            {num}
          </Button>
        ))}
      </div>
    </div>
  );
};