/**
 * Hostel Management System - Admin Module JavaScript
 * Handles Admin Login, Dashboard Metrics, Application Review, Merit List Engine, Allotments, Students & Reports.
 */

// =========================================================================
// 1. ADMIN LOGIN & REGISTRATION
// =========================================================================

function initAdminLogin() {
  $('#form-admin-login').on('submit', function (e) {
    e.preventDefault();
    const email = $('#admin-email').val().trim();
    const password = $('#admin-password').val().trim();

    if (!email || !password) {
      showAlert('#alert-container', 'Please enter admin credentials.', 'warning');
      return;
    }

    const btn = $('#btn-admin-login');
    btn.prop('disabled', true).text('Verifying Admin...');

    API.postParams('/users/login', { email, password })
      .done(function (user) {
        if (!user.role || user.role.toUpperCase() !== 'ADMIN') {
          showAlert('#alert-container', 'Access Denied: This account does not possess Administrator privileges.', 'danger');
          btn.prop('disabled', false).text('Login to Admin Console');
          return;
        }
        setUserSession(user);
        window.location.href = 'dashboard.html';
      })
      .fail(function (xhr) {
        const errorMsg = xhr.responseText || 'Admin authentication failed.';
        showAlert('#alert-container', errorMsg, 'danger');
        btn.prop('disabled', false).text('Login to Admin Console');
      });
  });
}

function initAdminRegister() {
  $('#form-admin-register').on('submit', function (e) {
    e.preventDefault();
    const name = $('#admin-reg-name').val().trim();
    const email = $('#admin-reg-email').val().trim();
    const password = $('#admin-reg-password').val().trim();
    const confirmPassword = $('#admin-reg-confirm-password').val().trim();

    if (!name || !email || !password) {
      showAlert('#alert-container', 'Please fill in all required fields.', 'warning');
      return;
    }

    if (password.length < 6) {
      showAlert('#alert-container', 'Password must be at least 6 characters.', 'warning');
      return;
    }

    if (password !== confirmPassword) {
      showAlert('#alert-container', 'Passwords do not match.', 'warning');
      return;
    }

    const btn = $('#btn-admin-register');
    btn.prop('disabled', true).text('Creating Admin Account...');

    const payload = {
      name: name,
      email: email,
      password: password,
      role: 'ADMIN'
    };

    API.post('/users/register-admin', payload)
      .done(function (user) {
        setUserSession(user);
        alert('Admin account created successfully! Redirecting to Dashboard...');
        window.location.href = 'dashboard.html';
      })
      .fail(function (xhr) {
        const errorMsg = xhr.responseText || 'Admin registration failed. Email may already be in use.';
        showAlert('#alert-container', errorMsg, 'danger');
        btn.prop('disabled', false).text('Register as Administrator');
      });
  });
}

// =========================================================================
// 2. ADMIN DASHBOARD & METRICS
// =========================================================================

function initAdminDashboard() {
  const user = requireAuth('ADMIN');
  if (!user) return;

  $('#admin-name-display').text(user.name);

  loadAdminDashboardMetrics();

  $('#btn-generate-210-seats').on('click', function () {
    if (confirm('Initialize 210 Hostel Seats across all branches, classes, and quota categories?')) {
      const btn = $(this);
      btn.prop('disabled', true).text('Generating...');
      API.post('/seats/generate', {})
        .done(function (res) {
          alert(typeof res === 'string' ? res : 'Seats generated successfully!');
          loadAdminDashboardMetrics();
          btn.prop('disabled', false).text('Initialize 210 Seats');
        })
        .fail(function (xhr) {
          alert(xhr.responseText || 'Seat generation failed or seats already initialized.');
          btn.prop('disabled', false).text('Initialize 210 Seats');
        });
    }
  });
}

function loadAdminDashboardMetrics() {
  API.get('/admin/dashboard/summary')
    .done(function (summary) {
      $('#stat-total-apps').text(summary.totalApplications ?? 0);
      $('#stat-approved-apps').text(summary.approvedApplications ?? 0);
      $('#stat-pending-apps').text(summary.pendingApplications ?? 0);
      $('#stat-rejected-apps').text(summary.rejectedApplications ?? 0);
      $('#stat-total-allotments').text(summary.totalAllotments ?? 0);
      $('#stat-accepted-seats').text(summary.acceptedSeats ?? 0);
      $('#stat-allotted-seats').text(summary.allottedSeats ?? 0);
      $('#stat-total-merit').text(summary.totalMeritLists ?? 0);
      $('#stat-total-docs').text(summary.totalDocuments ?? 0);
    })
    .fail(function () {
      showAlert('#alert-container', 'Failed to retrieve dashboard analytics.', 'danger');
    });
}

