import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Button } from 'react-bootstrap;
import { DeveloperForm } from '../components/DeveloperForm';
import { Voting } from '../components/Voting';
import {
  joinTable,
  getDevelopers,
  getTableById,
  createTable,
} from '../services/api';


const API_URL = 'http://localhost:8080';

type PageMode = 'loading' | 'initial' | 'joining-specific' | 'on-table' | 'error';


export const HomePage = () => {
  const [searchParams] = useSearchParams(); 
  const navigate = useNavigate();

  const [table, setTable] = useState<any>(null);
  const [developer, setDeveloper] = useState<any>(null); 
  const [developersList, setDevelopersList] = useState<any[]>([]);

  const [mode, setMode] = useState<PageMode>('loading');
  const [error, setError] = useState<string | null>(null);
  const [tableDetailsForJoin, setTableDetailsForJoin] = useState<any>(null); 
  const [isProcessing, setIsProcessing] = useState(false); 

  const [currentUserHasVoted, setCurrentUserHasVoted] = useState(false); 

  useEffect(() => {
    const init = async () => {
      const tableIdFromUrl = searchParams.get('tableId'); 

      if (tableIdFromUrl) {
        setMode('loading');
        try {
          const specificTable = await getTableById(Number(tableIdFromUrl));

          if (!specificTable) {
            setMode('error');
            setError(`Planning session not found with ID ${tableIdFromUrl}. Please check the link.`);

          } else if (specificTable.isClosed) {
            setMode('error');
            setError(`This planning session (${specificTable.name}) is closed.`);

          } else {
            setTableDetailsForJoin(specificTable);
            setMode('joining-specific');
          }

        } catch (err: any) {
          console.error("Error loading specific table details:", err);
          setMode('error');
          if (err.response?.status === 404) {
            setError(`Planning session not found with ID ${tableIdFromUrl}. Please check the link.`);
          } else {
            setError(`Failed to load planning session details for ID ${tableIdFromUrl}. Please try again.`);
          }
        } finally {
          setIsProcessing(false);
        }

      } else {
        setMode('initial');
        setIsProcessing(false);
      }
    };
    init();
  }, [searchParams]);


  useEffect(() => {
    const checkVoteStatus = async () => {
      if (developer?.id && table?.id) { // Sprawdź, czy developer i tabela są ustawione
        try {
          const response = await fetch(`${API_URL}/developers/${developer.id}/has-voted`);
          if (!response.ok) throw new Error('Failed to check vote status');
          const hasNotVoted = await response.json(); // True jeśli vote == null
          setCurrentUserHasVoted(!hasNotVoted);
        } catch (err) {
          console.error("Failed to check vote status:", err);
          setCurrentUserHasVoted(false);
        }
      } else {
        setCurrentUserHasVoted(false);
      }
    };
    checkVoteStatus();
  }, [developer, table]); 


  useEffect(() => {
    let interval: NodeJS.Timeout | undefined;
    
    if (currentUserHasVoted && table?.id) {
      interval = setInterval(async () => {
        try {
          const devs = await getDevelopers(table.id);
          setDevelopersList(devs);
        } catch (err) {
          console.error("Failed to fetch developers during polling:", err);
          
        }
      }, 3000);
    }

    
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
    
  }, [currentUserHasVoted, table?.id]);


  
  const handleJoin = async (name: string) => {
    
    let targetTableId: number | undefined = tableDetailsForJoin?.id;

    if (targetTableId === undefined) {
      console.error("Cannot join: No target table ID determined.");
      setError("Cannot join session. Invalid table ID or session details missing.");
      setMode('error');
      return;
    }

    setIsProcessing(true);
    setError(null);
    try {
      const response = await joinTable(name, targetTableId);

      setDeveloper(response.developer);
      setTable(response.table); 
      setMode('on-table'); 

      const devs = await getDevelopers(response.table.id);
      setDevelopersList(devs);

      const tableIdFromUrl = searchParams.get('tableId');
      if (!tableIdFromUrl || Number(tableIdFromUrl) !== response.table.id) {
        navigate(`/join-session?tableId=${response.table.id}`, { replace: true });
      }


    } catch (err: any) {
      console.error("Error joining session:", err);
      setError(`Failed to join session: ${err.response?.data?.message || err.message || 'Unknown error'}`);
      setMode('error'); 
    } finally {
      setIsProcessing(false); 
    }
  };


  const handleCreateTable = async () => {
    setIsProcessing(true);
    setError(null);
    try {
      const newTable = await createTable();
      navigate(`/join-session?tableId=${newTable.id}`);

    } catch (err: any) {
      console.error("Error creating table:", err);
      setError(`Failed to create session: ${err.response?.data?.message || err.message || 'Unknown error'}`);
      setIsProcessing(false); 
    }
  }

  const handleVoteSuccess = async () => {
    setCurrentUserHasVoted(true); 
    if (table?.id) {
      try {
        const devs = await getDevelopers(table.id);
        setDevelopersList(devs);
      } catch (err) {
        console.error("Failed to re-fetch developers after vote:", err);
      }
    }
  };


  if (mode === 'loading') {
    return <div className="container mt-5">Loading planning session...</div>;
  }

  if (mode === 'error') {
    return <div className="container mt-5 text-danger">Error: {error || "An unexpected error occurred."}</div>;
  }

  return (
      <div className="container mt-5">
        {mode === 'on-table' && table && <h1 className="mb-4">Planning Poker: {table.name}</h1>}
        {mode === 'joining-specific' && tableDetailsForJoin && <h1 className="mb-4">Join Planning Poker: {tableDetailsForJoin.name}</h1>}
        {mode === 'initial' && <h1 className="mb-4">Planning Poker</h1>}


        {mode === 'initial' && !developer && (
            <>
              <p>Welcome! Create a new planning session or join one via an invite link.</p>
              <Button
                  variant="primary"
                  onClick={handleCreateTable}
                  className="mt-3"
                  disabled={isProcessing} 
              >
                {isProcessing ? 'Creating...' : 'Create New Session'}
              </Button>
            </>
        )}

        {mode === 'joining-specific' && !developer && tableDetailsForJoin && (
            <>
              <p>Enter your name to join session: <strong>{tableDetailsForJoin.name}</strong></p>
              <DeveloperForm onJoin={handleJoin} isJoining={isProcessing} />
            </>
        )}


        {mode === 'on-table' && developer && table && (
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
                                {dev.vote != null && dev.vote !== 0 ? dev.vote : '...'}
                       </span>
                          ) : (
                              <span className="text-muted">
                         {dev.id === developer.id ?
                             (dev.vote != null && dev.vote !== 0 ? `Your vote: ${dev.vote}` : 'Waiting for your vote') :
                             'Vote hidden'} 
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

              {mode === 'on-table' && table?.id && (
                  <div className="mt-4">
                    <h5>Invite Teammates:</h5>
                    <p>Share this link: <a href={`${window.location.origin}/join-session?tableId=${table.id}`} target="_blank" rel="noopener noreferrer">{`${window.location.origin}/join-session?tableId=${table.id}`}</a></p>
                  </div>
              )}
            </>
        )}

        {isProcessing && mode !== 'loading' && <div className="mt-3">Processing action...</div>}

      </div>
  );
};