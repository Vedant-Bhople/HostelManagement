/**
 * Hostel Management System - Student Module JavaScript
 * Handles Student Login, Registration, Dashboard, Application Form, Document Upload, Merit List & Allotments.
 */

// =========================================================================
// 1. STUDENT LOGIN & REGISTRATION
// =========================================================================

function initStudentLogin() {
  $('#form-student-login').on('submit', function (e) {
    e.preventDefault();
    const email = $('#login-email').val().trim();
    const password = $('#login-password').val().trim();

    if (!email || !password) {
      showAlert('#alert-container', 'Please enter both email and password.', 'warning');
      return;
    }

    const btn = $('#btn-login');
    btn.prop('disabled', true).text('Signing in...');

    API.postParams('/users/login', { email, password })
      .done(function (user) {
        if (user.role && user.role.toUpperCase() !== 'STUDENT') {
          showAlert('#alert-container', 'This account is not registered as a Student. Please use the Admin portal.', 'danger');
          btn.prop('disabled', false).text('Sign In');
          return;
        }
        setUserSession(user);
        window.location.href = 'dashboard.html';
      })
      .fail(function (xhr) {
        const errorMsg = xhr.responseText || 'Login failed. Invalid credentials.';
        showAlert('#alert-container', errorMsg, 'danger');
        btn.prop('disabled', false).text('Sign In');
      });
  });
}

function initStudentRegister() {
  $('#form-student-register').on('submit', function (e) {
    e.preventDefault();
    const name = $('#reg-name').val().trim();
    const email = $('#reg-email').val().trim();
    const password = $('#reg-password').val().trim();
    const confirmPassword = $('#reg-confirm-password').val().trim();

    if (!name || !email || !password) {
      showAlert('#alert-container', 'Please fill in all required fields.', 'warning');
      return;
    }

    if (password !== confirmPassword) {
      showAlert('#alert-container', 'Passwords do not match.', 'warning');
      return;
    }

    const btn = $('#btn-register');
    btn.prop('disabled', true).text('Creating Account...');

    const payload = {
      name: name,
      email: email,
      password: password,
      role: 'STUDENT'
    };

    API.post('/users/register', payload)
      .done(function (user) {
        setUserSession(user);
        alert('Registration successful! Redirecting to dashboard...');
        window.location.href = 'dashboard.html';
      })
      .fail(function (xhr) {
        const errorMsg = xhr.responseText || 'Registration failed. Email may already be in use.';
        showAlert('#alert-container', errorMsg, 'danger');
        btn.prop('disabled', false).text('Register Account');
      });
  });
}

// =========================================================================
// 2. STUDENT DASHBOARD
// =========================================================================

function initStudentDashboard() {
  const user = requireAuth('STUDENT');
  if (!user) return;

  $('#student-name-display').text(user.name);
  $('#student-email-display').text(user.email);

  API.get(`/student/dashboard/${user.id}`)
    .done(function (data) {
      renderStudentDashboard(data);
    })
    .fail(function () {
      // Fallback: check applications directly
      API.get(`/applications/student/${user.id}`)
        .done(function (apps) {
          const app = (apps && apps.length > 0) ? apps[apps.length - 1] : null;
          renderStudentDashboard({ application: app, documents: [], allotments: [] });
        })
        .fail(function () {
          renderStudentDashboard({ application: null, documents: [], allotments: [] });
        });
    });
}