// =========================================================================
// 3. APPLICATION MANAGEMENT
// =========================================================================

let cachedApplications = [];
let activeStatusFilter = 'ALL';

function initAdminApplications() {
  const user = requireAuth('ADMIN');
  if (!user) return;

  loadAdminApplicationsTable();

  // Status Filter click handlers
  $('.btn-app-filter').on('click', function () {
    $('.btn-app-filter').removeClass('active btn-primary').addClass('btn-default');
    $(this).addClass('active btn-primary').removeClass('btn-default');
    activeStatusFilter = $(this).data('status');
    filterAndRenderApplications();
  });

  // Search input handler
  $('#input-app-search').on('input', function () {
    filterAndRenderApplications();
  });
}

function loadAdminApplicationsTable() {
  $('#applications-tbody').html('<tr><td colspan="9" class="text-center py-4">Loading applications...</td></tr>');

  API.get('/applications')
    .done(function (apps) {
      cachedApplications = apps || [];
      filterAndRenderApplications();
    })
    .fail(function (xhr) {
      $('#applications-tbody').html(`<tr><td colspan="9" class="text-center text-danger py-4">${xhr.responseText || 'Failed to load applications'}</td></tr>`);
    });
}

function filterAndRenderApplications() {
  let list = cachedApplications;
  const search = ($('#input-app-search').val() || '').toLowerCase().trim();

  // Filter by status
  if (activeStatusFilter !== 'ALL') {
    list = list.filter(a => (a.status || 'PENDING').toUpperCase() === activeStatusFilter);
  }

  // Filter by search
  if (search) {
    list = list.filter(a =>
      (a.fullName || '').toLowerCase().includes(search) ||
      (a.enrollmentNumber || '').toLowerCase().includes(search) ||
      (a.branch || '').toLowerCase().includes(search) ||
      (a.category || '').toLowerCase().includes(search)
    );
  }

  if (list.length === 0) {
    $('#applications-tbody').html('<tr><td colspan="9" class="text-center py-4 text-muted">No applications matching criteria.</td></tr>');
    return;
  }

  let html = '';
  list.forEach(app => {
    const status = app.status || 'PENDING';
    const pillClass = status === 'APPROVED' ? 'status-approved' : (status === 'REJECTED' ? 'status-rejected' : 'status-pending');

    html += `
      <tr>
        <td><strong>#${app.id}</strong></td>
        <td>
          <strong>${app.fullName || 'Unnamed'}</strong><br>
          <small class="text-muted">${app.mobileNumber || ''}</small>
        </td>
        <td>${app.gender || '--'}</td>
        <td>${app.enrollmentNumber || '--'}</td>
        <td>${app.branch} (Y${app.year})</td>
        <td><span class="label label-default">${app.category || '--'}</span></td>
        <td><strong>${app.aggregate ? app.aggregate.toFixed(2) + '%' : '--'}</strong></td>
        <td><span class="status-pill ${pillClass}">${status}</span></td>
        <td>
          <div class="btn-group">
            <a href="application-view.html?id=${app.id}" class="btn btn-xs btn-default" title="Inspect Full Application">
              <span class="glyphicon glyphicon-eye-open"></span>
            </a>
            ${status === 'PENDING' ? `
              <button class="btn btn-xs btn-success btn-approve-app" data-id="${app.id}" title="Approve">
                <span class="glyphicon glyphicon-ok"></span>
              </button>
              <button class="btn btn-xs btn-danger btn-reject-app" data-id="${app.id}" title="Reject">
                <span class="glyphicon glyphicon-remove"></span>
              </button>
            ` : ''}
            ${status !== 'PENDING' ? `
              <button class="btn btn-xs btn-warning btn-reset-app" data-id="${app.id}" title="Reset to Pending">
                <span class="glyphicon glyphicon-repeat"></span>
              </button>
            ` : ''}
            <button class="btn btn-xs btn-default text-danger btn-delete-app" data-id="${app.id}" title="Delete">
              <span class="glyphicon glyphicon-trash"></span>
            </button>
          </div>
        </td>
      </tr>
    `;
  });

  $('#applications-tbody').html(html);

  // Bind Actions
  $('.btn-approve-app').on('click', function () {
    const id = $(this).data('id');
    if (confirm(`Approve Application #${id}?`)) {
      API.put(`/applications/approve/${id}`)
        .done(function () {
          alert(`Application #${id} Approved!`);
          loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
          alert(xhr.responseText || 'Approval failed.');
        });
    }
  });

  $('.btn-reject-app').on('click', function () {
    const id = $(this).data('id');
    const reason = prompt(`Enter rejection reason for Application #${id}:`);
    if (reason && reason.trim()) {
      API.put(`/applications/reject/${id}`, { reason: reason.trim() })
        .done(function () {
          alert(`Application #${id} Rejected.`);
          loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
          alert(xhr.responseText || 'Rejection failed.');
        });
    }
  });

  $('.btn-reset-app').on('click', function () {
    const id = $(this).data('id');
    if (confirm(`Reset Application #${id} to PENDING status?`)) {
      API.put(`/applications/reset/${id}`)
        .done(function () {
          alert(`Application #${id} reset to PENDING.`);
          loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
          alert(xhr.responseText || 'Reset failed.');
        });
    }
  });

  $('.btn-delete-app').on('click', function () {
    const id = $(this).data('id');
    if (confirm(`Permanently delete Application #${id}?`)) {
      API.delete(`/applications/${id}`)
        .done(function () {
          alert(`Application #${id} deleted.`);
          loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
          alert(xhr.responseText || 'Deletion failed.');
        });
    }
  });
}

