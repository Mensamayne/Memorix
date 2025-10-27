/**
 * Operations Page
 * CRUD operations for memories
 */

import { API } from '../api.js';
import { AppState } from '../state.js';
import { toast } from '../components/toast.js';

export async function renderOperations(container) {
    container.innerHTML = `
        <div class="operations-page">
            <!-- Page Header -->
            <div style="margin-bottom: var(--spacing-xl);">
                <h1 style="margin-bottom: var(--spacing-sm);">⚡ Operations</h1>
                <p style="color: var(--text-secondary);">Create and update memories</p>
            </div>
            
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: var(--spacing-xl);">
                <!-- Create Memory -->
                <div class="card">
                    <div class="card-header">
                        <h2 class="card-title">➕ Create Memory</h2>
                    </div>
                    <div class="card-body">
                        <form id="create-memory-form">
                            <div class="form-group">
                                <label class="form-label">User ID</label>
                                <input type="text" class="form-input" id="create-userId" value="demo-user" required>
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Plugin Type</label>
                                <select class="form-select" id="create-pluginType" required>
                                </select>
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Content</label>
                                <textarea class="form-textarea" id="create-content" rows="6" placeholder="Enter memory content..." required></textarea>
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Importance (0-1)</label>
                                <input type="number" class="form-input" id="create-importance" min="0" max="1" step="0.1" value="0.5" required>
                            </div>
                            
                            <button type="submit" class="btn btn-primary" style="width: 100%;">
                                💾 Save Memory
                            </button>
                        </form>
                    </div>
                </div>
                
                <!-- Update Memory -->
                <div class="card">
                    <div class="card-header">
                        <h2 class="card-title">✏️ Update Memory</h2>
                    </div>
                    <div class="card-body">
                        <form id="update-memory-form">
                            <div class="form-group">
                                <label class="form-label">Memory ID</label>
                                <input type="text" class="form-input" id="update-memoryId" placeholder="Enter memory ID to update" required>
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Content (optional)</label>
                                <textarea class="form-textarea" id="update-content" rows="6" placeholder="Leave empty to keep current"></textarea>
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Importance (optional)</label>
                                <input type="number" class="form-input" id="update-importance" min="0" max="1" step="0.1" placeholder="Leave empty to keep current">
                            </div>
                            
                            <div style="padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-md); font-size: 0.875rem; color: var(--text-secondary); margin-bottom: var(--spacing-md);">
                                💡 <strong>Note:</strong> Only fill in fields you want to update. Empty fields will keep their current values.
                            </div>
                            
                            <button type="submit" class="btn btn-warning" style="width: 100%;">
                                🔄 Update Memory
                            </button>
                        </form>
                    </div>
                </div>
            </div>
            
            <!-- Quick Actions -->
            <div class="card" style="margin-top: var(--spacing-xl);">
                <div class="card-header">
                    <h2 class="card-title">🎨 Quick Actions</h2>
                </div>
                <div class="card-body">
                    <div style="display: flex; gap: var(--spacing-md); flex-wrap: wrap;">
                        <button class="btn btn-success" id="load-demo-data">
                            🚀 Load Demo Data
                        </button>
                        <button class="btn btn-secondary" id="test-deduplication">
                            🔄 Test Deduplication
                        </button>
                    </div>
                </div>
            </div>
            
            <!-- Result Display -->
            <div class="card" style="margin-top: var(--spacing-xl);">
                <div class="card-header">
                    <h2 class="card-title">📋 Result</h2>
                </div>
                <div class="card-body">
                    <div id="operation-result">
                        <div class="empty-state">
                            <div class="empty-state-icon">🎯</div>
                            <div class="empty-state-message">Perform an operation to see results</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    // Populate plugin types
    const createPluginSelect = container.querySelector('#create-pluginType');
    AppState.plugins.forEach(plugin => {
        const option = document.createElement('option');
        option.value = plugin;
        option.textContent = plugin;
        createPluginSelect.appendChild(option);
    });
    
    // Setup event listeners
    setupEventListeners(container);
}

function setupEventListeners(container) {
    // Create form
    const createForm = container.querySelector('#create-memory-form');
    createForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const data = {
            userId: container.querySelector('#create-userId').value,
            pluginType: container.querySelector('#create-pluginType').value,
            content: container.querySelector('#create-content').value,
            importance: parseFloat(container.querySelector('#create-importance').value)
        };
        
        try {
            const submitBtn = createForm.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = 'Saving...';
            
            const result = await API.createMemory(data);
            
            toast.success('Memory created successfully! View it in the Memories page.');
            displayResult(container, result, 'create');
            
            // Clear form
            container.querySelector('#create-content').value = '';
            
            submitBtn.disabled = false;
            submitBtn.textContent = '💾 Save Memory';
            
        } catch (error) {
            if (error.type === 'DUPLICATE') {
                toast.warning('Duplicate detected - memory was merged', 'Duplicate');
                displayResult(container, error.data, 'merge');
            } else {
                toast.error('Failed to create memory: ' + error.message);
            }
            
            const submitBtn = createForm.querySelector('button[type="submit"]');
            submitBtn.disabled = false;
            submitBtn.textContent = '💾 Save Memory';
        }
    });
    
    // Update form
    const updateForm = container.querySelector('#update-memory-form');
    updateForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const memoryId = container.querySelector('#update-memoryId').value;
        const content = container.querySelector('#update-content').value;
        const importance = container.querySelector('#update-importance').value;
        
        const data = {};
        if (content) data.content = content;
        if (importance) data.importance = parseFloat(importance);
        
        if (Object.keys(data).length === 0) {
            toast.warning('Please provide at least one field to update');
            return;
        }
        
        try {
            const submitBtn = updateForm.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            submitBtn.textContent = 'Updating...';
            
            const result = await API.updateMemory(memoryId, data);
            
            toast.success('Memory updated successfully!');
            displayResult(container, result, 'update');
            
            // Clear form
            container.querySelector('#update-memoryId').value = '';
            container.querySelector('#update-content').value = '';
            container.querySelector('#update-importance').value = '';
            
            submitBtn.disabled = false;
            submitBtn.textContent = '🔄 Update Memory';
            
        } catch (error) {
            if (error.type === 'IMMUTABLE') {
                toast.error('This memory is immutable and cannot be updated', 'Cannot Update');
            } else {
                toast.error('Failed to update memory: ' + error.message);
            }
            
            const submitBtn = updateForm.querySelector('button[type="submit"]');
            submitBtn.disabled = false;
            submitBtn.textContent = '🔄 Update Memory';
        }
    });
    
    // Quick Actions
    container.querySelector('#load-demo-data').addEventListener('click', () => loadDemoData(container));
    container.querySelector('#test-deduplication').addEventListener('click', () => testDeduplication(container));
}

function displayResult(container, memory, operation) {
    const resultDiv = container.querySelector('#operation-result');
    
    const operationLabels = {
        create: '✅ Created',
        update: '🔄 Updated',
        merge: '🔗 Merged'
    };
    
    resultDiv.innerHTML = `
        <div style="padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-lg); border: 2px solid var(--primary-500);">
            <div style="display: flex; align-items: center; gap: var(--spacing-sm); margin-bottom: var(--spacing-md);">
                <span class="badge badge-success">${operationLabels[operation] || 'Result'}</span>
                <span class="text-sm text-muted">Memory ID: <code style="color: var(--primary-500);">${memory.id}</code></span>
            </div>
            
            <div style="margin-bottom: var(--spacing-md);">
                <div class="text-sm text-muted" style="margin-bottom: var(--spacing-xs);">Content:</div>
                <div style="padding: var(--spacing-md); background: var(--bg-primary); border-radius: var(--radius-md); line-height: 1.6;">
                    ${escapeHtml(memory.content)}
                </div>
            </div>
            
            <div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--spacing-md); font-size: 0.875rem;">
                <div>
                    <div class="text-muted">Decay</div>
                    <div><strong>${memory.decay.toFixed(3)}</strong></div>
                </div>
                <div>
                    <div class="text-muted">Importance</div>
                    <div><strong>${memory.importance.toFixed(3)}</strong></div>
                </div>
                <div>
                    <div class="text-muted">Tokens</div>
                    <div><strong>${memory.tokenCount}</strong></div>
                </div>
            </div>
        </div>
    `;
}

async function loadDemoData(container) {
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
        
        const resultDiv = container.querySelector('#operation-result');
        resultDiv.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">✅</div>
                <div class="empty-state-title">Demo Data Loaded</div>
                <div class="empty-state-message">Created ${saved} memories. Check the Memories page to view them.</div>
            </div>
        `;
        
    } catch (error) {
        toast.error('Failed to load demo data: ' + error.message);
    }
}

