import { useEffect, useState, useCallback } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Button, Alert, Card, Form, ListGroup } from 'react-bootstrap';
import { Voting } from '../components/Voting';
import { UserStoryForm } from '../components/UserStoryForm';
import { UserStoryList } from '../components/UserStoryList';

import {
    joinTable,
    getDevelopers,
    getTableById,
    createTable,
    getUserStoriesByTableId,
    createUserStory,
    updateUserStory,
    deleteUserStory,
    UserStory,
    Developer,
    registerDeveloper,
    loginDeveloper,
    logoutDeveloper,
    hasDeveloperVoted,
    exportUserStoriesToCsv,
    getDeveloperById,
    getAllActiveTables,
    getMyClosedTables,
    PokerTable,
    JoinResponse,
    closePokerTable,
    resetAllVotes
} from '../services/api';

interface LoginFormProps {
    onSubmit: (email: string, password: string) => void;
    isSubmitting: boolean;
    error: string | null;
}

const LoginForm = ({ onSubmit, isSubmitting, error }: LoginFormProps) => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit(email, password);
    };

    return (
        <Card className="p-4 shadow-sm">
            <h4 className="mb-3">Login</h4>
            <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3" controlId="loginEmail">
                    <Form.Label>Email address</Form.Label>
                    <Form.Control
                        type="email"
                        placeholder="Enter email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        disabled={isSubmitting}
                    />
                </Form.Group>

                <Form.Group className="mb-3" controlId="loginPassword">
                    <Form.Label>Password</Form.Label>
                    <Form.Control
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        disabled={isSubmitting}
                    />
                </Form.Group>

                {error && <Alert variant="danger">{error}</Alert>}

                <Button variant="primary" type="submit" disabled={isSubmitting}>
                    {isSubmitting ? 'Logging in...' : 'Login'}
                </Button>
            </Form>
        </Card>
    );
};

interface RegisterFormProps {
    onSubmit: (name: string, email: string, password: string) => void;
    isSubmitting: boolean;
    error: string | null;
}

const RegisterForm = ({ onSubmit, isSubmitting, error }: RegisterFormProps) => {
    const [name, setName] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        onSubmit(name, email, password);
    };

    return (
        <Card className="p-4 shadow-sm">
            <h4 className="mb-3">Register</h4>
            <Form onSubmit={handleSubmit}>
                <Form.Group className="mb-3" controlId="registerName">
                    <Form.Label>Name</Form.Label>
                    <Form.Control
                        type="text"
                        placeholder="Enter your name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        required
                        disabled={isSubmitting}
                    />
                </Form.Group>

                <Form.Group className="mb-3" controlId="registerEmail">
                    <Form.Label>Email address</Form.Label>
                    <Form.Control
                        type="email"
                        placeholder="Enter email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        required
                        disabled={isSubmitting}
                    />
                </Form.Group>

                <Form.Group className="mb-3" controlId="registerPassword">
                    <Form.Label>Password</Form.Label>
                    <Form.Control
                        type="password"
                        placeholder="Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        required
                        disabled={isSubmitting}
                    />
                </Form.Group>

                {error && <Alert variant="danger">{error}</Alert>}

                <Button variant="success" type="submit" disabled={isSubmitting}>
                    {isSubmitting ? 'Registering...' : 'Register'}
                </Button>
            </Form>
        </Card>
    );
};


type PageMode = 'loading' | 'initial' | 'joining-specific' | 'on-table' | 'error' | 'auth' | 'view-only';
type AuthMode = 'login' | 'register';