// =========================================================================
// 4. APPLICATION VIEW & INSPECT
// =========================================================================

function initAdminApplicationView() {
  const user = requireAuth('ADMIN');
  if (!user) return;

  const urlParams = new URLSearchParams(window.location.search);
  const appId = urlParams.get('id');

  if (!appId) {
    showAlert('#alert-container', 'Application ID is missing in request URL.', 'danger');
    return;
  }

  loadFullApplicationDetails(appId);
}

function loadFullApplicationDetails(appId) {
  API.get(`/applications/${appId}`)
    .done(function (app) {
      renderApplicationInspectionView(app);
      loadApplicationDocumentsInspection(appId);
    })
    .fail(function (xhr) {
      $('#app-inspection-content').html(`<div class="alert alert-danger">${xhr.responseText || 'Failed to load application details.'}</div>`);
    });
}

function renderApplicationInspectionView(app) {
  $('#app-view-id').text(`#${app.id}`);
  $('#app-view-name').text(app.fullName);

  const status = app.status || 'PENDING';
  const pillClass = status === 'APPROVED' ? 'status-approved' : (status === 'REJECTED' ? 'status-rejected' : 'status-pending');
  $('#app-view-status').html(`<span class="status-pill ${pillClass}">${status}</span>`);

  // Personal Info
  $('#info-fullname').text(app.fullName || '--');
  $('#info-dob').text(app.dateOfBirth || '--');
  $('#info-gender').text(app.gender || '--');
  $('#info-mobile').text(app.mobileNumber || '--');
  $('#info-address').text(app.address || '--');
  $('#info-category').text(`${app.category || '--'} ${app.otherCategory ? `(${app.otherCategory})` : ''}`);

  // Academic Info
  $('#info-enrollment').text(app.enrollmentNumber || '--');
  $('#info-college').text(app.collegeName || '--');
  $('#info-branch').text(app.branch || '--');
  $('#info-year').text(`Year ${app.year || '--'}`);
  $('#info-admission-year').text(app.admissionYear || '--');
  $('#info-atkt').text(`${app.atktStatus || 'NO'} ${app.atktSubjects ? `(${app.atktSubjects} Subjects: ${app.atktSubjectDetails || ''})` : ''}`);

  // Scores
  $('#info-sem1').text(`${app.sem1Obtained || 0} / ${app.sem1Total || 0} (${app.sem1Percentage ? app.sem1Percentage.toFixed(2) + '%' : '--'})`);
  $('#info-sem2').text(`${app.sem2Obtained || 0} / ${app.sem2Total || 0} (${app.sem2Percentage ? app.sem2Percentage.toFixed(2) + '%' : '--'})`);
  $('#info-aggregate').text(app.aggregate ? app.aggregate.toFixed(2) + '%' : '--');
  $('#info-merit-rank').text(app.meritRank ? `#${app.meritRank}` : 'Pending calculation');

  if (app.rejectionReason) {
    $('#app-rejection-box').removeClass('hidden').html(`
      <div class="alert alert-danger">
        <strong>Rejection Reason:</strong> ${app.rejectionReason}
      </div>
    `);
  } else {
    $('#app-rejection-box').addClass('hidden');
  }

  // Setup Actions
  $('#btn-view-approve').off('click').on('click', function () {
    if (confirm('Approve this application?')) {
      API.put(`/applications/approve/${app.id}`)
        .done(function () {
          alert('Application approved successfully!');
          loadFullApplicationDetails(app.id);
        })
        .fail(function (xhr) { alert(xhr.responseText || 'Approval failed'); });
    }
  });

  $('#btn-view-reject').off('click').on('click', function () {
    const reason = prompt('Enter rejection reason:');
    if (reason && reason.trim()) {
      API.put(`/applications/reject/${app.id}`, { reason: reason.trim() })
        .done(function () {
          alert('Application rejected.');
          loadFullApplicationDetails(app.id);
        })
        .fail(function (xhr) { alert(xhr.responseText || 'Rejection failed'); });
    }
  });

  $('#btn-view-reset').off('click').on('click', function () {
    if (confirm('Reset application to PENDING status?')) {
      API.put(`/applications/reset/${app.id}`)
        .done(function () {
          alert('Application status reset to PENDING.');
          loadFullApplicationDetails(app.id);
        })
        .fail(function (xhr) { alert(xhr.responseText || 'Reset failed'); });
    }
  });
}

