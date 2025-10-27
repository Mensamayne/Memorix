/**
 * Memories Page
 * Main memory explorer with list, filters, sorting, pagination
 */

import { API } from '../api.js';
import { AppState } from '../state.js';
import { toast } from '../components/toast.js';
import { Modal, confirmModal } from '../components/modal.js';
import { createMemoryCard } from '../components/memory-card.js';
import { createPagination } from '../components/pagination.js';

let currentFilters = {
    userId: 'demo-user',
    pluginType: null,
    sortBy: 'createdAt',
    sortOrder: 'desc',
    searchTerm: ''
};

let currentPage = 1;
let itemsPerPage = 20;

export async function renderMemories(container) {
    container.innerHTML = `
        <div class="memories-page">
            <!-- Page Header -->
            <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: var(--spacing-xl);">
                <div>
                    <h1 style="margin-bottom: var(--spacing-sm);">🗂️ Memory Explorer</h1>
                    <p style="color: var(--text-secondary);">Browse, search, and manage memories</p>
                </div>
                <button class="btn btn-primary" id="create-memory-btn">
                    ➕ New Memory
                </button>
            </div>
            
            <!-- Filters Card -->
            <div class="card" style="margin-bottom: var(--spacing-xl);">
                <div class="card-body">
                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: var(--spacing-md); margin-bottom: var(--spacing-md);">
                        <div class="form-group" style="margin-bottom: 0;">
                            <label class="form-label">User ID</label>
                            <input type="text" id="filter-userId" class="form-input" value="${currentFilters.userId}">
                        </div>
                        
                        <div class="form-group" style="margin-bottom: 0;">
                            <label class="form-label">Plugin Type</label>
                            <select id="filter-pluginType" class="form-select">
                                <option value="">All Types</option>
                            </select>
                        </div>
                        
                        <div class="form-group" style="margin-bottom: 0;">
                            <label class="form-label">Sort By</label>
                            <select id="filter-sortBy" class="form-select">
                                <option value="createdAt">Created Date</option>
                                <option value="decay">Decay</option>
                                <option value="importance">Importance</option>
                                <option value="tokenCount">Token Count</option>
                            </select>
                        </div>
                        
                        <div class="form-group" style="margin-bottom: 0;">
                            <label class="form-label">Order</label>
                            <select id="filter-sortOrder" class="form-select">
                                <option value="desc">Descending</option>
                                <option value="asc">Ascending</option>
                            </select>
                        </div>
                    </div>
                    
                    <div style="display: flex; gap: var(--spacing-sm);">
                        <button class="btn btn-primary" id="apply-filters">
                            🔍 Apply Filters
                        </button>
                        <button class="btn btn-secondary" id="reset-filters">
                            ↺ Reset
                        </button>
                        <button class="btn btn-danger" id="clear-all-btn" style="margin-left: auto;">
                            🗑️ Clear All Memories
                        </button>
                    </div>
                </div>
            </div>
            
            <!-- Memory List -->
            <div id="memory-list-container">
                <div style="text-align: center; padding: var(--spacing-2xl);">
                    <div class="spinner" style="width: 40px; height: 40px; border-width: 4px;"></div>
                    <p style="margin-top: var(--spacing-md); color: var(--text-secondary);">Loading memories...</p>
                </div>
            </div>
            
            <!-- Pagination -->
            <div id="pagination-container"></div>
        </div>
    `;
    
    // Populate plugin types
    const pluginSelect = container.querySelector('#filter-pluginType');
    AppState.plugins.forEach(plugin => {
        const option = document.createElement('option');
        option.value = plugin;
        option.textContent = plugin;
        if (currentFilters.pluginType === plugin) {
            option.selected = true;
        }
        pluginSelect.appendChild(option);
    });
    
    // Set filter values
    container.querySelector('#filter-sortBy').value = currentFilters.sortBy;
    container.querySelector('#filter-sortOrder').value = currentFilters.sortOrder;
    
    // Event listeners
    setupEventListeners(container);
    
    // Load memories
    await loadMemories(container);
}

