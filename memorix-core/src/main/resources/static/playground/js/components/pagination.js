/**
 * Pagination Component
 */

export function createPagination(options = {}) {
    const {
        currentPage = 1,
        totalPages = 1,
        totalItems = 0,
        itemsPerPage = 20,
        onPageChange = null,
        maxVisible = 7
    } = options;
    
    const container = document.createElement('div');
    container.className = 'pagination-container';
    container.style.display = 'flex';
    container.style.alignItems = 'center';
    container.style.justifyContent = 'space-between';
    container.style.marginTop = 'var(--spacing-xl)';
    container.style.padding = 'var(--spacing-md)';
    container.style.background = 'var(--bg-secondary)';
    container.style.borderRadius = 'var(--radius-lg)';
    container.style.border = '1px solid var(--border-color)';
    
    // Info text
    const startItem = Math.min((currentPage - 1) * itemsPerPage + 1, totalItems);
    const endItem = Math.min(currentPage * itemsPerPage, totalItems);
    
    const info = document.createElement('div');
    info.className = 'pagination-info';
    info.style.color = 'var(--text-secondary)';
    info.style.fontSize = '0.875rem';
    info.innerHTML = `
        Showing <strong>${startItem}-${endItem}</strong> of <strong>${totalItems}</strong> memories
    `;
    
    // Pagination controls
    const controls = document.createElement('div');
    controls.className = 'pagination';
    
    // Previous button
    const prevBtn = createPaginationButton('← Prev', currentPage === 1);
    prevBtn.addEventListener('click', () => {
        if (currentPage > 1 && onPageChange) {
            onPageChange(currentPage - 1);
        }
    });
    controls.appendChild(prevBtn);
    
    // Page numbers
    const pages = generatePageNumbers(currentPage, totalPages, maxVisible);
    pages.forEach(page => {
        if (page === '...') {
            const ellipsis = document.createElement('span');
            ellipsis.className = 'pagination-ellipsis';
            ellipsis.textContent = '...';
            ellipsis.style.padding = 'var(--spacing-sm) var(--spacing-md)';
            ellipsis.style.color = 'var(--text-tertiary)';
            controls.appendChild(ellipsis);
        } else {
            const pageBtn = createPaginationButton(page, false, page === currentPage);
            pageBtn.addEventListener('click', () => {
                if (page !== currentPage && onPageChange) {
                    onPageChange(page);
                }
            });
            controls.appendChild(pageBtn);
        }
    });
    
    // Next button
    const nextBtn = createPaginationButton('Next →', currentPage === totalPages);
    nextBtn.addEventListener('click', () => {
        if (currentPage < totalPages && onPageChange) {
            onPageChange(currentPage + 1);
        }
    });
    controls.appendChild(nextBtn);
    
    // Per page selector
    const perPageSelector = document.createElement('div');
    perPageSelector.style.display = 'flex';
    perPageSelector.style.alignItems = 'center';
    perPageSelector.style.gap = 'var(--spacing-sm)';
    perPageSelector.innerHTML = `
        <label style="font-size: 0.875rem; color: var(--text-secondary);">Per page:</label>
        <select class="form-select" style="width: auto; padding: var(--spacing-xs) var(--spacing-sm);">
            <option value="20" ${itemsPerPage === 20 ? 'selected' : ''}>20</option>
            <option value="50" ${itemsPerPage === 50 ? 'selected' : ''}>50</option>
            <option value="100" ${itemsPerPage === 100 ? 'selected' : ''}>100</option>
        </select>
    `;
    
    const select = perPageSelector.querySelector('select');
    select.addEventListener('change', (e) => {
        if (options.onPerPageChange) {
            options.onPerPageChange(parseInt(e.target.value));
        }
    });
    
    container.appendChild(info);
    container.appendChild(controls);
    container.appendChild(perPageSelector);
    
    return container;
}

function createPaginationButton(text, disabled = false, active = false) {
    const button = document.createElement('button');
    button.className = 'pagination-btn';
    button.textContent = text;
    button.disabled = disabled;
    
    if (active) {
        button.classList.add('active');
    }
    
    return button;
}

function generatePageNumbers(current, total, maxVisible) {
    if (total <= maxVisible) {
        return Array.from({ length: total }, (_, i) => i + 1);
    }
    
    const pages = [];
    const halfVisible = Math.floor(maxVisible / 2);
    
    // Always show first page
    pages.push(1);
    
    let start = Math.max(2, current - halfVisible);
    let end = Math.min(total - 1, current + halfVisible);
    
    // Adjust if we're near the start
    if (current <= halfVisible + 1) {
        end = Math.min(total - 1, maxVisible - 1);
    }
    
    // Adjust if we're near the end
    if (current >= total - halfVisible) {
        start = Math.max(2, total - maxVisible + 2);
    }
    
    // Add ellipsis after first page if needed
    if (start > 2) {
        pages.push('...');
    }
    
    // Add middle pages
    for (let i = start; i <= end; i++) {
        pages.push(i);
    }
    
    // Add ellipsis before last page if needed
    if (end < total - 1) {
        pages.push('...');
    }
    
    // Always show last page
    if (total > 1) {
        pages.push(total);
    }
    
    return pages;
}