function loadApplicationDocumentsInspection(appId) {
  API.get(`/documents/application/${appId}`)
    .done(function (docs) {
      if (!docs || docs.length === 0) {
        $('#app-docs-panel').html('<p class="text-muted">No documents uploaded by student yet.</p>');
        return;
      }

      let html = '<div class="list-group">';
      docs.forEach(doc => {
        const vStatus = doc.verificationStatus || 'PENDING';
        const pillClass = vStatus === 'VERIFIED' ? 'status-verified' : (vStatus === 'REJECTED' ? 'status-rejected' : 'status-pending');

        html += `
          <div class="list-group-item">
            <div class="row">
              <div class="col-md-5">
                <h5 class="list-group-item-heading" style="color: #1e3a8a; font-weight: 700;">${doc.documentType}</h5>
                <p class="text-muted small" style="margin: 0;">File: ${doc.fileName || 'Attached file'}</p>
              </div>
              <div class="col-md-3 text-center" style="padding-top: 5px;">
                <span class="status-pill ${pillClass}">${vStatus}</span>
              </div>
              <div class="col-md-4 text-right">
                <a href="${API.getViewUrl(doc.id)}" target="_blank" class="btn btn-xs btn-default">
                  <span class="glyphicon glyphicon-download-alt"></span> View File
                </a>
                ${vStatus !== 'VERIFIED' ? `
                  <button class="btn btn-xs btn-success btn-verify-doc" data-id="${doc.id}">Verify</button>
                ` : ''}
                ${vStatus !== 'REJECTED' ? `
                  <button class="btn btn-xs btn-danger btn-reject-doc" data-id="${doc.id}">Reject</button>
                ` : ''}
              </div>
            </div>
          </div>
        `;
      });
      html += '</div>';

      $('#app-docs-panel').html(html);

      $('.btn-verify-doc').on('click', function () {
        const docId = $(this).data('id');
        API.put(`/documents/verify/${docId}`)
          .done(function () {
            alert('Document verified!');
            loadApplicationDocumentsInspection(appId);
          })
          .fail(function (xhr) { alert(xhr.responseText || 'Verification failed'); });
      });

      $('.btn-reject-doc').on('click', function () {
        const docId = $(this).data('id');
        const reason = prompt('Enter rejection reason for this document:');
        if (reason && reason.trim()) {
          API.put(`/documents/reject/${docId}`, { reason: reason.trim() })
            .done(function () {
              alert('Document marked as rejected.');
              loadApplicationDocumentsInspection(appId);
            })
            .fail(function (xhr) { alert(xhr.responseText || 'Rejection failed'); });
        }
      });
    });
}

// =========================================================================
// 5. MERIT LIST ENGINE
// =========================================================================

function initAdminMeritList() {
  const user = requireAuth('ADMIN');
  if (!user) return;

  $('#form-generate-merit').on('submit', function (e) {
    e.preventDefault();
    const gender = $('#merit-gender').val();
    const branch = $('#merit-branch').val();
    const year = $('#merit-year').val();

    $('#merit-results-box').html('<div class="text-center py-4"><p>Generating and sorting approved candidates...</p></div>');

    API.postParams('/merit/generate', { gender, branch, year })
      .done(function (list) {
        alert(`Merit list generated with ${list.length} applicants!`);
        renderAdminMeritListTable(list, gender, branch, year);
      })
      .fail(function (xhr) {
        const errorMsg = xhr.responseText || 'Failed to generate merit list. Make sure there are APPROVED applicants for this selection.';
        $('#merit-results-box').html(`<div class="alert alert-danger">${errorMsg}</div>`);
      });
  });
}

