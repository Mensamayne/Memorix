/**
 * Application State Management
 * Simple reactive state store
 */

export const AppState = {
    // Current page
    currentPage: 'dashboard',
    
    // Memories data
    memories: [],
    selectedMemory: null,
    
    // Filters
    filters: {
        userId: 'demo-user',
        pluginType: null,  // null = all types
        sortBy: 'createdAt',
        sortOrder: 'desc',
        searchTerm: ''
    },
    
    // Pagination
    pagination: {
        currentPage: 1,
        perPage: 20,
        total: 0,
        totalPages: 0
    },
    
    // Stats
    stats: null,
    
    // Plugins configuration
    plugins: [],
    pluginConfigs: {},
    
    // Datasources
    datasources: null,
    
    // UI State
    ui: {
        loading: false,
        modalOpen: false,
        modalType: null,  // 'create', 'edit', 'delete', 'details'
        sidebarCollapsed: false
    },
    
    // Subscribers for state changes
    subscribers: [],
    
    /**
     * Subscribe to state changes
     */
    subscribe(callback) {
        this.subscribers.push(callback);
        return () => {
            this.subscribers = this.subscribers.filter(cb => cb !== callback);
        };
    },
    
    /**
     * Notify all subscribers of state change
     */
    notify(changedKeys = []) {
        this.subscribers.forEach(callback => callback(this, changedKeys));
    },
    
    /**
     * Update state and notify subscribers
     */
    setState(updates) {
        const changedKeys = Object.keys(updates);
        Object.assign(this, updates);
        this.notify(changedKeys);
    },
    
    /**
     * Update nested state (e.g., filters, pagination)
     */
    updateNested(key, updates) {
        this[key] = { ...this[key], ...updates };
        this.notify([key]);
    },
    
    /**
     * Reset filters to default
     */
    resetFilters() {
        this.filters = {
            userId: 'demo-user',
            pluginType: null,
            sortBy: 'createdAt',
            sortOrder: 'desc',
            searchTerm: ''
        };
        this.notify(['filters']);
    }
};