export const HomePage = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    const [table, setTable] = useState<PokerTable | null>(null);
    const [developer, setDeveloper] = useState<Developer | null>(null);
    const [developersList, setDevelopersList] = useState<Developer[]>([]);
    const [userStories, setUserStories] = useState<UserStory[]>([]);
    const [editingStoryId, setEditingStoryId] = useState<number | null>(null);
    const [activeTables, setActiveTables] = useState<PokerTable[]>([]);
    const [closedTables, setClosedTables] = useState<PokerTable[]>([]);

    const [mode, setMode] = useState<PageMode>('loading');
    const [authMode, setAuthMode] = useState<AuthMode>('login');
    const [error, setError] = useState<string | null>(null);
    const [authError, setAuthError] = useState<string | null>(null);
    const [tableDetailsForJoin, setTableDetailsForJoin] = useState<any>(null);
    const [isProcessing, setIsProcessing] = useState(false);
    const [isUserStoryProcessing, setIsUserStoryProcessing] = useState(false);

    const [copiedMessage, setCopiedMessage] = useState<string | null>(null);
    const [isCopying, setIsCopying] = useState(false);

    const [currentUserHasVoted, setCurrentUserHasVoted] = useState(false);

    const autoJoinTable = useCallback(async (tableId: number) => {
        if (!developer) {
            console.error("Developer not logged in for auto-join. This should not happen.");
            setError("Developer not logged in for auto-join. Please log in.");
            setMode('error');
            return;
        }

        setIsProcessing(true);
        setError(null);
        try {
            const response: JoinResponse = await joinTable(tableId);
            setDeveloper(response.developer);
            setTable(response.table);
            setMode('on-table');
            localStorage.setItem('developerId', response.developer.id.toString());

            const devs = await getDevelopers(response.table.id);
            setDevelopersList(devs);
            const stories = await getUserStoriesByTableId(response.table.id);
            setUserStories(stories);

            const tableIdFromUrl = searchParams.get('tableId');
            if (!tableIdFromUrl || Number(tableIdFromUrl) !== response.table.id) {
                navigate(`/join-session?tableId=${response.table.id}`, { replace: true });
            }

        } catch (err: any) {
            console.error("Error during auto-join:", err);
            setError(`Failed to join session automatically: ${err.response?.data?.message || err.message || 'Unknown error'}`);
            setMode('error');
        } finally {
            setIsProcessing(false);
        }
    }, [developer, navigate, searchParams]);


    const autoLogin = useCallback(async () => {
        const storedToken = localStorage.getItem('jwtToken');
        const storedDeveloperId = localStorage.getItem('developerId');

        if (storedToken && storedDeveloperId) {
            setMode('loading');
            try {
                const dev: Developer = await getDeveloperById(Number(storedDeveloperId));
                setDeveloper(dev);
                setMode('initial');
            } catch (err) {
                console.error("Auto-login failed:", err);
                logoutDeveloper();
                localStorage.removeItem('developerId');
                setDeveloper(null);
                setMode('auth');
                setAuthMode('login');
                setAuthError("Twoja sesja wygasła lub jest nieprawidłowa. Zaloguj się ponownie.");
            } finally {
                setIsProcessing(false);
            }
        } else {
            setMode('auth');
            setAuthMode('login');
            setIsProcessing(false);
        }
    }, []);


    useEffect(() => {
        autoLogin();
    }, [autoLogin]);

    useEffect(() => {
        const tableIdFromUrl = searchParams.get('tableId');

        if (tableIdFromUrl && developer && mode !== 'on-table' && mode !== 'view-only') {
            autoJoinTable(Number(tableIdFromUrl));
        }
        else if (tableIdFromUrl && !developer && mode !== 'auth') {
            navigate('/join-session', { replace: true });
            setMode('auth');
            setAuthMode('login');
            setAuthError("Aby dołączyć do sesji, musisz się zalogować lub zarejestrować.");
            localStorage.setItem('pendingJoinTableId', tableIdFromUrl);
        }
        else if (!tableIdFromUrl && developer && mode !== 'initial' && mode !== 'on-table' && mode !== 'view-only') {
            setMode('initial');
        }
    }, [searchParams, developer, mode, autoJoinTable, navigate]);


    const fetchDevelopersList = useCallback(async () => {
        if (table?.id && developer?.id) {
            try {
                const devs: Developer[] = await getDevelopers(table.id);
                if (JSON.stringify(devs) !== JSON.stringify(developersList)) {
                    setDevelopersList(devs);

                    const currentDev = devs.find(d => d.id === developer.id);
                    if (currentDev && currentDev.vote === null) {
                        setCurrentUserHasVoted(false);
                    } else if (currentDev && currentDev.vote !== null) {
                        setCurrentUserHasVoted(true);
                    }
                }
            } catch (err) {
                console.error("Failed to fetch developers:", err);
            }
        } else {
            setDevelopersList([]);
            setCurrentUserHasVoted(false);
        }
    }, [table?.id, developer?.id, developersList]);

    useEffect(() => {
        let developerPollingInterval: NodeJS.Timeout | undefined;

        if (developer?.id && table?.id && mode === 'on-table') {
            fetchDevelopersList();

            developerPollingInterval = setInterval(() => {
                fetchDevelopersList();
            }, 3000);

        } else {
            setDevelopersList([]);
            setCurrentUserHasVoted(false);
        }

        return () => {
            if (developerPollingInterval) {
                clearInterval(developerPollingInterval);
            }
        };
    }, [developer, table, mode, fetchDevelopersList]);


    const fetchUserStoriesForTable = useCallback(async () => {
        if (table?.id) {
            setIsUserStoryProcessing(true);
            try {
                const stories: UserStory[] = await getUserStoriesByTableId(table.id);
                if (JSON.stringify(stories) !== JSON.stringify(userStories)) {
                    setUserStories(stories);
                }
                setError(null);
            } catch (err: any) {
                console.error("Failed to fetch user stories:", err);
                setError(`Failed to load user stories: ${err.response?.data?.message || err.message || 'Unknown error'}`);
            } finally {
                setIsUserStoryProcessing(false);
            }
        } else {
            setUserStories([]);
        }
    }, [table?.id, userStories]);

    useEffect(() => {
        let storyPollingInterval: NodeJS.Timeout | undefined;

        if (table?.id && (mode === 'on-table' || mode === 'view-only')) {
            fetchUserStoriesForTable();
            storyPollingInterval = setInterval(() => {
                fetchUserStoriesForTable();
            }, 5000);
        } else {
            setUserStories([]);
        }

        return () => {
            if (storyPollingInterval) {
                clearInterval(storyPollingInterval);
            }
        };
    }, [table?.id, mode, fetchUserStoriesForTable]);


    useEffect(() => {
        const loadTables = async () => {
            if (mode === 'initial' && developer) {
                setIsProcessing(true);
                setError(null);
                try {
                    const active: PokerTable[] = await getAllActiveTables();
                    setActiveTables(active);
                    const closed: PokerTable[] = await getMyClosedTables(developer.id);
                    setClosedTables(closed);

                    const pendingJoinTableId = localStorage.getItem('pendingJoinTableId');
                    if (pendingJoinTableId) {
                        localStorage.removeItem('pendingJoinTableId');
                        await autoJoinTable(Number(pendingJoinTableId));
                    }

                } catch (err: any) {
                    console.error("Failed to fetch tables:", err);
                    setError(`Failed to load sessions: ${err.response?.data?.message || err.message || 'Unknown error'}`);
                } finally {
                    setIsProcessing(false);
                }
            }
        };

        loadTables();
    }, [mode, developer, autoJoinTable]);


    const handleJoin = async (tableIdToJoin: number) => {
        if (!developer) {
            console.error("Developer not logged in. Cannot join table.");
            setError("Musisz być zalogowany, aby dołączyć do sesji.");
            setMode('auth');
            setAuthMode('login');
            localStorage.setItem('pendingJoinTableId', tableIdToJoin.toString());
            return;
        }
        await autoJoinTable(tableIdToJoin);
    };


    const handleCreateTable = async () => {
        setIsProcessing(true);
        setError(null);
        try {
            const newTable: PokerTable = await createTable();
            if (developer) {
                await autoJoinTable(newTable.id);
            } else {
                navigate('/join-session', { replace: true });
                setMode('auth');
                setAuthMode('login');
                setAuthError("Aby utworzyć i dołączyć do sesji, musisz być zalogowany.");
            }
        } catch (err: any) {
            console.error("Error creating table:", err);
            setError(`Failed to create session: ${err.response?.data?.message || err.message || 'Unknown error'}`);
            setIsProcessing(false);
        }
    }

    const handleViewPastSession = async (tableId: number) => {
        setIsProcessing(true);
        setError(null);
        try {
            const selectedTable: PokerTable = await getTableById(tableId);
            setTable(selectedTable);
            const stories: UserStory[] = await getUserStoriesByTableId(tableId);
            setUserStories(stories);
            setDevelopersList([]);
            setMode('view-only');
        } catch (err: any) {
            console.error("Failed to view past session:", err);
            setError(`Failed to load past session: ${err.response?.data?.message || err.message || 'Unknown error'}`);
            setMode('error');
        } finally {
            setIsProcessing(false);
        }
    };

    const handleCloseTable = async () => {
        if (!table?.id) return;
        setIsProcessing(true);
        setError(null);
        try {
            await closePokerTable(table.id);
            setTable(null);
            setDevelopersList([]);
            setUserStories([]);
            setEditingStoryId(null);
            setCurrentUserHasVoted(false);
            setMode('initial');
            if (developer) {
                const active = await getAllActiveTables();
                setActiveTables(active);
                const closed = await getMyClosedTables(developer.id);
                setClosedTables(closed);
            }
        } catch (err: any) {
            console.error("Error closing table:", err);
            setError(`Failed to close session: ${err.response?.data?.message || err.message || 'Unknown error'}`);
        } finally {
            setIsProcessing(false);
        }
    };

    const handleBackToSessionList = () => {
        setTable(null);
        setDevelopersList([]);
        setUserStories([]);
        setEditingStoryId(null);
        setCurrentUserHasVoted(false);
        setMode('initial');
        navigate('/', { replace: true });
    };


    const handleVoteSuccess = async () => {
        fetchDevelopersList();
    };

    const handleCopyInviteLink = async () => {
        if (!table?.id) return;

        const inviteLink = `${window.location.origin}/join-session?tableId=${table.id}`;

        setIsCopying(true);
        setCopiedMessage(null);

        try {
            const textArea = document.createElement('textarea');
            textArea.value = inviteLink;
            document.body.appendChild(textArea);
            textArea.select();
            document.execCommand('copy');
            textArea.remove();

            setCopiedMessage('Link skopiowany do schowka!');
        } catch (err) {
            console.error("Failed to copy invite link:", err);
            setCopiedMessage('Nie udało się skopiować linku.');
        } finally {
            setIsCopying(false);
            setTimeout(() => {
                setCopiedMessage(null);
            }, 3000);
        }
    };


    const handleAddStory = async (storyData: { title: string; description?: string; estimatedPoints?: number | null }) => {
        if (!table?.id) return;
        setIsUserStoryProcessing(true);
        setError(null);
        try {
            await createUserStory(table.id, storyData);
            await fetchUserStoriesForTable();
        } catch (err: any) {
            console.error("Error creating user story:", err);
            setError(`Failed to create user story: ${err.response?.data?.message || err.message || 'Unknown error'}`);
        } finally {
            setIsUserStoryProcessing(false);
        }
    };

    const handleEditStoryClick = (storyId: number) => {
        setEditingStoryId(storyId);
        setError(null);
    };

    const handleUpdateStory = async (storyId: number, storyData: { title: string; description?: string; estimatedPoints?: number | null }) => {
        if (!table?.id) return;
        setIsUserStoryProcessing(true);
        setError(null);
        try {
            await updateUserStory(storyId, storyData);
            setEditingStoryId(null);
            await fetchUserStoriesForTable();
        } catch (err: any) {
            console.error("Error updating user story:", err);
            setError(`Failed to update user story: ${err.response?.data?.message || err.message || 'Unknown error'}`);
        } finally {
            setIsUserStoryProcessing(false);
        }
    };

    const handleCancelEditStory = () => {
        setEditingStoryId(null);
        setError(null);
    };


    const handleDeleteStory = async (storyId: number) => {
        if (!table?.id) return;
        setIsUserStoryProcessing(true);
        setError(null);
        try {
            await deleteUserStory(storyId);
            await fetchUserStoriesForTable();
        } catch (err: any) {
            console.error("Error deleting user story:", err);
            setError(`Failed to delete user story: ${err.response?.data?.message || err.message || 'Unknown error'}`);
        } finally {
            setIsUserStoryProcessing(false);
        }
    };

    const handleExportStories = async () => {
        if (table?.id) {
            try {
                const blob = await exportUserStoriesToCsv(table.id);
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `user_stories_table_${table.id}.csv`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                window.URL.revokeObjectURL(url);
            } catch (err: any) {
                console.error("Failed to export user stories:", err);
                setError(`Failed to export user stories: ${err.response?.data?.message || err.message || 'Unknown error'}`);
            }
        }
    };

    const handleRegister = async (name: string, email: string, password: string) => {
        setIsProcessing(true);
        setAuthError(null);
        try {
            await registerDeveloper(name, email, password);
            setAuthMode('login');
            setAuthError("Rejestracja zakończona sukcesem! Zaloguj się.");
        } catch (err: any) {
            console.error("Registration failed:", err);
            setAuthError(err.response?.data?.message || err.message || 'Rejestracja nieudana.');
        } finally {
            setIsProcessing(false);
        }
    };

    const handleLogin = async (email: string, password: string) => {
        setIsProcessing(true);
        setAuthError(null);
        try {
            const { developer: loggedInDeveloper } = await loginDeveloper(email, password);
            setDeveloper(loggedInDeveloper);
            setMode('initial');
            setAuthError("Logowanie zakończone sukcesem!");
        } catch (err: any) {
            console.error("Login failed:", err);
            setAuthError(err.response?.data?.message || err.message || 'Logowanie nieudane.');
        } finally {
            setIsProcessing(false);
        }
    };

    const handleLogout = async () => {
        setIsProcessing(true);
        setError(null);
        try {
            await logoutDeveloper();
            setDeveloper(null);
            setTable(null);
            setDevelopersList([]);
            setUserStories([]);
            setEditingStoryId(null);
            setCurrentUserHasVoted(false);
            setMode('auth');
            setAuthMode('login');
            localStorage.removeItem('developerId');
            localStorage.removeItem('pendingJoinTableId');
        } catch (err: any) {
            console.error("Logout failed:", err);
            setError(`Nie udało się wylogować: ${err.response?.data?.message || err.message || 'Nieznany błąd'}`);
        } finally {
            setIsProcessing(false);
        }
    };

    const handleResetAllVotes = async () => {
        if (!table?.id) return;
        setIsProcessing(true);
        setError(null);
        try {
            await resetAllVotes(table.id);
            fetchDevelopersList();
        } catch (err: any) {
            console.error("Error resetting all votes:", err);
            setError(`Nie udało się zresetować głosów: ${err.response?.data?.message || err.message || 'Nieznany błąd'}`);
        } finally {
            setIsProcessing(false);
        }
    };


    if (mode === 'loading') {
        return <div className="container mt-5">Ładowanie sesji planowania...</div>;
    }

    if (mode === 'error') {
        return <div className="container mt-5">
            <Alert variant="danger">Błąd: {error || "Wystąpił nieoczekiwany błąd."}</Alert>
            {searchParams.get('tableId') && mode === 'error' && (
                <Button variant="secondary" onClick={() => navigate('/')}>
                    Wróć do strony głównej / Utwórz nową sesję
                </Button>
            )}
            <Button variant="info" onClick={autoLogin} className="ms-2">Spróbuj ponownie automatycznego logowania</Button>
        </div>;
    }

    const isReadyOnTable = mode === 'on-table' && developer && table;
    const isViewOnlyMode = mode === 'view-only' && table;

    return (
        <div className="container mt-5">
            <div className="d-flex justify-content-between align-items-center mb-4">
                {table && <h1 className="mb-0">Planning Poker: {table.name}</h1>}
                {mode === 'joining-specific' && tableDetailsForJoin && <h1 className="mb-0">Dołącz do Planning Poker: {tableDetailsForJoin.name}</h1>}
                {mode === 'initial' && <h1 className="mb-0">Planning Poker</h1>}
                {mode === 'auth' && <h1 className="mb-0">Witaj w Planning Poker</h1>}
                {mode === 'view-only' && table && <h1 className="mb-0">Historia sesji: {table.name}</h1>}


                <div>
                    {developer ? (
                        <>
                            <span className="me-2">Zalogowano jako: {developer.name}</span>
                            <Button variant="outline-danger" size="sm" onClick={handleLogout} disabled={isProcessing}>
                                Wyloguj
                            </Button>
                        </>
                    ) : (
                        <>
                            <Button variant="outline-primary" size="sm" onClick={() => { setMode('auth'); setAuthMode('login'); setError(null); setAuthError(null); }} className="me-2">
                                Zaloguj
                            </Button>
                            <Button variant="outline-success" size="sm" onClick={() => { setMode('auth'); setAuthMode('register'); setError(null); setAuthError(null); }}>
                                Zarejestruj
                            </Button>
                        </>
                    )}
                </div>
            </div>

            {error && mode !== 'loading' && mode !== 'error' && (
                <Alert variant="danger" className="mt-4">{error}</Alert>
            )}

            {mode === 'auth' && (
                <div className="d-flex justify-content-center mt-5">
                    {authMode === 'login' ? (
                        <LoginForm onSubmit={handleLogin} isSubmitting={isProcessing} error={authError} />
                    ) : (
                        <RegisterForm onSubmit={handleRegister} isSubmitting={isProcessing} error={authError} />
                    )}
                </div>
            )}

            {mode === 'initial' && developer && (
                <>
                    <p>Witaj! Utwórz nową sesję planowania lub dołącz do niej za pomocą linku z zaproszeniem.</p>
                    <Button
                        variant="primary"
                        onClick={handleCreateTable}
                        className="mt-3"
                        disabled={isProcessing}
                    >
                        {isProcessing ? 'Tworzenie...' : 'Utwórz nową sesję'}
                    </Button>

                    <h4 className="mt-5 mb-3">Dostępne aktywne sesje:</h4>
                    {isProcessing ? (
                        <p>Ładowanie sesji...</p>
                    ) : activeTables.length === 0 ? (
                        <p>Brak aktywnych sesji. Utwórz nową!</p>
                    ) : (
                        <ListGroup className="mt-3">
                            {activeTables.map(activeTable => (
                                <ListGroup.Item key={activeTable.id} className="d-flex justify-content-between align-items-center">
                                    <span>{activeTable.name} (ID: {activeTable.id})</span>
                                    <Button
                                        variant="outline-success"
                                        size="sm"
                                        onClick={() => handleJoin(activeTable.id)}
                                        disabled={isProcessing}
                                    >
                                        Dołącz
                                    </Button>
                                </ListGroup.Item>
                            ))}
                        </ListGroup>
                    )}

                    <h4 className="mt-5 mb-3">Twoje poprzednie sesje (tylko do odczytu):</h4>
                    {isProcessing ? (
                        <p>Ładowanie poprzednich sesji...</p>
                    ) : closedTables.length === 0 ? (
                        <p>Brak poprzednich sesji, w których brałeś udział.</p>
                    ) : (
                        <ListGroup className="mt-3">
                            {closedTables.map(closedTable => (
                                <ListGroup.Item key={closedTable.id} className="d-flex justify-content-between align-items-center">
                                    <span>{closedTable.name} (ID: {closedTable.id})</span>
                                    <Button
                                        variant="outline-info"
                                        size="sm"
                                        onClick={() => handleViewPastSession(closedTable.id)}
                                        disabled={isProcessing}
                                    >
                                        Podgląd
                                    </Button>
                                </ListGroup.Item>
                            ))}
                        </ListGroup>
                    )}
                </>
            )}

            {mode === 'joining-specific' && !developer && tableDetailsForJoin && (
                <Alert variant="info" className="mt-4">
                    Aby dołączyć do sesji <strong>{tableDetailsForJoin.name}</strong>, musisz się zalogować lub zarejestrować.
                </Alert>
            )}


            {isReadyOnTable && (
                <>
                    <div className="d-flex justify-content-between mb-4">
                        <Button
                            variant="secondary"
                            onClick={handleBackToSessionList}
                            disabled={isProcessing}
                        >
                            Wróć do listy sesji
                        </Button>
                        <Button
                            variant="danger"
                            onClick={handleCloseTable}
                            disabled={isProcessing}
                            className="ms-2"
                        >
                            Zamknij sesję
                        </Button>
                        <Button
                            variant="warning"
                            onClick={handleResetAllVotes}
                            disabled={isProcessing}
                            className="ms-auto"
                        >
                            Resetuj głosy dla wszystkich
                        </Button>
                    </div>

                    <UserStoryList
                        userStories={userStories}
                        onEditClick={handleEditStoryClick}
                        onDeleteClick={handleDeleteStory}
                        onUpdateSubmit={handleUpdateStory}
                        onCancelEdit={handleCancelEditStory}
                        editingStoryId={editingStoryId}
                        isSubmitting={isUserStoryProcessing}
                    />

                    {editingStoryId === null && (
                        <UserStoryForm
                            onSubmit={handleAddStory}
                            isSubmitting={isUserStoryProcessing}
                        />
                    )}

                    {userStories.length > 0 && (
                        <div className="mt-4">
                            <Button
                                variant="outline-secondary"
                                onClick={handleExportStories}
                                disabled={isUserStoryProcessing}
                            >
                                Eksportuj historyjki do CSV (dla JIRA)
                            </Button>
                        </div>
                    )}


                    <div className="card mt-4 mb-4">
                        <div className="card-body">
                            <h3 className="card-title">Uczestnicy ({developersList.length})</h3>
                            <ul className="list-group">
                                {developersList.map(dev => (
                                    <li
                                        key={dev.id}
                                        className="list-group-item d-flex justify-content-between align-items-center"
                                    >
                                        <span>
                                            {dev.name}
                                            {dev.id === developer.id && " (Ty)"}
                                        </span>

                                        {dev.vote !== null ? (
                                            <span className="badge bg-primary rounded-pill">
                                                {dev.vote}
                                            </span>
                                        ) : (
                                            <span className="text-muted">
                                                Brak głosu
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
                        <div className="mt-4 card">
                            <div className="card-body">
                                <h5 className="card-title">Zaproś członków zespołu</h5>
                                <p>Udostępnij ten link:</p>
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
                                        {isCopying ? 'Kopiowanie...' : 'Kopiuj'}
                                    </Button>
                                </div>
                                {copiedMessage && (
                                    <p className={`mt-2 mb-0 text-${copiedMessage.includes('skopiowany') ? 'success' : 'danger'}`}>
                                        {copiedMessage}
                                    </p>
                                )}
                            </div>
                        </div>
                    )}
                </>
            )}

            {isViewOnlyMode && (
                <>
                    <Button
                        variant="secondary"
                        onClick={handleBackToSessionList}
                        className="mb-4"
                    >
                        Wróć do listy sesji
                    </Button>

                    <h3 className="mb-3">Historyjki użytkownika dla sesji: {table?.name} ({userStories.length})</h3>
                    {userStories.length === 0 ? (
                        <p>Brak historyjek użytkownika dla tej sesji.</p>
                    ) : (
                        <ListGroup className="mb-4">
                            {userStories.map(story => (
                                <ListGroup.Item key={story.id}>
                                    <h5>{story.title}</h5>
                                    {story.description && <p className="text-muted mb-1">{story.description}</p>}
                                    {story.estimatedPoints != null && (
                                        <span className="badge bg-primary">Szacowane punkty: {story.estimatedPoints}</span>
                                    )}
                                </ListGroup.Item>
                            ))}
                        </ListGroup>
                    )}

                    {userStories.length > 0 && (
                        <div className="mt-4">
                            <Button
                                variant="outline-secondary"
                                onClick={handleExportStories}
                                disabled={isUserStoryProcessing}
                            >
                                Eksportuj historyjki do CSV (dla JIRA)
                            </Button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
};
