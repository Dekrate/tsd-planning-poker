import { useState } from "react";
import PlayerList from "./PlayerList";
import VotingResults from "./VotingResults";
import Card from "./Card";

const PokerTable = () => {
    const [selectedCard, setSelectedCard] = useState(null);
    const [votes, setVotes] = useState({});
    const [votingEnded, setVotingEnded] = useState(false);

    const players = ["Alice", "Bob", "You"];
    const availableCards = [1, 2, 3, 5, 8];

    const handleVote = (value) => {
        setSelectedCard(value);
        setVotes({ ...votes, You: value }); // Na razie tylko dla Ciebie
    };

    const endVoting = () => setVotingEnded(true);

    return (
        <div style={{ textAlign: "center", padding: "20px" }}>
            <h2>Planning Poker</h2>
            <PlayerList players={players} />

            <h3>Select your card:</h3>
            <div style={{ display: "flex", justifyContent: "center", gap: "10px" }}>
                {availableCards.map((card) => (
                    <Card key={card} value={card} onVote={handleVote} selected={selectedCard === card} />
                ))}
            </div>

            <VotingResults votes={votes} players={players} votingEnded={votingEnded} />

            <button onClick={endVoting} style={{ marginTop: "20px" }}>End Voting</button>
        </div>
    );
};

export default PokerTable;
