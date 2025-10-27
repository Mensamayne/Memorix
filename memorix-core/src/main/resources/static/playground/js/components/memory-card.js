/**
 * Memory Card Component
 */

export function createMemoryCard(memory, options = {}) {
    const {
        showActions = true,
        onView = null,
        onEdit = null,
        onDelete = null,
        selectable = false,
        selected = false
    } = options;
    
    const card = document.createElement('div');
    card.className = 'card memory-card';
    card.dataset.memoryId = memory.id;
    
    // Format date
    const createdDate = new Date(memory.createdAt).toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
    
    // Truncate content if too long
    const maxContentLength = 200;
    let displayContent = memory.content;
    let isTruncated = false;
    
    if (displayContent.length > maxContentLength) {
        displayContent = displayContent.substring(0, maxContentLength) + '...';
        isTruncated = true;
    }
    
    // Plugin type icon
    const pluginIcons = {
        'USER_PREFERENCE': '⭐',
        'DOCUMENTATION': '📚',
        'CONVERSATION': '💬',
        'SYSTEM': '⚙️'
    };
    const pluginIcon = pluginIcons[memory.type] || '📝';
    
    card.innerHTML = `
        <div class="card-header">
            <div style="display: flex; align-items: center; gap: var(--spacing-sm); flex: 1;">
                ${selectable ? `
                    <input type="checkbox" class="form-checkbox memory-checkbox" 
                           ${selected ? 'checked' : ''} 
                           data-memory-id="${memory.id}">
                ` : ''}
                <div>
                    <div style="display: flex; align-items: center; gap: var(--spacing-sm); margin-bottom: var(--spacing-xs);">
                        <span class="badge badge-primary">${pluginIcon} ${memory.type || 'N/A'}</span>
                        <span class="text-xs text-muted font-mono">ID: ${memory.id}</span>
                    </div>
                    <div class="text-sm text-muted">
                        <span>User: <strong>${memory.userId}</strong></span>
                    </div>
                </div>
            </div>
            <div style="display: flex; gap: var(--spacing-xs);">
                <span class="badge badge-warning">Decay: ${memory.decay.toFixed(2)}</span>
                <span class="badge badge-secondary">${memory.tokenCount} tokens</span>
            </div>
        </div>
        
        <div class="card-body">
            <div class="memory-content" style="line-height: 1.6; color: var(--text-primary); margin-bottom: var(--spacing-md);">
                ${escapeHtml(displayContent)}
            </div>
            
            <div style="display: flex; align-items: center; justify-content: space-between; font-size: 0.875rem; color: var(--text-tertiary);">
                <div style="display: flex; gap: var(--spacing-md);">
                    <span>📅 ${createdDate}</span>
                    <span>💎 Importance: ${memory.importance.toFixed(2)}</span>
                </div>
            </div>
        </div>
        
        ${showActions ? `
            <div class="card-footer">
                <button class="btn btn-sm btn-ghost memory-action-view" data-action="view">
                    👁️ View
                </button>
                <button class="btn btn-sm btn-ghost memory-action-edit" data-action="edit">
                    ✏️ Edit
                </button>
                <button class="btn btn-sm btn-ghost btn-danger memory-action-delete" data-action="delete">
                    🗑️ Delete
                </button>
            </div>
        ` : ''}
    `;
    
    // Attach event listeners for actions
    if (showActions) {
        const viewBtn = card.querySelector('.memory-action-view');
        const editBtn = card.querySelector('.memory-action-edit');
        const deleteBtn = card.querySelector('.memory-action-delete');
        
        if (onView) {
            viewBtn.addEventListener('click', () => onView(memory));
        }
        
        if (onEdit) {
            editBtn.addEventListener('click', () => onEdit(memory));
        }
        
        if (onDelete) {
            deleteBtn.addEventListener('click', () => onDelete(memory));
        }
    }
    
    return card;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// Helper to create memory list container
export function createMemoryList(memories, options = {}) {
    const container = document.createElement('div');
    container.className = 'memory-list';
    container.style.display = 'flex';
    container.style.flexDirection = 'column';
    container.style.gap = 'var(--spacing-md)';
    
    if (memories.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">🔍</div>
                <div class="empty-state-title">No memories found</div>
                <div class="empty-state-message">Try adjusting your filters or create a new memory</div>
            </div>
        `;
        return container;
    }
    
    memories.forEach(memory => {
        const card = createMemoryCard(memory, options);
        container.appendChild(card);
    });
    
    return container;
}