function renderAdminMeritListTable(list, gender, branch, year) {
  const isPublished = list && list.length > 0 && list[0].published;

  $('#merit-action-toolbar').removeClass('hidden');

  let html = `
    <div class="row mb-3">
      <div class="col-xs-6">
        <h4 style="margin: 0; font-weight: 700; color: #1e3a8a;">Merit List: ${gender} - ${branch} - Year ${year}</h4>
      </div>
      <div class="col-xs-6 text-right">
        <span class="status-pill ${isPublished ? 'status-approved' : 'status-pending'}">${isPublished ? 'PUBLISHED' : 'DRAFT / UNPUBLISHED'}</span>
      </div>
    </div>

    <div class="table-responsive">
      <table class="table table-custom table-hover">
        <thead>
          <tr>
            <th>Rank</th>
            <th>Student Name</th>
            <th>Enrollment No</th>
            <th>Actual Category</th>
            <th>Merit Quota</th>
            <th>Aggregate %</th>
            <th>ATKT</th>
            <th>Merit Status</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
  `;

  list.forEach(m => {
    html += `
      <tr>
        <td><strong>#${m.meritRank}</strong></td>
        <td><strong>${m.studentName || '--'}</strong></td>
        <td>${m.enrollmentNo || '--'}</td>
        <td>${m.category || '--'}</td>
        <td><span class="label label-default">${m.meritCategory || '--'}</span></td>
        <td><strong>${m.aggregate ? m.aggregate.toFixed(2) + '%' : '--'}</strong></td>
        <td>${m.atktStatus || 'NO'}</td>
        <td><span class="status-pill ${m.meritStatus === 'SELECTED' ? 'status-selected' : 'status-waiting'}">${m.meritStatus || 'WAITING'}</span></td>
        <td>
          <select class="form-control input-sm select-merit-status" data-id="${m.id}" style="width: 110px; display: inline-block;">
            <option value="SELECTED" ${m.meritStatus === 'SELECTED' ? 'selected' : ''}>SELECTED</option>
            <option value="WAITING" ${m.meritStatus === 'WAITING' ? 'selected' : ''}>WAITING</option>
          </select>
        </td>
      </tr>
    `;
  });

  html += '</tbody></table></div>';
  $('#merit-results-box').html(html);

  // Status updater handler
  $('.select-merit-status').on('change', function () {
    const id = $(this).data('id');
    const status = $(this).val();
    API.put(`/merit/status/${id}`, { status })
      .done(function () { alert(`Status updated to ${status}`); })
      .fail(function (xhr) { alert(xhr.responseText || 'Failed to update status'); });
  });

  // Publish / Unpublish / Delete handlers
  $('#btn-publish-merit').off('click').on('click', function () {
    API.put('/merit/publish', { gender, branch, year })
      .done(function (updatedList) {
        alert('Merit list PUBLISHED! Students can now view their rankings.');
        renderAdminMeritListTable(updatedList, gender, branch, year);
      })
      .fail(function (xhr) { alert(xhr.responseText || 'Publish failed'); });
  });

  $('#btn-unpublish-merit').off('click').on('click', function () {
    API.put('/merit/unpublish', { gender, branch, year })
      .done(function (updatedList) {
        alert('Merit list UNPUBLISHED.');
        renderAdminMeritListTable(updatedList, gender, branch, year);
      })
      .fail(function (xhr) { alert(xhr.responseText || 'Unpublish failed'); });
  });

  $('#btn-delete-merit').off('click').on('click', function () {
    if (confirm(`Delete merit list for ${gender} - ${branch} - Year ${year}?`)) {
      API.delete('/merit', { gender, branch, year })
        .done(function () {
          alert('Merit list deleted.');
          $('#merit-results-box').html('<div class="empty-box"><p>Merit list deleted.</p></div>');
          $('#merit-action-toolbar').addClass('hidden');
        })
        .fail(function (xhr) { alert(xhr.responseText || 'Delete failed'); });
    }
  });
}

// =========================================================================
// 6. SEAT ALLOTMENT & VACANCY ENGINE
// =========================================================================

