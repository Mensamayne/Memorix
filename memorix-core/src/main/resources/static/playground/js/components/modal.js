/**
 * Modal Component
 */

export class Modal {
    constructor(options = {}) {
        this.options = {
            title: options.title || 'Modal',
            content: options.content || '',
            footer: options.footer || null,
            size: options.size || 'md', // sm, md, lg
            onClose: options.onClose || null,
            closeOnOverlayClick: options.closeOnOverlayClick !== false
        };
        
        this.overlay = null;
        this.modal = null;
    }
    
    open() {
        // Create overlay
        this.overlay = document.createElement('div');
        this.overlay.className = 'modal-overlay';
        
        // Create modal
        this.modal = document.createElement('div');
        this.modal.className = `modal modal-${this.options.size}`;
        
        // Header
        const header = document.createElement('div');
        header.className = 'modal-header';
        header.innerHTML = `
            <h3 class="modal-title">${this.escapeHtml(this.options.title)}</h3>
            <button class="modal-close" aria-label="Close">×</button>
        `;
        
        // Body
        const body = document.createElement('div');
        body.className = 'modal-body';
        if (typeof this.options.content === 'string') {
            body.innerHTML = this.options.content;
        } else if (this.options.content instanceof HTMLElement) {
            body.appendChild(this.options.content);
        }
        
        // Footer (optional)
        let footer = null;
        if (this.options.footer) {
            footer = document.createElement('div');
            footer.className = 'modal-footer';
            if (typeof this.options.footer === 'string') {
                footer.innerHTML = this.options.footer;
            } else if (this.options.footer instanceof HTMLElement) {
                footer.appendChild(this.options.footer);
            }
        }
        
        // Assemble modal
        this.modal.appendChild(header);
        this.modal.appendChild(body);
        if (footer) {
            this.modal.appendChild(footer);
        }
        
        this.overlay.appendChild(this.modal);
        document.body.appendChild(this.overlay);
        
        // Event listeners
        const closeBtn = header.querySelector('.modal-close');
        closeBtn.addEventListener('click', () => this.close());
        
        if (this.options.closeOnOverlayClick) {
            this.overlay.addEventListener('click', (e) => {
                if (e.target === this.overlay) {
                    this.close();
                }
            });
        }
        
        // Escape key to close
        this.escapeHandler = (e) => {
            if (e.key === 'Escape') {
                this.close();
            }
        };
        document.addEventListener('keydown', this.escapeHandler);
        
        return this;
    }
    
    close() {
        if (this.escapeHandler) {
            document.removeEventListener('keydown', this.escapeHandler);
        }
        
        if (this.overlay && this.overlay.parentNode) {
            this.overlay.style.opacity = '0';
            setTimeout(() => {
                if (this.overlay && this.overlay.parentNode) {
                    this.overlay.parentNode.removeChild(this.overlay);
                }
            }, 200);
        }
        
        if (this.options.onClose) {
            this.options.onClose();
        }
    }
    
    getBody() {
        return this.modal?.querySelector('.modal-body');
    }
    
    getFooter() {
        return this.modal?.querySelector('.modal-footer');
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Helper function to create confirmation modal
export function confirmModal(message, title = 'Confirm', options = {}) {
    return new Promise((resolve) => {
        const footer = document.createElement('div');
        footer.style.display = 'flex';
        footer.style.gap = 'var(--spacing-sm)';
        
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.textContent = options.cancelText || 'Cancel';
        
        const confirmBtn = document.createElement('button');
        confirmBtn.className = `btn ${options.confirmClass || 'btn-primary'}`;
        confirmBtn.textContent = options.confirmText || 'Confirm';
        
        footer.appendChild(cancelBtn);
        footer.appendChild(confirmBtn);
        
        const modal = new Modal({
            title,
            content: `<p>${message}</p>`,
            footer,
            size: 'sm',
            onClose: () => resolve(false)
        });
        
        modal.open();
        
        cancelBtn.addEventListener('click', () => {
            modal.close();
            resolve(false);
        });
        
        confirmBtn.addEventListener('click', () => {
            modal.close();
            resolve(true);
        });
    });
}

