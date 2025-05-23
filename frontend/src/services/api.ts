import axios from 'axios';

const API_URL = 'http://localhost:8080';

// Updated: UserStory interface now reflects UserStoryDto from backend
interface UserStory {
    id: number;
    title: string;
    description?: string;
    estimatedPoints?: number | null;
}

// Updated: Developer interface now reflects DeveloperDto from backend
interface Developer {
    id: number;
    name: string;
    email?: string;
    vote?: number | null;
}

// Updated: PokerTable interface now reflects PokerTableDto from backend
interface PokerTable {
    id: number;
    name: string;
    createdAt: string;
    isClosed: boolean;
}

// Updated: LoginResponse interface now uses Developer
interface LoginResponse {
    jwt: string;
    developer: Developer; // Now Developer (which is DeveloperDto)
}

// Updated: JoinResponse interface now uses Developer and PokerTable
interface JoinResponse {
    developer: Developer; // Now Developer (which is DeveloperDto)
    table: PokerTable; // Now PokerTable (which is PokerTableDto)
}

// Function to get JWT token from localStorage
const getAuthToken = (): string | null => {
    return localStorage.getItem('jwtToken');
};

// Function to set JWT token in localStorage
const setAuthToken = (token: string) => {
    localStorage.setItem('jwtToken', token);
};

// Function to remove JWT token from localStorage
const removeAuthToken = () => {
    localStorage.removeItem('jwtToken');
};

// Axios instance with default headers for authenticated requests
const authAxios = axios.create({
    baseURL: API_URL,
    withCredentials: true, // Important for sessions and CSRF
});

// Interceptor to add JWT token to every request
authAxios.interceptors.request.use(
    (config) => {
        const token = getAuthToken();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// --- Authentication API Calls ---

// Updated: registerDeveloper now returns Developer (DeveloperDto from backend)
export const registerDeveloper = async (name: string, email: string, password: string): Promise<Developer> => {
    const response = await axios.post(`${API_URL}/developers/register`, { name, email, password });
    return response.data;
};

// Changed return type to LoginResponse to include Developer (DeveloperDto from backend)
export const loginDeveloper = async (email: string, password: string): Promise<LoginResponse> => {
    const response = await axios.post(`${API_URL}/developers/login`, { email, password });
    const { jwt, developer } = response.data;
    setAuthToken(jwt);
    localStorage.setItem('developerId', developer.id.toString());
    return response.data;
};

export const logoutDeveloper = async (): Promise<void> => {
    await authAxios.post('/developers/logout');
    removeAuthToken();
    localStorage.removeItem('developerId');
};

// --- Developer API Calls ---

// Updated: joinTable now returns JoinResponse
export const joinTable = async (tableId: number): Promise<JoinResponse> => {
    const response = await authAxios.post(`/developers/join?tableId=${tableId}`);
    return response.data;
};

export const vote = async (developerId: number, tableId: number, value: number) => {
    await authAxios.patch(`/developers/${developerId}/vote?tableId=${tableId}&vote=${value}`);
};

// Updated: getDevelopers now returns Developer[] (List<DeveloperDto> from backend)
export const getDevelopers = async (tableId: number): Promise<Developer[]> => {
    const response = await authAxios.get(`/developers/poker-table/${tableId}`);
    return response.data;
};

// Updated: getDeveloperById now returns Developer (DeveloperDto from backend)
export const getDeveloperById = async (developerId: number): Promise<Developer> => {
    const response = await authAxios.get(`/developers/${developerId}`);
    return response.data;
};

export const hasDeveloperVoted = async (developerId: number): Promise<boolean> => {
    const response = await authAxios.get(`/developers/${developerId}/has-voted`);
    return response.data;
};


// --- Poker Table API Calls ---

// Updated: getActiveTable now returns PokerTable (PokerTableDto from backend)
export const getActiveTable = async (): Promise<PokerTable> => {
    const response = await authAxios.get(`/tables/active`);
    return response.data;
};

// Updated: getTableById now returns PokerTable (PokerTableDto from backend)
export const getTableById = async (tableId: number): Promise<PokerTable> => {
    const response = await authAxios.get(`/tables/${tableId}`);
    return response.data;
}

// Updated: createTable now returns PokerTable (PokerTableDto from backend)
export const createTable = async (): Promise<PokerTable> => {
    const response = await authAxios.post(`/tables`);
    return response.data;
}

export const closePokerTable = async (tableId: number): Promise<void> => {
    await authAxios.patch(`/tables/${tableId}/close`);
};

export const exportUserStoriesToCsv = async (tableId: number): Promise<Blob> => {
    const response = await authAxios.get(`/tables/${tableId}/export-stories`, {
        responseType: 'blob', // Important for file downloads
    });
    return response.data;
};

// Updated: getAllActiveTables now returns PokerTable[] (List<PokerTableDto> from backend)
export const getAllActiveTables = async (): Promise<PokerTable[]> => {
    const response = await authAxios.get(`/tables/all-active`);
    return response.data;
};

// Updated: getMyClosedTables now returns PokerTable[] (List<PokerTableDto> from backend)
export const getMyClosedTables = async (developerId: number): Promise<PokerTable[]> => {
    const response = await authAxios.get(`/tables/my-closed`);
    return response.data;
};


// --- User Story API Calls ---

// Updated: getUserStoriesByTableId now returns UserStory[] (Set<UserStoryDto> from backend)
export const getUserStoriesByTableId = async (tableId: number): Promise<UserStory[]> => {
    const response = await authAxios.get(`/user-stories/table/${tableId}`);
    return response.data;
}

// Updated: createUserStory now returns UserStory (UserStoryDto from backend)
export const createUserStory = async (tableId: number, storyData: { title: string; description?: string; estimatedPoints?: number | null }): Promise<UserStory> => {
    const response = await authAxios.post(`/user-stories?pokerTableId=${tableId}`, storyData);
    return response.data;
}

// Updated: updateUserStory now returns UserStory (UserStoryDto from backend)
export const updateUserStory = async (storyId: number, storyData: { title?: string; description?: string; estimatedPoints?: number | null }): Promise<UserStory> => {
    const response = await authAxios.put(`${API_URL}/user-stories/${storyId}`, storyData);
    return response.data;
}

export const deleteUserStory = async (storyId: number): Promise<void> => {
    await authAxios.delete(`/user-stories/${storyId}`);
}

export type { UserStory, Developer, PokerTable, JoinResponse }; // Export the types for use in components

// ... (istniejące importy i inne funkcje)

export const resetAllVotes = async (tableId: number) => {
    try {
        const response = await authAxios.post(`/tables/${tableId}/reset-all-votes`);
        return response.data;
    } catch (error) {
        console.error('Error resetting all votes:', error);
        throw error;
    }
};
