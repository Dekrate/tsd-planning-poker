import axios from 'axios';

const API_URL = 'http://localhost:8080';

interface UserStory {
    id: number;
    title: string;
    description?: string;
    estimatedPoints?: number | null;
}

interface Developer {
    id: number;
    name: string;
    email?: string;
    vote?: number | null;
}

interface PokerTable {
    id: number;
    name: string;
    createdAt: string;
    isClosed: boolean;
    finalVotes?: { [developerId: number]: number };
}

interface LoginResponse {
    jwt: string;
    developer: Developer;
}

interface JoinResponse {
    developer: Developer;
    table: PokerTable;
}

const getAuthToken = (): string | null => {
    return localStorage.getItem('jwtToken');
};

const setAuthToken = (token: string) => {
    localStorage.setItem('jwtToken', token);
};

const removeAuthToken = () => {
    localStorage.removeItem('jwtToken');
};

const authAxios = axios.create({
    baseURL: API_URL,
    withCredentials: true,
});

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

export const registerDeveloper = async (name: string, email: string, password: string): Promise<Developer> => {
    const response = await axios.post(`${API_URL}/developers/register`, { name, email, password });
    return response.data;
};

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

export const joinTable = async (tableId: number): Promise<JoinResponse> => {
    const response = await authAxios.post(`/developers/join?tableId=${tableId}`);
    return response.data;
};

export const vote = async (developerId: number, tableId: number, value: number) => {
    await authAxios.patch(`/developers/${developerId}/vote?tableId=${tableId}&vote=${value}`);
};

export const getDevelopers = async (tableId: number): Promise<Developer[]> => {
    const response = await authAxios.get(`/developers/poker-table/${tableId}`);
    return response.data;
};

export const getDeveloperById = async (developerId: number): Promise<Developer> => {
    const response = await authAxios.get(`/developers/${developerId}`);
    return response.data;
};

export const hasDeveloperVoted = async (developerId: number): Promise<boolean> => {
    const response = await authAxios.get(`/developers/${developerId}/has-voted`);
    return response.data;
};

export const getActiveTable = async (): Promise<PokerTable> => {
    const response = await authAxios.get(`/tables/active`);
    return response.data;
};

export const getTableById = async (tableId: number): Promise<PokerTable> => {
    const response = await authAxios.get(`/tables/${tableId}`);
    return response.data;
}

export const createTable = async (): Promise<PokerTable> => {
    const response = await authAxios.post(`/tables`);
    return response.data;
}

export const closePokerTable = async (tableId: number): Promise<void> => {
    await authAxios.patch(`/tables/${tableId}/close`);
};

export const exportUserStoriesToCsv = async (tableId: number): Promise<Blob> => {
    const response = await authAxios.get(`/tables/${tableId}/export-stories`, {
        responseType: 'blob',
    });
    return response.data;
};

export const getAllActiveTables = async (): Promise<PokerTable[]> => {
    const response = await authAxios.get(`/tables/all-active`);
    return response.data;
};

export const getMyClosedTables = async (developerId: number): Promise<PokerTable[]> => {
    const response = await authAxios.get(`/tables/my-closed`);
    return response.data;
};

export const resetAllVotes = async (tableId: number) => {
    try {
        const response = await authAxios.post(`/tables/${tableId}/reset-all-votes`);
        return response.data;
    } catch (error) {
        console.error('Error resetting all votes:', error);
        throw error;
    }
};

export const getUserStoriesByTableId = async (tableId: number): Promise<UserStory[]> => {
    const response = await authAxios.get(`/user-stories/table/${tableId}`);
    return response.data;
}

export const createUserStory = async (tableId: number, storyData: { title: string; description?: string; estimatedPoints?: number | null }): Promise<UserStory> => {
    const response = await authAxios.post(`/user-stories?pokerTableId=${tableId}`, storyData);
    return response.data;
}

export const updateUserStory = async (storyId: number, storyData: { title?: string; description?: string; estimatedPoints?: number | null }): Promise<UserStory> => {
    const response = await authAxios.put(`${API_URL}/user-stories/${storyId}`, storyData);
    return response.data;
}

export const deleteUserStory = async (storyId: number): Promise<void> => {
    await authAxios.delete(`/user-stories/${storyId}`);
}

export type { UserStory, Developer, PokerTable, JoinResponse };