function initAdminAllotment() {
  const user = requireAuth('ADMIN');
  if (!user) return;

  // Generate Allotment Form
  $('#form-generate-allotment').on('submit', function (e) {
    e.preventDefault();
    const gender = $('#allot-gender').val();
    const branch = $('#allot-branch').val();
    const year = $('#allot-year').val();

    $('#allotment-results-box').html('<div class="text-center py-4"><p>Executing quota-based seat allocation algorithm...</p></div>');

    API.postParams('/allotment/generate', { gender, branch, year })
      .done(function (allotments) {
        alert(`Seat Allotment completed for ${allotments.length} applicants!`);
        renderAdminAllotmentTable(allotments, gender, branch, year);
      })
      .fail(function (xhr) {
        const errorMsg = xhr.responseText || 'Allotment failed. Ensure the Merit List for this branch & year has been generated and PUBLISHED.';
        $('#allotment-results-box').html(`<div class="alert alert-danger">${errorMsg}</div>`);
      });
  });

  // Vacancy & Waiting List Form
  $('#form-check-vacancy').on('submit', function (e) {
    e.preventDefault();
    const gender = $('#vac-gender').val();
    const branch = $('#vac-branch').val();
    const year = $('#vac-year').val();

    loadVacancyAndWaiting(gender, branch, year);
  });

  // Allot Next Waiting Student
  $('#btn-allot-next-waiting').on('click', function () {
    const gender = $('#vac-gender').val();
    const branch = $('#vac-branch').val();
    const year = $('#vac-year').val();

    if (confirm(`Allocate available seat to next eligible waiting candidate for ${gender} - ${branch} - Year ${year}?`)) {
      API.put('/waiting-list/allot-next', { gender, branch, year })
        .done(function (allot) {
          alert(`Seat ${allot.seatNumber} allotted to ${allot.application?.fullName || 'student'}!`);
          loadVacancyAndWaiting(gender, branch, year);
        })
        .fail(function (xhr) {
          alert(xhr.responseText || 'No waiting candidates or no available seats.');
        });
    }
  });
}

function renderAdminAllotmentTable(list, gender, branch, year) {
  let html = `
    <h4 style="margin: 0 0 15px 0; font-weight: 700; color: #1e3a8a;">Allotment Results: ${gender} - ${branch} - Year ${year}</h4>
    <div class="table-responsive">
      <table class="table table-custom table-hover">
        <thead>
          <tr>
            <th>Merit Rank</th>
            <th>Student Name</th>
            <th>Assigned Seat</th>
            <th>Quota Category</th>
            <th>Actual Category</th>
            <th>Aggregate %</th>
            <th>Allotment Status</th>
          </tr>
        </thead>
        <tbody>
  `;

  list.forEach(a => {
    html += `
      <tr>
        <td><strong>#${a.meritRank}</strong></td>
        <td><strong>${a.application?.fullName || 'Student'}</strong></td>
        <td><span class="label label-primary" style="font-size: 13px;">${a.seatNumber || 'N/A'}</span></td>
        <td><span class="label label-default">${a.allotmentCategory || 'OPEN'}</span></td>
        <td>${a.category || '--'}</td>
        <td>${a.aggregate ? a.aggregate.toFixed(2) + '%' : '--'}</td>
        <td><span class="status-pill ${a.allotmentStatus === 'ACCEPTED' ? 'status-accepted' : (a.allotmentStatus === 'ALLOTTED' ? 'status-allotted' : 'status-waiting')}">${a.allotmentStatus}</span></td>
      </tr>
    `;
  });

  html += '</tbody></table></div>';
  $('#allotment-results-box').html(html);
}