function renderStudentDashboard(data) {
  const app = data.application;
  const docs = data.documents || [];
  const allotments = data.allotments || [];

  // Application Status Card
  if (app) {
    const status = app.status || 'PENDING';
    const pillClass = status === 'APPROVED' ? 'status-approved' : (status === 'REJECTED' ? 'status-rejected' : 'status-pending');
    $('#dash-app-status').html(`<span class="status-pill ${pillClass}">${status}</span>`);
    $('#dash-enrollment').text(app.enrollmentNumber || '--');
    $('#dash-branch-year').text(`${app.branch || '--'} (Year ${app.year || '--'})`);
    $('#dash-aggregate').text(app.aggregate ? `${app.aggregate.toFixed(2)}%` : '--');
    $('#dash-merit-rank').text(app.meritRank ? `#${app.meritRank}` : 'Pending');

    if (status === 'REJECTED') {
      $('#dash-rejection-alert').removeClass('hidden').html(`
        <div class="alert alert-danger">
          <strong>Application Rejected:</strong> ${app.rejectionReason || 'Please contact the hostel administrator for details.'}
        </div>
      `);
    } else if (status === 'APPROVED') {
      $('#dash-approved-alert').removeClass('hidden');
    }
  } else {
    $('#dash-app-status').html('<span class="status-pill status-open">Not Submitted</span>');
    $('#dash-no-app-alert').removeClass('hidden');
  }

  // Documents Summary
  const verifiedDocs = docs.filter(d => d.verificationStatus === 'VERIFIED').length;
  $('#dash-doc-count').text(`${docs.length} Uploaded (${verifiedDocs} Verified)`);

  // Allotment Summary
  if (allotments.length > 0) {
    const latestAllot = allotments[allotments.length - 1];
    const allotStatus = latestAllot.allotmentStatus || 'PENDING';
    const pillClass = allotStatus === 'ACCEPTED' ? 'status-accepted' : (allotStatus === 'ALLOTTED' ? 'status-allotted' : (allotStatus === 'REJECTED' ? 'status-rejected' : 'status-waiting'));
    $('#dash-allotment-status').html(`<span class="status-pill ${pillClass}">${allotStatus}</span>`);
    $('#dash-seat-number').text(latestAllot.seatNumber || 'Pending Allocation');

    if (allotStatus === 'ALLOTTED') {
      $('#dash-allotment-action-alert').removeClass('hidden');
    }
  } else {
    $('#dash-allotment-status').html('<span class="status-pill status-open">No Allotment</span>');
    $('#dash-seat-number').text('N/A');
  }
}

// =========================================================================
// 3. APPLICATION FORM & LIVE CALCULATIONS
// =========================================================================

function initStudentApplication() {
  const user = requireAuth('STUDENT');
  if (!user) return;

  // Pre-fill name
  $('#app-fullname').val(user.name);

  // Setup calculation listeners
  setupLiveMarkCalculation();

  // Category change listener
  $('#app-category').on('change', function () {
    if ($(this).val() === 'OTHER') {
      $('#other-category-group').removeClass('hidden');
    } else {
      $('#other-category-group').addClass('hidden');
    }
  });

  // ATKT change listener
  $('#app-atkt-status').on('change', function () {
    if ($(this).val() === 'YES') {
      $('#atkt-details-group').removeClass('hidden');
    } else {
      $('#atkt-details-group').addClass('hidden');
    }
  });

  // Load existing application if any
  API.get(`/applications/student/${user.id}`)
    .done(function (apps) {
      if (apps && apps.length > 0) {
        const app = apps[apps.length - 1];
        populateApplicationForm(app);
        $('#btn-submit-application').text('Update Application');
        $('#form-info-alert').removeClass('hidden').text(`Editing existing Application #${app.id} (Status: ${app.status || 'PENDING'})`);
      }
    });

  // Form submit
  $('#form-hostel-application').on('submit', function (e) {
    e.preventDefault();

    const payload = {
      fullName: $('#app-fullname').val().trim(),
      dateOfBirth: $('#app-dob').val(),
      gender: $('#app-gender').val(),
      mobileNumber: $('#app-mobile').val().trim(),
      address: $('#app-address').val().trim(),
      enrollmentNumber: $('#app-enrollment').val().trim(),
      collegeName: $('#app-college').val().trim(),
      branch: $('#app-branch').val(),
      year: $('#app-year').val(),
      admissionYear: $('#app-admission-year').val().trim(),
      category: $('#app-category').val(),
      otherCategory: $('#app-other-category').val() ? $('#app-other-category').val().trim() : null,
      sem1Obtained: parseFloat($('#app-sem1-obtained').val()) || null,
      sem1Total: parseFloat($('#app-sem1-total').val()) || null,
      sem2Obtained: parseFloat($('#app-sem2-obtained').val()) || null,
      sem2Total: parseFloat($('#app-sem2-total').val()) || null,
      atktStatus: $('#app-atkt-status').val(),
      atktSubjects: parseInt($('#app-atkt-subjects').val()) || 0,
      atktSubjectDetails: $('#app-atkt-details').val() ? $('#app-atkt-details').val().trim() : null
    };

    if (!payload.fullName || !payload.gender || !payload.branch || !payload.year || !payload.enrollmentNumber) {
      showAlert('#alert-container', 'Please complete all required fields.', 'warning');
      return;
    }

    const btn = $('#btn-submit-application');
    btn.prop('disabled', true).text('Submitting...');

    API.post(`/applications/create/${user.id}`, payload)
      .done(function () {
        alert('Application submitted successfully! Redirecting to Document Upload...');
        window.location.href = 'documents.html';
      })
      .fail(function (xhr) {
        const errorMsg = xhr.responseText || 'Submission failed. Please check form inputs.';
        showAlert('#alert-container', errorMsg, 'danger');
        btn.prop('disabled', false).text('Submit Application');
      });
  });
}

