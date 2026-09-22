/**
 * Hostel Management System - Common JavaScript Utilities
 * Handles API Base URL, Authentication state, session storage, and common UI helpers.
 */

// API Base URL (Configured for Spring Boot port 8082)
const API_BASE = '/api';

/**
 * Get current user from sessionStorage / localStorage
 */
function getUserSession() {
  const session = sessionStorage.getItem('hostel_user') || localStorage.getItem('hostel_user');
  if (session) {
    try {
      return JSON.parse(session);
    } catch (e) {
      console.error('Failed to parse user session', e);
      return null;
    }
  }
  return null;
}

/**
 * Save user session
 */
function setUserSession(user) {
  sessionStorage.setItem('hostel_user', JSON.stringify(user));
  localStorage.setItem('hostel_user', JSON.stringify(user));
}

/**
 * Clear user session (Logout)
 */
function clearUserSession() {
  sessionStorage.removeItem('hostel_user');
  localStorage.removeItem('hostel_user');
}

/**
 * Logout user and redirect to login
 */
function logout(isStudent = true) {
  clearUserSession();
  if (isStudent) {
    window.location.href = (window.location.pathname.includes('/student/') || window.location.pathname.includes('/admin/')) 
      ? '../student/login.html' 
      : 'student/login.html';
  } else {
    window.location.href = (window.location.pathname.includes('/student/') || window.location.pathname.includes('/admin/')) 
      ? '../admin/login.html' 
      : 'admin/login.html';
  }
}

/**
 * Protect page route based on required role
 */
function requireAuth(requiredRole = 'STUDENT') {
  const user = getUserSession();
  if (!user) {
    const loginPath = requiredRole === 'ADMIN' ? '../admin/login.html' : '../student/login.html';
    window.location.href = loginPath;
    return null;
  }

  const role = (user.role || 'STUDENT').toUpperCase();
  if (requiredRole && role !== requiredRole.toUpperCase()) {
    alert('Access Denied: You do not have permission to view this page.');
    window.location.href = role === 'ADMIN' ? '../admin/dashboard.html' : '../student/dashboard.html';
    return null;
  }

  return user;
}

/**
 * Display Bootstrap 3 Alert message in container
 */
function showAlert(containerSelector, message, type = 'info') {
  const container = $(containerSelector);
  if (!container.length) return;

  const alertClass = `alert alert-${type} alert-dismissible`;
  const alertHtml = `
    <div class="${alertClass}" role="alert">
      <button type="button" class="close" data-dismiss="alert" aria-label="Close">
        <span aria-hidden="true">&times;</span>
      </button>
      <strong>${type === 'danger' ? 'Error: ' : (type === 'success' ? 'Success: ' : (type === 'warning' ? 'Notice: ' : 'Info: '))}</strong>
      ${message}
    </div>
  `;

  container.html(alertHtml);
}

/**
 * Initialize Navbar User Badge & Logout Handler
 */
function initNavUserBadge() {
  const user = getUserSession();
  if (user) {
    $('#nav-user-name').text(user.name || user.email);
    $('#nav-user-role').text(`[${(user.role || 'STUDENT').toUpperCase()}]`);
    $('#nav-user-section').removeClass('hidden');
    $('#nav-guest-section').addClass('hidden');
  } else {
    $('#nav-user-section').addClass('hidden');
    $('#nav-guest-section').removeClass('hidden');
  }

  $('#btn-nav-logout').on('click', function (e) {
    e.preventDefault();
    const user = getUserSession();
    logout(user && user.role === 'ADMIN' ? false : true);
  });
}

/**
 * Core AJAX Request Wrappers
 */
const API = {
  get: function (endpoint) {
    return $.ajax({
      url: `${API_BASE}${endpoint}`,
      method: 'GET',
      dataType: 'json'
    });
  },

  post: function (endpoint, data) {
    return $.ajax({
      url: `${API_BASE}${endpoint}`,
      method: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(data),
      dataType: 'json'
    });
  },

  postParams: function (endpoint, queryParams) {
    return $.ajax({
      url: `${API_BASE}${endpoint}?${$.param(queryParams)}`,
      method: 'POST',
      dataType: 'json'
    });
  },

  put: function (endpoint, queryParams = {}) {
    const url = Object.keys(queryParams).length ? `${API_BASE}${endpoint}?${$.param(queryParams)}` : `${API_BASE}${endpoint}`;
    return $.ajax({
      url: url,
      method: 'PUT',
      dataType: 'json'
    });
  },

  delete: function (endpoint, queryParams = {}) {
    const url = Object.keys(queryParams).length ? `${API_BASE}${endpoint}?${$.param(queryParams)}` : `${API_BASE}${endpoint}`;
    return $.ajax({
      url: url,
      method: 'DELETE'
    });
  },

  upload: function (endpoint, formData) {
    return $.ajax({
      url: `${API_BASE}${endpoint}`,
      method: 'POST',
      data: formData,
      processData: false,
      contentType: false,
      dataType: 'json'
    });
  },

  getViewUrl: function (documentId) {
    return `${API_BASE}/documents/view/${documentId}`;
  }
};

$(document).ready(function () {
  initNavUserBadge();
});
