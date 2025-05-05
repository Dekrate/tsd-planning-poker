import { useEffect, useState, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Button, Alert } from 'react-bootstrap'; // Added Alert for error display
import { DeveloperForm } from '../components/DeveloperForm';
import { Voting } from '../components/Voting';
import { UserStoryForm } from '../components/UserStoryForm'; // Import UserStoryForm
import { UserStoryList } from '../components/UserStoryList'; // Import UserStoryList

import {
  joinTable,
  getDevelopers,
  getTableById,
  createTable,
  getUserStoriesByTableId, // Import new API calls
  createUserStory,
  updateUserStory,
  deleteUserStory,
  UserStory, // Import UserStory type
} from '../services/api';


type PageMode = 'loading' | 'initial' | 'joining-specific' | 'on-table' | 'error';


export const HomePage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  const [table, setTable] = useState<any>(null);
  const [developer, setDeveloper] = useState<any>(null);
  const [developersList, setDevelopersList] = useState<any[]>([]);
  const [userStories, setUserStories] = useState<UserStory[]>([]); // State for user stories
  const [editingStoryId, setEditingStoryId] = useState<number | null>(null); // State for editing mode

  const [mode, setMode] = useState<PageMode>('loading');
  const [error, setError] = useState<string | null>(null);
  const [tableDetailsForJoin, setTableDetailsForJoin] = useState<any>(null);
  const [isProcessing, setIsProcessing] = useState(false); // For main page actions (join, create)
  const [isUserStoryProcessing, setIsUserStoryProcessing] = useState(false); // For user story actions

  const [copiedMessage, setCopiedMessage] = useState<string | null>(null);
  const [isCopying, setIsCopying] = useState(false);

  const [currentUserHasVoted, setCurrentUserHasVoted] = useState(false);


  // Initial load effect
  useEffect(() => {
    const init = async () => {
      const tableIdFromUrl = searchParams.get('tableId');
      setIsProcessing(true); // Set processing for initial load
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

  // Effect to fetch developers when table and developer state change
  // and poll if user has voted
  useEffect(() => {
    let interval: NodeJS.Timeout | undefined;

    const fetchDevelopers = async (currentTableId: number) => {
      try {
        const devs = await getDevelopers(currentTableId);
        setDevelopersList(devs);
      } catch (err) {
        console.error("Failed to fetch developers:", err);
        // Optionally set an error, but maybe not critical enough to block everything
      }
    };

    const checkVoteStatusAndPoll = async () => {
      if (developer?.id && table?.id) {
        try {
          const response = await fetch(`http://localhost:8080/developers/${developer.id}/has-voted`); // Use direct fetch for simplicity or update api.ts if needed
          if (!response.ok) throw new Error('Failed to check vote status');
          const hasNotVoted = await response.json();
          const userVoted = !hasNotVoted;
          setCurrentUserHasVoted(userVoted);

          // Start polling if user has voted (meaning votes might be revealed soon)
          if (userVoted) {
            // Fetch immediately and then start interval
            fetchDevelopers(table.id);
            interval = setInterval(() => fetchDevelopers(table.id), 3000);
          } else {
            // If user hasn't voted, fetch developers once to get current state (e.g., new joiners)
            fetchDevelopers(table.id);
          }

        } catch (err) {
          console.error("Failed to check vote status or poll:", err);
          setCurrentUserHasVoted(false); // Assume not voted or error
        }
      } else {
        setCurrentUserHasVoted(false);
        setDevelopersList([]); // Clear list if not on table
      }
    };

    if (table?.id && developer?.id) {
      checkVoteStatusAndPoll();
    } else {
      setDevelopersList([]);
      setCurrentUserHasVoted(false);
    }


    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };

  }, [developer, table]); // Depend on developer and table state


  // Effect to fetch user stories when table state changes
  useEffect(() => {
    const fetchUserStories = async () => {
      if (table?.id) {
        try {
          const stories = await getUserStoriesByTableId(table.id);
          setUserStories(stories);
        } catch (err) {
          console.error("Failed to fetch user stories:", err);
          // Optionally set error
        }
      } else {
        setUserStories([]); // Clear stories if not on a table
      }
    };

    fetchUserStories();
  }, [table]); // Depend on table state


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

      // Fetch developers and stories immediately after joining
      const devs = await getDevelopers(response.table.id);
      setDevelopersList(devs);
      const stories = await getUserStoriesByTableId(response.table.id);
      setUserStories(stories);


      const tableIdFromUrl = searchParams.get('tableId');
      if (!tableIdFromUrl || Number(tableIdFromUrl) !== response.table.id) {
        navigate(`/join-session?tableId=${response.table.id}`, { replace: true });
      }


    } catch (err: any) {
      console.error("Error joining session:", err);
      setError(`Failed to join session: ${err.response?.data?.message || err.message || 'Unknown error'}`);
      // Stay in joining-specific mode or move to error mode depending on desired flow
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
    // Developer list polling will start automatically due to useEffect dependency
    // Fetch developers once immediately after voting to update the list
    if (table?.id) {
      try {
        const devs = await getDevelopers(table.id);
        setDevelopersList(devs);
      } catch(err) {
        console.error("Failed to re-fetch developers after vote:", err);
      }
    }
  };

  const handleCopyInviteLink = async () => {
    if (!table?.id) return;

    const inviteLink = `${window.location.origin}/join-session?tableId=${table.id}`;

    setIsCopying(true);
    setCopiedMessage(null);

    try {
      await navigator.clipboard.writeText(inviteLink);
      setCopiedMessage('Link copied to clipboard!');
    } catch (err) {
      console.error("Failed to copy invite link:", err);
      setCopiedMessage('Failed to copy link.');
    } finally {
      setIsCopying(false);
      setTimeout(() => {
        setCopiedMessage(null);
      }, 3000);
    }
  };

  // --- User Story Handlers ---

  const fetchUserStoriesForTable = useCallback(async () => {
    if (table?.id) {
      setIsUserStoryProcessing(true); // Set processing for story list fetch
      try {
        const stories = await getUserStoriesByTableId(table.id);
        setUserStories(stories);
        setError(null); // Clear story-related errors on successful fetch
      } catch (err: any) {
        console.error("Failed to fetch user stories:", err);
        setError(`Failed to load user stories: ${err.response?.data?.message || err.message || 'Unknown error'}`);
      } finally {
        setIsUserStoryProcessing(false);
      }
    } else {
      setUserStories([]);
    }
  }, [table?.id]); // Dependency on table.id

  const handleAddStory = async (storyData: { title: string; description?: string; estimatedPoints?: number | null }) => {
    if (!table?.id) return;
    setIsUserStoryProcessing(true);
    setError(null);
    try {
      await createUserStory(table.id, storyData);
      await fetchUserStoriesForTable(); // Refresh the list
    } catch (err: any) {
      console.error("Error creating user story:", err);
      setError(`Failed to create user story: ${err.response?.data?.message || err.message || 'Unknown error'}`);
    } finally {
      setIsUserStoryProcessing(false);
    }
  };

  const handleEditStoryClick = (storyId: number) => {
    setEditingStoryId(storyId);
    setError(null); // Clear any previous errors when starting edit
  };

  const handleUpdateStory = async (storyId: number, storyData: { title: string; description?: string; estimatedPoints?: number | null }) => {
    if (!table?.id) return;
    setIsUserStoryProcessing(true);
    setError(null);
    try {
      await updateUserStory(storyId, storyData);
      setEditingStoryId(null); // Exit editing mode
      await fetchUserStoriesForTable(); // Refresh the list
    } catch (err: any) {
      console.error("Error updating user story:", err);
      setError(`Failed to update user story: ${err.response?.data?.message || err.message || 'Unknown error'}`);
    } finally {
      setIsUserStoryProcessing(false);
    }
  };

  const handleCancelEditStory = () => {
    setEditingStoryId(null);
    setError(null); // Clear any previous errors when cancelling edit
  };


  const handleDeleteStory = async (storyId: number) => {
    if (!table?.id) return;
    setIsUserStoryProcessing(true);
    setError(null);
    try {
      await deleteUserStory(storyId);
      await fetchUserStoriesForTable(); // Refresh the list
    } catch (err: any) {
      console.error("Error deleting user story:", err);
      setError(`Failed to delete user story: ${err.response?.data?.message || err.message || 'Unknown error'}`);
    } finally {
      setIsUserStoryProcessing(false);
    }
  };


  // --- Render Logic ---

  if (mode === 'loading') {
    return <div className="container mt-5">Loading planning session...</div>;
  }

  if (mode === 'error') {
    return <div className="container mt-5">
      <Alert variant="danger">Error: {error || "An unexpected error occurred."}</Alert>
      {/* Optionally provide a button to go back or try creating a new session */}
      {searchParams.get('tableId') && mode === 'error' && (
          <Button variant="secondary" onClick={() => navigate('/')}>
            Back to Home / Create New Session
          </Button>
      )}
    </div>;
  }

  const isReadyOnTable = mode === 'on-table' && developer && table;

  return (
      <div className="container mt-5">
        {table && <h1 className="mb-4">Planning Poker: {table.name}</h1>}
        {mode === 'joining-specific' && tableDetailsForJoin && <h1 className="mb-4">Join Planning Poker: {tableDetailsForJoin.name}</h1>}
        {mode === 'initial' && !table && !developer && <h1 className="mb-4">Planning Poker</h1>}


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

        {error && mode !== 'loading' && mode !== 'error' && (
            <Alert variant="danger" className="mt-4">{error}</Alert>
        )}


        {isReadyOnTable && (
            <>
              {/* User Story Section */}
              <UserStoryList
                  userStories={userStories}
                  onEditClick={handleEditStoryClick}
                  onDeleteClick={handleDeleteStory}
                  onUpdateSubmit={handleUpdateStory}
                  onCancelEdit={handleCancelEditStory}
                  editingStoryId={editingStoryId}
                  isSubmitting={isUserStoryProcessing} // Pass user story specific processing state
              />

              {/* Add New Story Form (only if not editing) */}
              {editingStoryId === null && (
                  <UserStoryForm
                      onSubmit={handleAddStory}
                      isSubmitting={isUserStoryProcessing} // Pass user story specific processing state
                  />
              )}

              {/* Participants Section */}
              <div className="card mt-4 mb-4">
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

              {/* Voting Section */}
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

              {/* Invite Link Section */}
              {mode === 'on-table' && table?.id && (
                  <div className="mt-4 card">
                    <div className="card-body">
                      <h5 className="card-title">Invite Teammates</h5>
                      <p>Share this link:</p>
                      <div className="d-flex align-items-center mb-2">
                        <a href={`${window.location.origin}/join-session?tableId=${table.id}`} target="_blank" rel="noopener noreferrer" className="me-2 text-break">
                          {`${window.location.origin}/join-session?tableId=${table.id}`}
                        </a>
                        <Button
                            variant="secondary"
                            onClick={handleCopyInviteLink}
                            disabled={isCopying}
                            size="sm"
                        >
                          {isCopying ? 'Copying...' : 'Copy'}
                        </Button>
                      </div>
                      {copiedMessage && (
                          <p className={`mt-2 mb-0 text-${copiedMessage.includes('copied') ? 'success' : 'danger'}`}>
                            {copiedMessage}
                          </p>
                      )}
                    </div>
                  </div>
              )}
            </>
        )}

      </div>
  );
};