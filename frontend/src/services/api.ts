import axios from 'axios';

const API_URL = 'http://localhost:8080';

export const joinTable = async (name: string, tableId: number) => { // Added tableId parameter
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