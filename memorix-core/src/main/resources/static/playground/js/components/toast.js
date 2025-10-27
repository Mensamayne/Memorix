/**
 * Toast Notification System
 */

const TOAST_DURATION = 5000;
const TOAST_ICONS = {
    success: '✅',
    error: '❌',
    warning: '⚠️',
    info: 'ℹ️'
};

export class ToastManager {
    constructor() {
        this.container = document.getElementById('toast-container');
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.id = 'toast-container';
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    }
    
    show(message, type = 'info', title = null, duration = TOAST_DURATION) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        const icon = TOAST_ICONS[type] || TOAST_ICONS.info;
        
        toast.innerHTML = `
            <div class="toast-icon">${icon}</div>
            <div class="toast-content">
                ${title ? `<div class="toast-title">${this.escapeHtml(title)}</div>` : ''}
                <div class="toast-message">${this.escapeHtml(message)}</div>
            </div>
            <button class="toast-close" aria-label="Close">×</button>
        `;
        
        const closeBtn = toast.querySelector('.toast-close');
        closeBtn.addEventListener('click', () => this.remove(toast));
        
        this.container.appendChild(toast);
        
        // Auto remove after duration
        if (duration > 0) {
            setTimeout(() => this.remove(toast), duration);
        }
        
        return toast;
    }
    
    success(message, title = 'Success') {
        return this.show(message, 'success', title);
    }
    
    error(message, title = 'Error') {
        return this.show(message, 'error', title);
    }
    
    warning(message, title = 'Warning') {
        return this.show(message, 'warning', title);
    }
    
    info(message, title = null) {
        return this.show(message, 'info', title);
    }
    
    remove(toast) {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100px)';
        setTimeout(() => {
            if (toast.parentNode) {
                toast.parentNode.removeChild(toast);
            }
        }, 300);
    }
    
    clear() {
        this.container.innerHTML = '';
    }
    
    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
}

// Export singleton instance
export const toast = new ToastManager();