function setupLiveMarkCalculation() {
  function compute() {
    const o1 = parseFloat($('#app-sem1-obtained').val());
    const t1 = parseFloat($('#app-sem1-total').val());
    let p1 = null;
    if (!isNaN(o1) && !isNaN(t1) && t1 > 0) {
      p1 = (o1 / t1) * 100;
      $('#app-sem1-percentage').val(p1.toFixed(2) + '%');
    } else {
      $('#app-sem1-percentage').val('');
    }

    const o2 = parseFloat($('#app-sem2-obtained').val());
    const t2 = parseFloat($('#app-sem2-total').val());
    let p2 = null;
    if (!isNaN(o2) && !isNaN(t2) && t2 > 0) {
      p2 = (o2 / t2) * 100;
      $('#app-sem2-percentage').val(p2.toFixed(2) + '%');
    } else {
      $('#app-sem2-percentage').val('');
    }

    if (p1 !== null && p2 !== null) {
      const agg = (p1 + p2) / 2;
      $('#app-aggregate').val(agg.toFixed(2) + '%');
    } else if (p1 !== null) {
      $('#app-aggregate').val(p1.toFixed(2) + '%');
    } else if (p2 !== null) {
      $('#app-aggregate').val(p2.toFixed(2) + '%');
    } else {
      $('#app-aggregate').val('');
    }
  }

  $('#app-sem1-obtained, #app-sem1-total, #app-sem2-obtained, #app-sem2-total').on('input', compute);
}

function populateApplicationForm(app) {
  $('#app-fullname').val(app.fullName || '');
  $('#app-dob').val(app.dateOfBirth || '');
  $('#app-gender').val(app.gender || 'BOYS');
  $('#app-mobile').val(app.mobileNumber || '');
  $('#app-address').val(app.address || '');
  $('#app-enrollment').val(app.enrollmentNumber || '');
  $('#app-college').val(app.collegeName || '');
  $('#app-branch').val(app.branch || 'COMPUTER');
  $('#app-year').val(app.year || '1');
  $('#app-admission-year').val(app.admissionYear || '2026-2027');
  $('#app-category').val(app.category || 'OPEN');
  if (app.otherCategory) {
    $('#app-other-category').val(app.otherCategory);
    $('#other-category-group').removeClass('hidden');
  }
  $('#app-sem1-obtained').val(app.sem1Obtained || '');
  $('#app-sem1-total').val(app.sem1Total || '');
  $('#app-sem1-percentage').val(app.sem1Percentage ? app.sem1Percentage.toFixed(2) + '%' : '');
  $('#app-sem2-obtained').val(app.sem2Obtained || '');
  $('#app-sem2-total').val(app.sem2Total || '');
  $('#app-sem2-percentage').val(app.sem2Percentage ? app.sem2Percentage.toFixed(2) + '%' : '');
  $('#app-aggregate').val(app.aggregate ? app.aggregate.toFixed(2) + '%' : '');
  $('#app-atkt-status').val(app.atktStatus || 'NO');
  if (app.atktStatus === 'YES') {
    $('#app-atkt-subjects').val(app.atktSubjects || 0);
    $('#app-atkt-details').val(app.atktSubjectDetails || '');
    $('#atkt-details-group').removeClass('hidden');
  }
}