function loadVacancyAndWaiting(gender, branch, year) {
  $('#vacancy-stats-box').html('<p>Loading vacancy data...</p>');
  $('#waiting-list-box').html('<p>Loading waiting list...</p>');

  API.get(`/vacancy?gender=${encodeURIComponent(gender)}&branch=${encodeURIComponent(branch)}&year=${encodeURIComponent(year)}`)
    .done(function (vac) {
      $('#vacancy-stats-box').html(`
        <div class="row text-center">
          <div class="col-md-3 col-xs-6">
            <div class="metric-card primary">
              <div class="metric-value">${vac.totalSeats || 0}</div>
              <div class="metric-label">Total Quota Seats</div>
            </div>
          </div>
          <div class="col-md-3 col-xs-6">
            <div class="metric-card warning">
              <div class="metric-value">${vac.allottedSeats || 0}</div>
              <div class="metric-label">Allotted Seats</div>
            </div>
          </div>
          <div class="col-md-3 col-xs-6">
            <div class="metric-card success">
              <div class="metric-value">${vac.acceptedSeats || 0}</div>
              <div class="metric-label">Accepted Seats</div>
            </div>
          </div>
          <div class="col-md-3 col-xs-6">
            <div class="metric-card info">
              <div class="metric-value" style="color: #06b6d4;">${vac.availableSeats || 0}</div>
              <div class="metric-label">Available / Vacant Seats</div>
            </div>
          </div>
        </div>
      `);
    });

  API.get(`/waiting-list?gender=${encodeURIComponent(gender)}&branch=${encodeURIComponent(branch)}&year=${encodeURIComponent(year)}`)
    .done(function (waiting) {
      if (!waiting || waiting.length === 0) {
        $('#waiting-list-box').html('<div class="empty-box"><p>No candidates currently on the waiting list.</p></div>');
        $('#allot-next-toolbar').addClass('hidden');
        return;
      }

      $('#allot-next-toolbar').removeClass('hidden');

      let html = `
        <h5 style="font-weight: 700; margin-bottom: 12px;">Waiting List Queue (${waiting.length} Students)</h5>
        <div class="table-responsive">
          <table class="table table-custom">
            <thead>
              <tr>
                <th>Queue Position</th>
                <th>Student Name</th>
                <th>Merit Rank</th>
                <th>Category</th>
                <th>Aggregate %</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
      `;

      waiting.forEach((w, index) => {
        html += `
          <tr>
            <td><strong>#${index + 1}</strong> in Queue</td>
            <td><strong>${w.application?.fullName || 'Student'}</strong></td>
            <td>#${w.meritRank}</td>
            <td>${w.category || '--'}</td>
            <td>${w.aggregate ? w.aggregate.toFixed(2) + '%' : '--'}</td>
            <td><span class="status-pill status-waiting">${w.allotmentStatus}</span></td>
          </tr>
        `;
      });

      html += '</tbody></table></div>';
      $('#waiting-list-box').html(html);
    });
}

// =========================================================================
// 7. STUDENTS DIRECTORY
// =========================================================================

function initAdminStudents() {
  const user = requireAuth('ADMIN');
  if (!user) return;

  loadStudentsDirectory();

  $('#filter-branch, #filter-year, #filter-gender, #filter-status, #filter-category').on('change', function () {
    filterStudentsDirectory();
  });

  $('#search-student').on('input', function () {
    filterStudentsDirectory();
  });

  // Modal Email Form Submission
  $('#form-send-student-email').on('submit', function (e) {
    e.preventDefault();
    const to = $('#email-to').val().trim();
    const subject = $('#email-subject').val().trim();
    const message = $('#email-message').val().trim();

    if (!to || !subject || !message) {
      showAlert('#modal-email-alert', 'Please fill in all email fields.', 'warning');
      return;
    }

    const btn = $('#btn-send-student-email');
    btn.prop('disabled', true).html('<span class="glyphicon glyphicon-refresh glyphicon-refresh-animate"></span> Sending...');

    API.post('/email/send', { to, subject, message })
      .done(function (res) {
        alert(typeof res === 'string' ? res : 'Email dispatched successfully!');
        $('#modal-send-email').modal('hide');
        $('#form-send-student-email')[0].reset();
        btn.prop('disabled', false).html('<span class="glyphicon glyphicon-send"></span> Dispatch Email');
      })
      .fail(function (xhr) {
        showAlert('#modal-email-alert', xhr.responseText || 'Failed to dispatch email.', 'danger');
        btn.prop('disabled', false).html('<span class="glyphicon glyphicon-send"></span> Dispatch Email');
      });
  });
}

let cachedStudents = [];

function loadStudentsDirectory() {
  API.get('/applications')
    .done(function (apps) {
      cachedStudents = apps || [];
      filterStudentsDirectory();
    });
}

function openStudentEmailModal(email, name) {
  $('#modal-email-alert').empty();
  $('#email-to').val(email || '');
  $('#email-subject').val(`Notice: Campus Hostel Admission (${name || 'Student'})`);
  $('#email-message').val(`Dear ${name || 'Student'},\n\nThis is an official communication regarding your hostel admission application.\n\nRegards,\nHostel Warden Office`);
  $('#modal-send-email').modal('show');
}