function setupEventListeners(container) {
    // Create memory button
    const createBtn = container.querySelector('#create-memory-btn');
    createBtn.addEventListener('click', () => {
        // Navigate to operations page
        const operationsLink = document.querySelector('.nav-link[data-page="operations"]');
        if (operationsLink) operationsLink.click();
    });
    
    // Apply filters
    const applyBtn = container.querySelector('#apply-filters');
    applyBtn.addEventListener('click', () => {
        currentFilters.userId = container.querySelector('#filter-userId').value;
        currentFilters.pluginType = container.querySelector('#filter-pluginType').value || null;
        currentFilters.sortBy = container.querySelector('#filter-sortBy').value;
        currentFilters.sortOrder = container.querySelector('#filter-sortOrder').value;
        currentPage = 1;
        loadMemories(container);
    });
    
    // Reset filters
    const resetBtn = container.querySelector('#reset-filters');
    resetBtn.addEventListener('click', () => {
        currentFilters = {
            userId: 'demo-user',
            pluginType: null,
            sortBy: 'createdAt',
            sortOrder: 'desc',
            searchTerm: ''
        };
        container.querySelector('#filter-userId').value = currentFilters.userId;
        container.querySelector('#filter-pluginType').value = '';
        container.querySelector('#filter-sortBy').value = currentFilters.sortBy;
        container.querySelector('#filter-sortOrder').value = currentFilters.sortOrder;
        currentPage = 1;
        loadMemories(container);
    });
    
    // Clear all memories
    const clearBtn = container.querySelector('#clear-all-btn');
    clearBtn.addEventListener('click', async () => {
        const userId = container.querySelector('#filter-userId').value;
        const confirmed = await confirmModal(
            `Are you sure you want to delete ALL memories for user "${userId}"? This action cannot be undone.`,
            'Confirm Deletion',
            { confirmClass: 'btn-danger', confirmText: 'Delete All' }
        );
        
        if (confirmed) {
            try {
                const result = await API.deleteMemories(userId);
                toast.success(`Deleted ${result.deletedCount} memories for ${result.userId}`);
                loadMemories(container);
            } catch (error) {
                toast.error('Failed to delete memories: ' + error.message);
            }
        }
    });
}