// =========================================================================
// 4. DOCUMENT UPLOAD MODULE
// =========================================================================

function initStudentDocuments() {
  const user = requireAuth('STUDENT');
  if (!user) return;

  API.get(`/applications/student/${user.id}`)
    .done(function (apps) {
      if (!apps || apps.length === 0) {
        $('#documents-container').html(`
          <div class="empty-box">
            <span class="glyphicon glyphicon-file"></span>
            <h4>No Application Found</h4>
            <p>Please submit your Hostel Application form before uploading verification documents.</p>
            <a href="application.html" class="btn btn-primary-custom mt-3">Fill Application Form</a>
          </div>
        `);
        return;
      }

      const app = apps[apps.length - 1];
      loadUploadedDocuments(app.id);
      setupDocumentUploadHandlers(app.id);
    })
    .fail(function () {
      showAlert('#alert-container', 'Failed to retrieve application details.', 'danger');
    });
}

function loadUploadedDocuments(applicationId) {
  API.get(`/documents/application/${applicationId}`)
    .done(function (docs) {
      renderDocumentsList(docs || []);
    })
    .fail(function () {
      renderDocumentsList([]);
    });
}

function renderDocumentsList(docs) {
  const docTypes = [
    { type: 'PASSPORT_PHOTO', label: '1. Passport Size Photo', desc: 'Recent clear photograph (JPG, PNG)' },
    { type: 'SEMESTER_MARKSHEET', label: '2. Previous Marksheet', desc: 'Grade card of qualifying examination (PDF, JPG, PNG)' },
    { type: 'ADMISSION_RECEIPT', label: '3. College Fee Receipt', desc: 'Proof of college admission fee payment' },
    { type: 'CAP_LETTER', label: '4. CAP Allotment / Caste Certificate', desc: 'CAP confirmation letter or reservation proof' },
    { type: 'OTHER', label: '5. Other Document', desc: 'Any supplementary undertaking or certificate' }
  ];

  let html = '';
  docTypes.forEach(dt => {
    const uploaded = docs.find(d => d.documentType === dt.type);
    const status = uploaded ? (uploaded.verificationStatus || 'PENDING') : 'NOT_UPLOADED';
    const pillClass = status === 'VERIFIED' ? 'status-verified' : (status === 'REJECTED' ? 'status-rejected' : (status === 'PENDING' ? 'status-pending' : 'status-open'));

    html += `
      <div class="panel panel-custom mb-3">
        <div class="panel-body">
          <div class="row">
            <div class="col-md-5">
              <h4 style="margin: 0 0 5px 0; font-weight: 700; color: #1e3a8a;">${dt.label}</h4>
              <p class="text-muted small" style="margin: 0;">${dt.desc}</p>
            </div>
            <div class="col-md-3 text-center" style="padding-top: 5px;">
              <span class="status-pill ${pillClass}">${status.replace('_', ' ')}</span>
            </div>
            <div class="col-md-4 text-right">
              ${uploaded ? `
                <div class="btn-group">
                  <a href="${API.getViewUrl(uploaded.id)}" target="_blank" class="btn btn-sm btn-default">
                    <span class="glyphicon glyphicon-eye-open"></span> View
                  </a>
                  <button type="button" class="btn btn-sm btn-default btn-reupload" data-type="${dt.type}">
                    <span class="glyphicon glyphicon-refresh"></span> Re-upload
                  </button>
                </div>
              ` : `
                <div class="input-group">
                  <input type="file" id="file-${dt.type}" class="form-control input-sm">
                  <span class="input-group-btn">
                    <button class="btn btn-primary-custom btn-sm btn-upload-doc" data-type="${dt.type}" type="button">Upload</button>
                  </span>
                </div>
              `}
            </div>
          </div>
        </div>
      </div>
    `;
  });

  $('#documents-list-panel').html(html);
}