async function testDeduplication(container) {
    const content = 'User loves pizza margherita';
    
    try {
        // First save
        const memory1 = await API.createMemory({
            userId: 'demo-user',
            content,
            pluginType: 'USER_PREFERENCE',
            importance: 0.8
        });
        
        // Second save (should merge)
        const memory2 = await API.createMemory({
            userId: 'demo-user',
            content,
            pluginType: 'USER_PREFERENCE',
            importance: 0.8
        });
        
        const resultDiv = container.querySelector('#operation-result');
        const isMerged = memory1.id === memory2.id;
        
        resultDiv.innerHTML = `
            <div style="padding: var(--spacing-lg);">
                <h3 style="margin-bottom: var(--spacing-lg); color: var(--primary-500);">🔄 Deduplication Test Results</h3>
                
                <div style="margin-bottom: var(--spacing-xl);">
                    <div style="font-weight: 600; margin-bottom: var(--spacing-sm);">First Save:</div>
                    <div style="padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-md);">
                        <div class="text-sm text-muted">ID: ${memory1.id}</div>
                        <div class="text-sm">Decay: ${memory1.decay.toFixed(3)}</div>
                    </div>
                </div>
                
                <div style="margin-bottom: var(--spacing-xl);">
                    <div style="font-weight: 600; margin-bottom: var(--spacing-sm);">Second Save (duplicate):</div>
                    <div style="padding: var(--spacing-md); background: var(--bg-tertiary); border-radius: var(--radius-md); ${isMerged ? 'border: 2px solid var(--warning-500);' : ''}">
                        <div class="text-sm text-muted">ID: ${memory2.id}</div>
                        <div class="text-sm">Decay: ${memory2.decay.toFixed(3)}</div>
                    </div>
                </div>
                
                <div style="padding: var(--spacing-lg); background: ${isMerged ? 'rgba(16, 185, 129, 0.1)' : 'rgba(245, 158, 11, 0.1)'}; border-radius: var(--radius-lg); border: 2px solid ${isMerged ? 'var(--success-500)' : 'var(--warning-500)'};">
                    ${isMerged ? `
                        <div style="font-size: 1.125rem; font-weight: 600; color: var(--success-500); margin-bottom: var(--spacing-sm);">
                            ✅ Deduplication Worked!
                        </div>
                        <div style="color: var(--text-secondary);">
                            Same ID returned, decay reinforced from ${memory1.decay.toFixed(3)} → ${memory2.decay.toFixed(3)}
                        </div>
                    ` : `
                        <div style="font-size: 1.125rem; font-weight: 600; color: var(--warning-500); margin-bottom: var(--spacing-sm);">
                            ⚠️ Different IDs
                        </div>
                        <div style="color: var(--text-secondary);">
                            Deduplication may be disabled for this plugin type
                        </div>
                    `}
                </div>
            </div>
        `;
        
        if (isMerged) {
            toast.success('Deduplication test successful!');
        } else {
            toast.info('Created separate memories');
        }
        
    } catch (error) {
        toast.error('Deduplication test failed: ' + error.message);
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

