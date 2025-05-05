import axios from 'axios';

const API_URL = 'http://localhost:8080';

interface UserStory {
    id: number;
    title: string;
    description?: string;
    estimatedPoints?: number | null;
}

export const joinTable = async (name: string, tableId: number) => {
    const response = await axios.post(`${API_URL}/developers/join?name=${name}&tableId=${tableId}`);
    return response.data;
};

export const vote = async (developerId: number, tableId: number, value: number) => {
    await axios.patch(`${API_URL}/developers/${developerId}/vote?tableId=${tableId}&vote=${value}`);
};

export const getDevelopers = async (tableId: number) => {
    const response = await axios.get(`${API_URL}/developers/poker-table/${tableId}`, {
        withCredentials: true
    });
    return response.data;
};

export const getActiveTable = async () => {
    const response = await axios.get(`${API_URL}/tables/active`);
    return response.data;
};

export const getTableById = async (tableId: number) => {
    const response = await axios.get(`${API_URL}/tables/${tableId}`);
    return response.data;
}

export const createTable = async () => {
    const response = await axios.post(`${API_URL}/tables`);
    return response.data;
}

// --- User Story API Calls ---

export const getUserStoriesByTableId = async (tableId: number): Promise<UserStory[]> => {
    const response = await axios.get(`${API_URL}/user-stories/table/${tableId}`);
    return response.data;
}

export const createUserStory = async (tableId: number, storyData: { title: string; description?: string; estimatedPoints?: number | null }): Promise<UserStory> => {
    const response = await axios.post(`${API_URL}/user-stories?pokerTableId=${tableId}`, storyData);
    return response.data;
}

export const updateUserStory = async (storyId: number, storyData: { title?: string; description?: string; estimatedPoints?: number | null }): Promise<UserStory> => {
    const response = await axios.put(`${API_URL}/user-stories/${storyId}`, storyData);
    return response.data;
}

export const deleteUserStory = async (storyId: number): Promise<void> => {
    await axios.delete(`${API_URL}/user-stories/${storyId}`);
}

export type { UserStory }; // Export the type for use in components