function setupDocumentUploadHandlers(applicationId) {
  // Handle new upload
  $(document).on('click', '.btn-upload-doc', function () {
    const docType = $(this).data('type');
    const fileInput = $(`#file-${docType}`)[0];
    if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
      alert('Please select a file to upload.');
      return;
    }
    uploadFile(applicationId, docType, fileInput.files[0]);
  });

  // Handle re-upload trigger
  $(document).on('click', '.btn-reupload', function () {
    const docType = $(this).data('type');
    const fileInput = $('<input type="file" style="display:none">');
    fileInput.on('change', function () {
      if (this.files && this.files.length > 0) {
        uploadFile(applicationId, docType, this.files[0]);
      }
    });
    fileInput.trigger('click');
  });
}

function uploadFile(applicationId, documentType, file) {
  const formData = new FormData();
  formData.append('documentType', documentType);
  formData.append('file', file);

  showAlert('#alert-container', `Uploading ${file.name}...`, 'info');

  API.upload(`/documents/upload/${applicationId}`, formData)
    .done(function () {
      showAlert('#alert-container', `${file.name} uploaded successfully!`, 'success');
      loadUploadedDocuments(applicationId);
    })
    .fail(function (xhr) {
      const errorMsg = xhr.responseText || 'File upload failed.';
      showAlert('#alert-container', errorMsg, 'danger');
    });
}

// =========================================================================
// 5. PUBLISHED MERIT LIST VIEWER
// =========================================================================

function initStudentMeritList() {
  $('#form-merit-search').on('submit', function (e) {
    e.preventDefault();
    const gender = $('#merit-gender').val();
    const branch = $('#merit-branch').val();
    const year = $('#merit-year').val();

    $('#merit-results-container').html('<div class="text-center py-4"><p>Loading published merit rankings...</p></div>');

    API.get(`/merit/published?gender=${encodeURIComponent(gender)}&branch=${encodeURIComponent(branch)}&year=${encodeURIComponent(year)}`)
      .done(function (list) {
        if (!list || list.length === 0) {
          $('#merit-results-container').html(`
            <div class="empty-box">
              <span class="glyphicon glyphicon-list-alt"></span>
              <h4>No Published Merit List Available</h4>
              <p>The merit list for ${gender} - ${branch} - Year ${year} has not been published yet. Please check back later.</p>
            </div>
          `);
          return;
        }

        const user = getUserSession();
        let html = `
          <div class="table-responsive">
            <table class="table table-custom table-hover">
              <thead>
                <tr>
                  <th>Rank</th>
                  <th>Student Name</th>
                  <th>Enrollment No</th>
                  <th>Branch / Year</th>
                  <th>Category</th>
                  <th>Merit Quota</th>
                  <th>Aggregate</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
        `;

        list.forEach(item => {
          const isMe = user && (item.studentName === user.name);
          const statusClass = item.meritStatus === 'SELECTED' ? 'status-selected' : 'status-waiting';

          html += `
            <tr style="${isMe ? 'background-color: #eff6ff; font-weight: bold;' : ''}">
              <td><span class="label label-primary">#${item.meritRank}</span></td>
              <td>${item.studentName || '--'} ${isMe ? '<span class="label label-info">You</span>' : ''}</td>
              <td>${item.enrollmentNo || '--'}</td>
              <td>${item.branch} (Y${item.year})</td>
              <td>${item.category || '--'}</td>
              <td><span class="status-pill status-open">${item.meritCategory || '--'}</span></td>
              <td>${item.aggregate ? item.aggregate.toFixed(2) + '%' : '--'}</td>
              <td><span class="status-pill ${statusClass}">${item.meritStatus || 'WAITING'}</span></td>
            </tr>
          `;
        });

        html += '</tbody></table></div>';
        $('#merit-results-container').html(html);
      })
      .fail(function (xhr) {
        const errorMsg = xhr.responseText || 'No published merit list found.';
        $('#merit-results-container').html(`<div class="alert alert-warning">${errorMsg}</div>`);
      });
  });
}

