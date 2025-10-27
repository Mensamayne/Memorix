/**
 * Search Page
 * Semantic search with advanced options
 */

import { API } from '../api.js';
import { AppState } from '../state.js';
import { toast } from '../components/toast.js';
import { createMemoryCard } from '../components/memory-card.js';

export async function renderSearch(container) {
    container.innerHTML = `
        <div class="search-page">
            <!-- Page Header -->
            <div style="margin-bottom: var(--spacing-xl);">
                <h1 style="margin-bottom: var(--spacing-sm);">🔍 Semantic Search</h1>
                <p style="color: var(--text-secondary);">Find memories using AI-powered semantic search</p>
            </div>
            
            <!-- Search Form -->
            <div class="card" style="margin-bottom: var(--spacing-xl);">
                <div class="card-body">
                    <form id="search-form">
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">User ID</label>
                                <input type="text" class="form-input" id="search-userId" value="demo-user" required>
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Plugin Type</label>
                                <select class="form-select" id="search-pluginType" required>
                                </select>
                            </div>
                        </div>
                        
                        <div class="form-group">
                            <label class="form-label">Search Query</label>
                            <input type="text" class="form-input" id="search-query" placeholder="e.g., food preferences, vacation plans..." required>
                        </div>
                        
                        <div class="form-row">
                            <div class="form-group">
                                <label class="form-label">Max Count</label>
                                <input type="number" class="form-input" id="search-maxCount" value="20" min="1" max="100">
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Max Tokens</label>
                                <input type="number" class="form-input" id="search-maxTokens" value="500" min="0">
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Min Similarity</label>
                                <input type="number" class="form-input" id="search-minSimilarity" value="0.5" min="0" max="1" step="0.1">
                            </div>
                            
                            <div class="form-group">
                                <label class="form-label">Strategy</label>
                                <select class="form-select" id="search-strategy">
                                    <option value="GREEDY">GREEDY</option>
                                    <option value="ALL">ALL</option>
                                    <option value="ANY">ANY</option>
                                    <option value="FIRST_MET">FIRST_MET</option>
                                </select>
                            </div>
                        </div>
                        
                        <button type="submit" class="btn btn-primary" style="width: 100%;">
                            🔍 Search Memories
                        </button>
                    </form>
                </div>
            </div>
            
            <!-- Results -->
            <div id="search-results">
                <div class="empty-state">
                    <div class="empty-state-icon">🔍</div>
                    <div class="empty-state-message">Enter a search query to find memories</div>
                </div>
            </div>
        </div>
    `;
    
    // Populate plugin types
    const pluginSelect = container.querySelector('#search-pluginType');
    AppState.plugins.forEach(plugin => {
        const option = document.createElement('option');
        option.value = plugin;
        option.textContent = plugin;
        pluginSelect.appendChild(option);
    });
    
    // Setup event listener
    const searchForm = container.querySelector('#search-form');
    searchForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        await performSearch(container);
    });
}

async function performSearch(container) {
    const resultsDiv = container.querySelector('#search-results');
    const submitBtn = container.querySelector('button[type="submit"]');
    
    // Show loading
    resultsDiv.innerHTML = `
        <div style="text-align: center; padding: var(--spacing-2xl);">
            <div class="spinner" style="width: 40px; height: 40px; border-width: 4px;"></div>
            <p style="margin-top: var(--spacing-md); color: var(--text-secondary);">Searching...</p>
        </div>
    `;
    
    submitBtn.disabled = true;
    submitBtn.textContent = 'Searching...';
    
    try {
        const searchRequest = {
            userId: container.querySelector('#search-userId').value,
            pluginType: container.querySelector('#search-pluginType').value,
            query: container.querySelector('#search-query').value,
            maxCount: parseInt(container.querySelector('#search-maxCount').value),
            maxTokens: parseInt(container.querySelector('#search-maxTokens').value) || null,
            minSimilarity: parseFloat(container.querySelector('#search-minSimilarity').value) || null,
            strategy: container.querySelector('#search-strategy').value
        };
        
        const result = await API.searchMemories(searchRequest);
        
        displaySearchResults(resultsDiv, result);
        
        toast.success(`Found ${result.memories.length} memories`);
        
    } catch (error) {
        console.error('[Search] Search failed:', error);
        resultsDiv.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">⚠️</div>
                <div class="empty-state-title">Search Failed</div>
                <div class="empty-state-message">${error.message}</div>
            </div>
        `;
        toast.error('Search failed: ' + error.message);
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = '🔍 Search Memories';
    }
}

function displaySearchResults(container, result) {
    const metadata = result.metadata;
    const memories = result.memories;
    
    container.innerHTML = '';
    
    // Metadata Card
    const metadataCard = document.createElement('div');
    metadataCard.className = 'card';
    metadataCard.style.marginBottom = 'var(--spacing-xl)';
    metadataCard.innerHTML = `
        <div class="card-header">
            <h3 class="card-title">📊 Query Metadata</h3>
        </div>
        <div class="card-body">
            <div class="stat-grid">
                <div class="stat-card">
                    <div class="stat-value">${metadata.totalFound}</div>
                    <div class="stat-label">Total Found</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${metadata.returned}</div>
                    <div class="stat-label">Returned</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${metadata.totalTokens}</div>
                    <div class="stat-label">Total Tokens</div>
                </div>
                <div class="stat-card">
                    <div class="stat-value">${metadata.executionTimeMs}ms</div>
                    <div class="stat-label">Execution Time</div>
                </div>
            </div>
            <div style="margin-top: var(--spacing-md); text-align: center;">
                <span class="badge badge-primary">Limited by: ${metadata.limitReason}</span>
            </div>
        </div>
    `;
    
    container.appendChild(metadataCard);
    
    // Results
    if (memories.length === 0) {
        const emptyState = document.createElement('div');
        emptyState.className = 'empty-state';
        emptyState.innerHTML = `
            <div class="empty-state-icon">😕</div>
            <div class="empty-state-title">No memories found</div>
            <div class="empty-state-message">Try adjusting your search query or filters</div>
        `;
        container.appendChild(emptyState);
        return;
    }
    
    const resultsHeader = document.createElement('div');
    resultsHeader.style.marginBottom = 'var(--spacing-md)';
    resultsHeader.innerHTML = `<h3>📋 Results (${memories.length})</h3>`;
    container.appendChild(resultsHeader);
    
    const memoryList = document.createElement('div');
    memoryList.style.display = 'flex';
    memoryList.style.flexDirection = 'column';
    memoryList.style.gap = 'var(--spacing-md)';
    
    memories.forEach((memory, index) => {
        const card = createMemoryCard(memory, {
            showActions: false
        });
        // Add rank badge
        const header = card.querySelector('.card-header');
        const rankBadge = document.createElement('span');
        rankBadge.className = 'badge badge-success';
        rankBadge.textContent = `#${index + 1}`;
        rankBadge.style.marginRight = 'var(--spacing-sm)';
        header.insertBefore(rankBadge, header.firstChild);
        
        memoryList.appendChild(card);
    });
    
    container.appendChild(memoryList);
}