function filterStudentsDirectory() {
  let list = cachedStudents;

  const branch = $('#filter-branch').val();
  const year = $('#filter-year').val();
  const gender = $('#filter-gender').val();
  const status = $('#filter-status').val();
  const category = $('#filter-category').val();
  const search = ($('#search-student').val() || '').toLowerCase().trim();

  if (branch !== 'ALL') list = list.filter(s => s.branch === branch);
  if (year !== 'ALL') list = list.filter(s => s.year === year);
  if (gender !== 'ALL') list = list.filter(s => s.gender === gender);
  if (status !== 'ALL') list = list.filter(s => (s.status || 'PENDING') === status);
  if (category !== 'ALL') list = list.filter(s => s.category === category);

  if (search) {
    list = list.filter(s =>
      (s.fullName || '').toLowerCase().includes(search) ||
      (s.enrollmentNumber || '').toLowerCase().includes(search) ||
      (s.mobileNumber || '').toLowerCase().includes(search)
    );
  }

  if (list.length === 0) {
    $('#students-tbody').html('<tr><td colspan="9" class="text-center py-4 text-muted">No students matching criteria.</td></tr>');
    return;
  }

  let html = '';
  list.forEach(s => {
    const studentEmail = (s.user && s.user.email) ? s.user.email : '';
    const safeName = (s.fullName || s.enrollmentNumber || 'Student').replace(/'/g, "\\'");
    const safeEmail = studentEmail.replace(/'/g, "\\'");

    html += `
      <tr>
        <td><strong>#${s.id}</strong></td>
        <td><strong>${s.fullName || '--'}</strong></td>
        <td>${s.enrollmentNumber || '--'}</td>
        <td>${s.gender || '--'}</td>
        <td>${s.branch} (Y${s.year})</td>
        <td><span class="label label-default">${s.category || '--'}</span></td>
        <td>${s.aggregate ? s.aggregate.toFixed(2) + '%' : '--'}</td>
        <td><span class="status-pill ${s.status === 'APPROVED' ? 'status-approved' : (s.status === 'REJECTED' ? 'status-rejected' : 'status-pending')}">${s.status || 'PENDING'}</span></td>
        <td class="text-right">
          ${studentEmail ? `
            <button class="btn btn-default btn-xs" onclick="openStudentEmailModal('${safeEmail}', '${safeName}')" title="Send direct email to student">
              <span class="glyphicon glyphicon-envelope"></span> Email
            </button>
          ` : '<span class="text-muted small">No Email</span>'}
        </td>
      </tr>
    `;
  });

  $('#students-tbody').html(html);
}

// =========================================================================
// 8. ADMIN REPORTS & BREAKDOWNS
// =========================================================================

function initAdminReports() {
  const user = requireAuth('ADMIN');
  if (!user) return;

  API.get('/applications')
    .done(function (apps) {
      renderReportsData(apps || []);
    });
}

function renderReportsData(apps) {
  $('#report-total-apps').text(apps.length);

  // Status Distribution
  const approved = apps.filter(a => a.status === 'APPROVED').length;
  const pending = apps.filter(a => a.status === 'PENDING').length;
  const rejected = apps.filter(a => a.status === 'REJECTED').length;

  $('#rep-approved').text(approved);
  $('#rep-pending').text(pending);
  $('#rep-rejected').text(rejected);

  // Branch Distribution
  const branches = ['COMPUTER', 'MECHANICAL', 'CIVIL', 'ELECTRICAL', 'IT'];
  let branchHtml = '';
  branches.forEach(b => {
    const count = apps.filter(a => a.branch === b).length;
    const pct = apps.length > 0 ? ((count / apps.length) * 100).toFixed(1) : 0;
    branchHtml += `
      <tr>
        <th>${b}</th>
        <td><strong>${count}</strong></td>
        <td>
          <div class="progress" style="margin-bottom: 0;">
            <div class="progress-bar progress-bar-info" style="width: ${pct}%;">${pct}%</div>
          </div>
        </td>
      </tr>
    `;
  });
  $('#branch-breakdown-tbody').html(branchHtml);

  // Gender Distribution
  const boys = apps.filter(a => a.gender === 'BOYS').length;
  const girls = apps.filter(a => a.gender === 'GIRLS').length;
  $('#rep-boys').text(boys);
  $('#rep-girls').text(girls);

  // Year Distribution
  const y1 = apps.filter(a => a.year === '1').length;
  const y2 = apps.filter(a => a.year === '2').length;
  const y3 = apps.filter(a => a.year === '3').length;
  $('#rep-y1').text(y1);
  $('#rep-y2').text(y2);
  $('#rep-y3').text(y3);
}