async function loadMemories(container) {
    const listContainer = container.querySelector('#memory-list-container');
    const paginationContainer = container.querySelector('#pagination-container');
    
    // Show loading
    listContainer.innerHTML = `
        <div style="text-align: center; padding: var(--spacing-2xl);">
            <div class="spinner" style="width: 40px; height: 40px; border-width: 4px;"></div>
            <p style="margin-top: var(--spacing-md); color: var(--text-secondary);">Loading memories...</p>
        </div>
    `;
    
    try {
        const result = await API.getMemories({
            userId: currentFilters.userId,
            pluginType: currentFilters.pluginType,
            page: currentPage,
            perPage: itemsPerPage,
            sortBy: currentFilters.sortBy,
            sortOrder: currentFilters.sortOrder
        });
        
        const memories = result.memories || [];
        
        // Clear containers
        listContainer.innerHTML = '';
        paginationContainer.innerHTML = '';
        
        if (memories.length === 0) {
            listContainer.innerHTML = `
                <div class="empty-state">
                    <div class="empty-state-icon">📭</div>
                    <div class="empty-state-title">No memories found</div>
                    <div class="empty-state-message">Load demo data to get started</div>
                    <button class="btn btn-primary" id="load-demo-inline" style="margin-top: var(--spacing-lg);">
                        🚀 Load Demo Data
                    </button>
                </div>
            `;
            
            // Add event listener for inline demo load
            const loadBtn = listContainer.querySelector('#load-demo-inline');
            if (loadBtn) {
                loadBtn.addEventListener('click', async () => {
                    loadBtn.disabled = true;
                    loadBtn.textContent = 'Loading...';
                    await loadDemoDataInline(container);
                    setTimeout(() => loadMemories(container), 1000); // Reload after adding
                });
            }
            return;
        }
        
        // Render memories
        const memoryList = document.createElement('div');
        memoryList.style.display = 'flex';
        memoryList.style.flexDirection = 'column';
        memoryList.style.gap = 'var(--spacing-md)';
        
        memories.forEach(memory => {
            const card = createMemoryCard(memory, {
                showActions: true,
                onView: (mem) => showMemoryDetails(mem),
                onEdit: (mem) => showEditMemory(mem, container),
                onDelete: (mem) => deleteMemory(mem, container)
            });
            memoryList.appendChild(card);
        });
        
        listContainer.appendChild(memoryList);
        
        // Render pagination
        const totalPages = Math.ceil(memories.length / itemsPerPage);
        const pagination = createPagination({
            currentPage,
            totalPages: totalPages > 0 ? totalPages : 1,
            totalItems: memories.length,
            itemsPerPage,
            onPageChange: (page) => {
                currentPage = page;
                loadMemories(container);
            },
            onPerPageChange: (perPage) => {
                itemsPerPage = perPage;
                currentPage = 1;
                loadMemories(container);
            }
        });
        
        paginationContainer.appendChild(pagination);
        
    } catch (error) {
        console.error('[Memories] Failed to load memories:', error);
        listContainer.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">⚠️</div>
                <div class="empty-state-title">Failed to load memories</div>
                <div class="empty-state-message">${error.message}</div>
            </div>
        `;
    }
}

function showMemoryDetails(memory) {
    const content = document.createElement('div');
    content.style.display = 'flex';
    content.style.flexDirection = 'column';
    content.style.gap = 'var(--spacing-md)';
    
    content.innerHTML = `
        <div>
            <div style="font-size: 0.875rem; color: var(--text-tertiary); margin-bottom: var(--spacing-sm);">Memory ID</div>
            <div style="font-family: var(--font-mono); color: var(--text-primary);">${memory.id}</div>
        </div>
        
        <div>
            <div style="font-size: 0.875rem; color: var(--text-tertiary); margin-bottom: var(--spacing-sm);">Content</div>
            <div style="padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-md); line-height: 1.6;">
                ${escapeHtml(memory.content)}
            </div>
        </div>
        
        <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: var(--spacing-md);">
            <div>
                <div style="font-size: 0.875rem; color: var(--text-tertiary);">User ID</div>
                <div style="color: var(--text-primary);">${memory.userId}</div>
            </div>
            <div>
                <div style="font-size: 0.875rem; color: var(--text-tertiary);">Plugin Type</div>
                <div style="color: var(--text-primary);">${memory.type || 'N/A'}</div>
            </div>
            <div>
                <div style="font-size: 0.875rem; color: var(--text-tertiary);">Decay</div>
                <div style="color: var(--text-primary);">${memory.decay.toFixed(3)}</div>
            </div>
            <div>
                <div style="font-size: 0.875rem; color: var(--text-tertiary);">Importance</div>
                <div style="color: var(--text-primary);">${memory.importance.toFixed(3)}</div>
            </div>
            <div>
                <div style="font-size: 0.875rem; color: var(--text-tertiary);">Token Count</div>
                <div style="color: var(--text-primary);">${memory.tokenCount}</div>
            </div>
            <div>
                <div style="font-size: 0.875rem; color: var(--text-tertiary);">Created At</div>
                <div style="color: var(--text-primary);">${new Date(memory.createdAt).toLocaleString()}</div>
            </div>
        </div>
    `;
    
    const modal = new Modal({
        title: '👁️ Memory Details',
        content,
        size: 'lg'
    });
    
    modal.open();
}

async function showEditMemory(memory, container) {
    const form = document.createElement('form');
    form.id = 'edit-memory-form';
    form.innerHTML = `
        <div class="form-group">
            <label class="form-label">Content</label>
            <textarea class="form-textarea" id="edit-content" rows="6" required>${escapeHtml(memory.content)}</textarea>
        </div>
        
        <div class="form-group">
            <label class="form-label">Importance (0-1)</label>
            <input type="number" class="form-input" id="edit-importance" min="0" max="1" step="0.1" value="${memory.importance}">
        </div>
        
        <div style="padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-md); font-size: 0.875rem; color: var(--text-secondary);">
            💡 <strong>Note:</strong> Editing will re-embed the content and may reset some properties.
        </div>
    `;
    
    const footer = document.createElement('div');
    footer.style.display = 'flex';
    footer.style.gap = 'var(--spacing-sm)';
    
    const cancelBtn = document.createElement('button');
    cancelBtn.type = 'button';
    cancelBtn.className = 'btn btn-secondary';
    cancelBtn.textContent = 'Cancel';
    
    const saveBtn = document.createElement('button');
    saveBtn.type = 'submit';
    saveBtn.className = 'btn btn-primary';
    saveBtn.textContent = '💾 Save Changes';
    
    footer.appendChild(cancelBtn);
    footer.appendChild(saveBtn);
    
    const modal = new Modal({
        title: '✏️ Edit Memory',
        content: form,
        footer,
        size: 'lg'
    });
    
    modal.open();
    
    cancelBtn.addEventListener('click', () => modal.close());
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const content = document.getElementById('edit-content').value;
        const importance = parseFloat(document.getElementById('edit-importance').value);
        
        try {
            saveBtn.disabled = true;
            saveBtn.textContent = 'Saving...';
            
            await API.updateMemory(memory.id, { content, importance });
            
            toast.success('Memory updated successfully');
            modal.close();
            
            // Reload memories after a short delay to allow backend to process
            setTimeout(() => loadMemories(container), 500);
            
        } catch (error) {
            if (error.type === 'IMMUTABLE') {
                toast.error('This memory is immutable and cannot be edited', 'Cannot Edit');
            } else {
                toast.error('Failed to update memory: ' + error.message);
            }
            saveBtn.disabled = false;
            saveBtn.textContent = '💾 Save Changes';
        }
    });
}

async function deleteMemory(memory, container) {
    // Note: We don't have a single memory delete endpoint, so we'll show a message
    const modal = new Modal({
        title: '⚠️ Delete Memory',
        content: `
            <p>Individual memory deletion is not yet available in the API.</p>
            <p style="margin-top: var(--spacing-md);">You can delete all memories for a user using the "Clear All Memories" button.</p>
            <div style="margin-top: var(--spacing-md); padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-md);">
                <div style="font-size: 0.875rem; color: var(--text-tertiary);">Memory ID</div>
                <div style="font-family: var(--font-mono); color: var(--text-primary); margin-top: var(--spacing-xs);">${memory.id}</div>
            </div>
        `,
        size: 'md'
    });
    
    modal.open();
}

async function loadDemoDataInline(container) {
    const demoMemories = [
        { content: "User loves pizza margherita with extra basil", pluginType: "USER_PREFERENCE" },
        { content: "User prefers al dente pasta carbonara", pluginType: "USER_PREFERENCE" },
        { content: "User drinks only black coffee, no sugar", pluginType: "USER_PREFERENCE" },
        { content: "User is vegetarian, doesn't eat meat", pluginType: "USER_PREFERENCE" },
        { content: "API endpoint /memories returns user memories", pluginType: "DOCUMENTATION" },
        { content: "Discussed vacation plans for summer in Italy", pluginType: "CONVERSATION" }
    ];
    
    try {
        let saved = 0;
        for (const demo of demoMemories) {
            try {
                await API.createMemory({
                    userId: 'demo-user',
                    content: demo.content,
                    pluginType: demo.pluginType,
                    importance: 0.7
                });
                saved++;
            } catch (error) {
                // Ignore duplicates
            }
        }
        
        toast.success(`Loaded ${saved} demo memories!`);
    } catch (error) {
        toast.error('Failed to load demo data: ' + error.message);
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

