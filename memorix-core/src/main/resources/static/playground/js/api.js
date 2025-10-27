/**
 * Memorix API Client
 * Centralized API communication layer
 */

const API_BASE = 'http://localhost:8080/api/memorix';

export const API = {
    // ============================================
    // MEMORIES CRUD
    // ============================================
    
    /**
     * Get all memories with pagination and filters
     */
    async getMemories(params = {}) {
        const { userId, pluginType, page = 1, perPage = 20, sortBy = 'createdAt', sortOrder = 'desc' } = params;
        
        // Note: This is a workaround since we don't have a GET /memories endpoint
        // We'll use search with a broad query to get memories
        const searchRequest = {
            userId: userId || 'demo-user',
            query: 'user memory preference conversation documentation', // Broad query to match most memories
            pluginType: pluginType || 'USER_PREFERENCE',
            maxCount: 100, // Get more results to enable local pagination
            minSimilarity: 0.0, // Accept all similarities
            strategy: 'ALL'
        };
        
        const response = await fetch(`${API_BASE}/memories/search`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(searchRequest)
        });
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        const result = await response.json();
        
        // Sort memories locally
        if (result.memories) {
            result.memories.sort((a, b) => {
                const aVal = a[sortBy];
                const bVal = b[sortBy];
                const comparison = aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
                return sortOrder === 'asc' ? comparison : -comparison;
            });
        }
        
        return result;
    },
    
    /**
     * Create a new memory
     */
    async createMemory(data) {
        const response = await fetch(`${API_BASE}/memories`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        if (response.status === 409) {
            throw { type: 'DUPLICATE', message: result.message, data: result };
        }
        
        if (!response.ok) throw new Error(result.message || 'Failed to create memory');
        
        return result;
    },
    
    /**
     * Update existing memory
     */
    async updateMemory(memoryId, data) {
        const response = await fetch(`${API_BASE}/memories/${memoryId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        
        const result = await response.json();
        
        if (response.status === 403) {
            throw { type: 'IMMUTABLE', message: result.message, data: result };
        }
        
        if (!response.ok) throw new Error(result.message || 'Failed to update memory');
        
        return result;
    },
    
    /**
     * Delete memory by user ID (all memories for user)
     */
    async deleteMemories(userId) {
        const response = await fetch(`${API_BASE}/memories/${userId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        return await response.json();
    },
    
    // ============================================
    // SEARCH
    // ============================================
    
    /**
     * Semantic search for memories
     */
    async searchMemories(searchRequest) {
        const response = await fetch(`${API_BASE}/memories/search`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(searchRequest)
        });
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        return await response.json();
    },
    
    // ============================================
    // LIFECYCLE
    // ============================================
    
    /**
     * Apply decay to memories
     */
    async applyDecay(request) {
        const response = await fetch(`${API_BASE}/lifecycle/decay`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(request)
        });
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        return await response.json();
    },
    
    // ============================================
    // STATS
    // ============================================
    
    /**
     * Get statistics for user
     */
    async getStats(userId) {
        const response = await fetch(`${API_BASE}/stats/${userId}`);
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        return await response.json();
    },
    
    // ============================================
    // PLUGINS
    // ============================================
    
    /**
     * Get all plugin types
     */
    async getPlugins() {
        const response = await fetch(`${API_BASE}/plugins`);
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        return await response.json();
    },
    
    /**
     * Get plugin configuration
     */
    async getPluginConfig(pluginType) {
        const response = await fetch(`${API_BASE}/plugins/${pluginType}`);
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        return await response.json();
    },
    
    // ============================================
    // DATASOURCES
    // ============================================
    
    /**
     * Get datasource information
     */
    async getDatasources() {
        const response = await fetch(`${API_BASE}/datasources`);
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        return await response.json();
    },
    
    /**
     * Get datasource statistics
     */
    async getDatasourceStats() {
        const response = await fetch(`${API_BASE}/datasources/stats`);
        
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        
        return await response.json();
    }
};