// =========================================================================
// 6. STUDENT ALLOTMENT & CONFIRMATION
// =========================================================================

function initStudentAllotment() {
  const user = requireAuth('STUDENT');
  if (!user) return;

  loadStudentAllotment(user.id);
}

function loadStudentAllotment(userId) {
  API.get(`/allotment/student/${userId}`)
    .done(function (allotments) {
      if (!allotments || allotments.length === 0) {
        $('#allotment-content').html(`
          <div class="empty-box">
            <span class="glyphicon glyphicon-bed"></span>
            <h4>No Seat Allotment Record Found</h4>
            <p>You have not been allotted a seat yet. Allotments are released after the official Merit List is published.</p>
          </div>
        `);
        return;
      }

      let html = '';
      allotments.forEach(allot => {
        const status = allot.allotmentStatus || 'PENDING';
        const isAllotted = status === 'ALLOTTED';
        const isAccepted = status === 'ACCEPTED';
        const isRejected = status === 'REJECTED';
        const isWaiting = status === 'WAITING';

        const borderClass = isAccepted ? '#10b981' : (isAllotted ? '#f59e0b' : (isRejected ? '#ef4444' : '#06b6d4'));

        html += `
          <div class="panel panel-custom" style="border-top: 4px solid ${borderClass};">
            <div class="panel-heading">
              <div class="row">
                <div class="col-xs-6">
                  <h3 class="panel-title">Hostel Seat Allotment #${allot.id}</h3>
                </div>
                <div class="col-xs-6 text-right">
                  <span class="status-pill ${isAccepted ? 'status-accepted' : (isAllotted ? 'status-allotted' : (isRejected ? 'status-rejected' : 'status-waiting'))}">
                    ${status}
                  </span>
                </div>
              </div>
            </div>
            <div class="panel-body">
              <div class="text-center" style="margin-bottom: 25px;">
                <h1 style="color: #1e3a8a; font-weight: 800; margin: 10px 0 5px 0;">${allot.seatNumber || 'Pending Allocation'}</h1>
                <p class="text-muted" style="font-weight: 600;">Allocated Seat Number</p>
              </div>

              <div class="row">
                <div class="col-md-6">
                  <table class="table table-bordered">
                    <tr><th width="45%">Hostel Type:</th><td>${allot.hostelType || '--'} (${allot.gender || '--'})</td></tr>
                    <tr><th>Branch & Class:</th><td>${allot.branch} (Year ${allot.year})</td></tr>
                    <tr><th>Merit Rank:</th><td>#${allot.meritRank || '--'}</td></tr>
                  </table>
                </div>
                <div class="col-md-6">
                  <table class="table table-bordered">
                    <tr><th width="45%">Category:</th><td>${allot.category || '--'}</td></tr>
                    <tr><th>Allotted Quota:</th><td><span class="label label-default">${allot.allotmentCategory || 'OPEN'}</span></td></tr>
                    <tr><th>Academic Aggregate:</th><td>${allot.aggregate ? allot.aggregate.toFixed(2) + '%' : '--'}</td></tr>
                  </table>
                </div>
              </div>

              ${isAllotted ? `
                <div class="alert alert-warning text-center">
                  <strong>Action Required:</strong> A seat has been reserved for you. Please accept to confirm your admission or reject to surrender the seat.
                </div>
                <div class="text-center" style="margin-top: 20px;">
                  <button class="btn btn-success-custom btn-lg btn-accept-seat" data-id="${allot.id}" style="margin-right: 15px;">
                    <span class="glyphicon glyphicon-ok"></span> Accept Seat
                  </button>
                  <button class="btn btn-danger-custom btn-lg btn-reject-seat" data-id="${allot.id}">
                    <span class="glyphicon glyphicon-remove"></span> Reject / Surrender
                  </button>
                </div>
              ` : ''}

              ${isAccepted ? `
                <div class="alert alert-success text-center">
                  <strong>Seat Confirmed:</strong> You have accepted this hostel seat allotment. Please visit the hostel warden's office with your original verification documents.
                </div>
              ` : ''}

              ${isRejected ? `
                <div class="alert alert-danger text-center">
                  You have rejected/surrendered this hostel seat.
                </div>
              ` : ''}

              ${isWaiting ? `
                <div class="alert alert-info text-center">
                  You are currently on the waiting list. If an allotted seat becomes vacant, it will be allocated to waiting candidates in order of merit.
                </div>
              ` : ''}
            </div>
          </div>
        `;
      });

      $('#allotment-content').html(html);

      // Bind Accept / Reject actions
      $('.btn-accept-seat').on('click', function () {
        const id = $(this).data('id');
        if (confirm('Are you sure you want to accept and confirm this hostel seat?')) {
          API.put(`/allotment/${id}/accept`)
            .done(function () {
              alert('Seat accepted successfully!');
              loadStudentAllotment(userId);
            })
            .fail(function (xhr) {
              alert(xhr.responseText || 'Failed to accept seat.');
            });
        }
      });

      $('.btn-reject-seat').on('click', function () {
        const id = $(this).data('id');
        if (confirm('Warning: Rejecting this seat will release it to waiting list candidates. Are you sure you want to surrender this seat?')) {
          API.put(`/allotment/${id}/reject`)
            .done(function () {
              alert('Seat surrendered.');
              loadStudentAllotment(userId);
            })
            .fail(function (xhr) {
              alert(xhr.responseText || 'Failed to reject seat.');
            });
        }
      });
    })
    .fail(function () {
      $('#allotment-content').html('<div class="alert alert-danger">Failed to load seat allotment details.</div>');
    });
}

