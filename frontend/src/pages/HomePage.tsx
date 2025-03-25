import { useEffect, useState } from 'react';
import { DeveloperForm } from '../components/DeveloperForm';
import { Voting } from '../components/Voting';
import { getActiveTable, joinTable, getDevelopers, vote } from '../services/api';


const API_URL = 'http://localhost:8080';

export const HomePage = () => {
  const [table, setTable] = useState<any>(null);
  const [developer, setDeveloper] = useState<any>(null);
  const [developersList, setDevelopersList] = useState<any[]>([]);
  const [currentUserHasVoted, setCurrentUserHasVoted] = useState(true);

  useEffect(() => {
    const init = async () => {
      const activeTable = await getActiveTable();
      setTable(activeTable);
      
      if (activeTable?.id) {
        const devs = await getDevelopers(activeTable.id);
        setDevelopersList(devs);
      }
    };
    init();
  }, []);

  useEffect(() => {
    const checkVoteStatus = async () => {
      if (developer?.id) {
        const response = await fetch(
          `${API_URL}/developers/${developer.id}/has-voted`
        );
        const hasVoted = await response.json();
        setCurrentUserHasVoted(hasVoted);
      }
    };
    checkVoteStatus();
  }, [developer]);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (currentUserHasVoted) {
      interval = setInterval(async () => {
        if (table?.id) {
          const devs = await getDevelopers(table.id);
          setDevelopersList(devs);
        }
      }, 3000);
    }
    return () => clearInterval(interval);
  }, [currentUserHasVoted, table?.id]);

  const handleJoin = async (name: string) => {
    const response = await joinTable(name);
    setDeveloper(response.developer);
    localStorage.setItem('sessionId', response.developer.sessionId);
  };

  const handleVoteSuccess = async () => {
    setCurrentUserHasVoted(true);
    if (table?.id) {
      const devs = await getDevelopers(table.id);
      setDevelopersList(devs);
    }
  };

  if (!table) return <div>Loading...</div>;

  return (
    <div className="container mt-5">
      <h1 className="mb-4">Planning Poker: {table.name}</h1>
      
      {!developer ? (
        <DeveloperForm onJoin={handleJoin} />
      ) : (
        <>
          <div className="card mb-4">
            <div className="card-body">
              <h3 className="card-title">Participants ({developersList.length})</h3>
              <ul className="list-group">
                {developersList.map(dev => (
                  <li 
                    key={dev.id} 
                    className="list-group-item d-flex justify-content-between align-items-center"
                  >
                    <span>
                      {dev.name} 
                      {dev.id === developer.id && " (You)"}
                    </span>
                    
                    {currentUserHasVoted ? (
                      <span className="badge bg-primary rounded-pill">
                        {dev.vote || 'Not voted yet'}
                      </span>
                    ) : (
                      <span className="text-muted">
                        {dev.id === developer.id ? 
                          (dev.vote ? `Your vote: ${dev.vote}` : 'Waiting for your vote') : 
                          'Hidden until you vote'}
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          </div>

          {!currentUserHasVoted && (
            <div className="card">
              <div className="card-body">
                <Voting 
                  developerId={developer.id} 
                  tableId={table.id} 
                  onVoteSuccess={handleVoteSuccess}
                />
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
};