// =========================================================================
// 7. STUDENT PROFILE MODULE
// =========================================================================

function initStudentProfile() {
  const user = requireAuth('STUDENT');
  if (!user) return;

  $('#profile-user-name').text(user.name);
  $('#profile-user-email').text(user.email);
  $('#profile-user-id').text(`#${user.id}`);
  $('#profile-user-role').text(user.role || 'STUDENT');

  API.get(`/applications/student/${user.id}`)
    .done(function (apps) {
      if (apps && apps.length > 0) {
        const app = apps[apps.length - 1];
        $('#profile-app-id').text(`#${app.id}`);
        $('#profile-app-status').text(app.status || 'PENDING');
        $('#profile-enrollment').text(app.enrollmentNumber || '--');
        $('#profile-branch').text(app.branch || '--');
        $('#profile-year').text(`Year ${app.year || '--'}`);
        $('#profile-dob').text(app.dateOfBirth || '--');
        $('#profile-gender').text(app.gender || '--');
        $('#profile-mobile').text(app.mobileNumber || '--');
        $('#profile-address').text(app.address || '--');
        $('#profile-category').text(app.category || '--');
        $('#profile-aggregate').text(app.aggregate ? app.aggregate.toFixed(2) + '%' : '--');
      } else {
        $('#profile-application-section').html('<p class="text-muted">No hostel application submitted yet.</p>');
      }
    });
}
