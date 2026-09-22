/**
 * Hostel Management System - Admin Module JavaScript
 * Handles Admin Login, Dashboard, Applications, Merit Lists,
 * Allotments, Students and Reports.
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

        API.postParams('/users/login', {
            email: email,
            password: password
        })
        .done(function (user) {
            if (!user || !user.role || user.role.toUpperCase() !== 'ADMIN') {
                showAlert(
                    '#alert-container',
                    'Access Denied: This account does not possess Administrator privileges.',
                    'danger'
                );

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

// =========================================================================
// 2. ADMIN REGISTRATION
// =========================================================================

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
            const errorMsg =
                xhr.responseText ||
                'Admin registration failed. Email may already be in use.';

            showAlert('#alert-container', errorMsg, 'danger');

            btn.prop('disabled', false).text('Register as Administrator');
        });
    });
}

// =========================================================================
// 3. ADMIN DASHBOARD
// =========================================================================

function initAdminDashboard() {
    const user = requireAuth('ADMIN');
    if (!user) return;

    $('#admin-name-display').text(user.name || 'Administrator');

    loadAdminDashboardMetrics();

    $('#btn-generate-210-seats').on('click', function () {
        if (!confirm('Initialize 210 Hostel Seats across all branches, classes, and quota categories?')) {
            return;
        }

        const btn = $(this);

        btn.prop('disabled', true).text('Generating...');

        API.post('/seats/generate', {})
        .done(function (res) {
            alert(typeof res === 'string' ? res : 'Seats generated successfully!');

            loadAdminDashboardMetrics();

            btn.prop('disabled', false).text('Initialize 210 Seats');
        })
        .fail(function (xhr) {
            alert(
                xhr.responseText ||
                'Seat generation failed or seats already initialized.'
            );

            btn.prop('disabled', false).text('Initialize 210 Seats');
        });
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
        showAlert(
            '#alert-container',
            'Failed to retrieve dashboard analytics.',
            'danger'
        );
    });
}

// =========================================================================
// 4. APPLICATION MANAGEMENT
// =========================================================================

let cachedApplications = [];
let activeStatusFilter = 'ALL';

function initAdminApplications() {
    const user = requireAuth('ADMIN');
    if (!user) return;

    loadAdminApplicationsTable();

    $('.btn-app-filter').on('click', function () {
        $('.btn-app-filter')
            .removeClass('active btn-primary')
            .addClass('btn-default');

        $(this)
            .addClass('active btn-primary')
            .removeClass('btn-default');

        activeStatusFilter = $(this).data('status');

        filterAndRenderApplications();
    });

    $('#input-app-search').on('input', function () {
        filterAndRenderApplications();
    });
}

function loadAdminApplicationsTable() {
    $('#applications-tbody').html(
        '<tr><td colspan="9" class="text-center">Loading applications...</td></tr>'
    );

    API.get('/applications')
    .done(function (apps) {
        cachedApplications = apps || [];
        filterAndRenderApplications();
    })
    .fail(function (xhr) {
        $('#applications-tbody').html(
            `<tr>
                <td colspan="9" class="text-center text-danger">
                    ${xhr.responseText || 'Failed to load applications'}
                </td>
            </tr>`
        );
    });
}

function filterAndRenderApplications() {
    let list = cachedApplications;

    const search = ($('#input-app-search').val() || '').toLowerCase().trim();

    if (activeStatusFilter !== 'ALL') {
        list = list.filter(function (a) {
            return (a.status || 'PENDING').toUpperCase() === activeStatusFilter;
        });
    }

    if (search) {
        list = list.filter(function (a) {
            return (
                (a.fullName || '').toLowerCase().includes(search) ||
                (a.enrollmentNumber || '').toLowerCase().includes(search) ||
                (a.branch || '').toLowerCase().includes(search) ||
                (a.category || '').toLowerCase().includes(search)
            );
        });
    }

    if (list.length === 0) {
        $('#applications-tbody').html(
            '<tr><td colspan="9" class="text-center text-muted">No applications matching criteria.</td></tr>'
        );
        return;
    }

    let html = '';

    list.forEach(function (app) {
        const status = app.status || 'PENDING';

        const pillClass =
            status === 'APPROVED'
                ? 'status-approved'
                : status === 'REJECTED'
                    ? 'status-rejected'
                    : 'status-pending';

        html += `
            <tr>
                <td><strong>#${app.id}</strong></td>

                <td>
                    <strong>${app.fullName || 'Unnamed'}</strong>
                    <br>
                    <small class="text-muted">${app.mobileNumber || ''}</small>
                </td>

                <td>${app.gender || '--'}</td>

                <td>${app.enrollmentNumber || '--'}</td>

                <td>${app.branch || '--'} (Y${app.year || '--'})</td>

                <td>
                    <span class="label label-default">
                        ${app.category || '--'}
                    </span>
                </td>

                <td>
                    <strong>
                        ${
                            app.aggregate != null
                                ? Number(app.aggregate).toFixed(2) + '%'
                                : '--'
                        }
                    </strong>
                </td>

                <td>
                    <span class="status-pill ${pillClass}">
                        ${status}
                    </span>
                </td>

                <td>
                    <div class="btn-group">
                        <a
                            href="application-view.html?id=${app.id}"
                            class="btn btn-xs btn-default"
                            title="View">
                            <span class="glyphicon glyphicon-eye-open"></span>
                        </a>

                        ${
                            status === 'PENDING'
                                ? `
                                    <button class="btn btn-xs btn-success btn-approve-app" data-id="${app.id}">
                                        <span class="glyphicon glyphicon-ok"></span>
                                    </button>

                                    <button class="btn btn-xs btn-danger btn-reject-app" data-id="${app.id}">
                                        <span class="glyphicon glyphicon-remove"></span>
                                    </button>
                                `
                                : `
                                    <button class="btn btn-xs btn-warning btn-reset-app" data-id="${app.id}">
                                        <span class="glyphicon glyphicon-repeat"></span>
                                    </button>
                                `
                        }

                        <button class="btn btn-xs btn-danger btn-delete-app" data-id="${app.id}">
                            <span class="glyphicon glyphicon-trash"></span>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    });

    $('#applications-tbody').html(html);

    $('.btn-approve-app').on('click', function () {
        const id = $(this).data('id');

        if (!confirm(`Approve Application #${id}?`)) return;

        API.put(`/applications/approve/${id}`)
        .done(function () {
            alert(`Application #${id} Approved!`);
            loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
            alert(xhr.responseText || 'Approval failed.');
        });
    });

    $('.btn-reject-app').on('click', function () {
        const id = $(this).data('id');

        const reason = prompt(`Enter rejection reason for Application #${id}:`);

        if (!reason || !reason.trim()) return;

        API.put(`/applications/reject/${id}`, {
            reason: reason.trim()
        })
        .done(function () {
            alert(`Application #${id} Rejected.`);
            loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
            alert(xhr.responseText || 'Rejection failed.');
        });
    });

    $('.btn-reset-app').on('click', function () {
        const id = $(this).data('id');

        if (!confirm(`Reset Application #${id} to PENDING status?`)) return;

        API.put(`/applications/reset/${id}`)
        .done(function () {
            alert(`Application #${id} reset to PENDING.`);
            loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
            alert(xhr.responseText || 'Reset failed.');
        });
    });

    $('.btn-delete-app').off('click').on('click', function () {
        const btn = $(this);
        const id = btn.data('id');

        if (!confirm(`Are you sure you want to permanently delete Application #${id}?\n\nThis will safely remove all associated documents, merit list records, and seat allotments.`)) {
            return;
        }

        btn.prop('disabled', true).html('<span class="glyphicon glyphicon-refresh glyphicon-spin"></span>');

        API.delete(`/applications/${id}`)
        .done(function (res) {
            const msg = (res && res.message) ? res.message : `Application #${id} deleted successfully.`;
            alert(msg);
            loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
            const errorMsg = xhr.responseText || 'Deletion failed. Unable to delete application.';
            alert(errorMsg);
            btn.prop('disabled', false).html('<span class="glyphicon glyphicon-trash"></span>');
        });
    });
}

// =========================================================================
// 5. APPLICATION VIEW
// =========================================================================

function initAdminApplicationView() {
    const user = requireAuth('ADMIN');
    if (!user) return;

    const urlParams = new URLSearchParams(window.location.search);
    const appId = urlParams.get('id');

    if (!appId) {
        showAlert('#alert-container', 'Application ID is missing.', 'danger');
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
        $('#app-inspection-content').html(
            `<div class="alert alert-danger">
                ${xhr.responseText || 'Failed to load application details.'}
            </div>`
        );
    });
}

function renderApplicationInspectionView(app) {
    $('#app-view-id').text(`#${app.id}`);
    $('#app-view-name').text(app.fullName || '--');

    const status = app.status || 'PENDING';

    const pillClass =
        status === 'APPROVED'
            ? 'status-approved'
            : status === 'REJECTED'
                ? 'status-rejected'
                : 'status-pending';

    $('#app-view-status').html(
        `<span class="status-pill ${pillClass}">${status}</span>`
    );

    $('#info-fullname').text(app.fullName || '--');
    $('#info-dob').text(app.dateOfBirth || '--');
    $('#info-gender').text(app.gender || '--');
    $('#info-mobile').text(app.mobileNumber || '--');
    $('#info-address').text(app.address || '--');
    $('#info-category').text(app.category || '--');

    $('#info-enrollment').text(app.enrollmentNumber || '--');
    $('#info-college').text(app.collegeName || '--');
    $('#info-branch').text(app.branch || '--');
    $('#info-year').text(`Year ${app.year || '--'}`);
    $('#info-admission-year').text(app.admissionYear || '--');
    $('#info-atkt').text(app.atktStatus || 'NO');

    $('#info-sem1').text(`${app.sem1Obtained || 0} / ${app.sem1Total || 0}`);
    $('#info-sem2').text(`${app.sem2Obtained || 0} / ${app.sem2Total || 0}`);

    $('#info-aggregate').text(
        app.aggregate != null
            ? Number(app.aggregate).toFixed(2) + '%'
            : '--'
    );

    $('#info-merit-rank').text(
        app.meritRank ? `#${app.meritRank}` : 'Pending calculation'
    );

    if (app.rejectionReason) {
        $('#app-rejection-box')
            .removeClass('hidden')
            .html(
                `<div class="alert alert-danger">
                    <strong>Rejection Reason:</strong>
                    ${app.rejectionReason}
                </div>`
            );
    } else {
        $('#app-rejection-box').addClass('hidden');
    }

    $('#btn-view-approve')
        .off('click')
        .on('click', function () {
            if (!confirm('Approve this application?')) return;

            API.put(`/applications/approve/${app.id}`)
            .done(function () {
                alert('Application approved successfully!');
                loadFullApplicationDetails(app.id);
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Approval failed.');
            });
        });

    $('#btn-view-reject')
        .off('click')
        .on('click', function () {
            const reason = prompt('Enter rejection reason:');

            if (!reason || !reason.trim()) return;

            API.put(`/applications/reject/${app.id}`, {
                reason: reason.trim()
            })
            .done(function () {
                alert('Application rejected.');
                loadFullApplicationDetails(app.id);
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Rejection failed.');
            });
        });

    $('#btn-view-reset')
        .off('click')
        .on('click', function () {
            if (!confirm('Reset application to PENDING status?')) return;

            API.put(`/applications/reset/${app.id}`)
            .done(function () {
                alert('Application status reset to PENDING.');
                loadFullApplicationDetails(app.id);
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Reset failed.');
            });
        });

    $('#btn-view-delete')
        .off('click')
        .on('click', function () {
            if (!confirm(`Are you sure you want to permanently delete Application #${app.id}?\n\nThis will safely remove all associated documents, merit list records, and seat allotments.`)) {
                return;
            }

            const btn = $(this);
            btn.prop('disabled', true).html('<span class="glyphicon glyphicon-refresh glyphicon-spin"></span> Deleting...');

            API.delete(`/applications/${app.id}`)
            .done(function (res) {
                const msg = (res && res.message) ? res.message : `Application #${app.id} deleted successfully.`;
                alert(msg);
                window.location.href = 'applications.html';
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Deletion failed.');
                btn.prop('disabled', false).html('<span class="glyphicon glyphicon-trash"></span> Delete');
            });
        });
}

// =========================================================================
// 6. DOCUMENTS
// =========================================================================

function loadApplicationDocumentsInspection(appId) {
    API.get(`/documents/application/${appId}`)
    .done(function (docs) {
        if (!docs || docs.length === 0) {
            $('#app-docs-panel').html(
                '<p class="text-muted">No documents uploaded by student yet.</p>'
            );
            return;
        }

        let html = '<div class="list-group">';

        docs.forEach(function (doc) {
            const status = doc.verificationStatus || 'PENDING';

            const pillClass =
                status === 'VERIFIED'
                    ? 'status-verified'
                    : status === 'REJECTED'
                        ? 'status-rejected'
                        : 'status-pending';

            html += `
                <div class="list-group-item">
                    <div class="row">
                        <div class="col-md-5">
                            <h5>${doc.documentType || 'Document'}</h5>
                            <p class="text-muted small">
                                ${doc.fileName || 'Attached file'}
                            </p>
                        </div>

                        <div class="col-md-3 text-center">
                            <span class="status-pill ${pillClass}">
                                ${status}
                            </span>
                        </div>

                        <div class="col-md-4 text-right">
                            <a
                                href="${API.getViewUrl(doc.id)}"
                                target="_blank"
                                class="btn btn-xs btn-default">
                                <span class="glyphicon glyphicon-eye-open"></span>
                                View File
                            </a>

                            ${
                                status !== 'VERIFIED'
                                    ? `
                                        <button
                                            class="btn btn-xs btn-success btn-verify-doc"
                                            data-id="${doc.id}">
                                            Verify
                                        </button>
                                    `
                                    : ''
                            }

                            ${
                                status !== 'REJECTED'
                                    ? `
                                        <button
                                            class="btn btn-xs btn-danger btn-reject-doc"
                                            data-id="${doc.id}">
                                            Reject
                                        </button>
                                    `
                                    : ''
                            }
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
            .fail(function (xhr) {
                alert(xhr.responseText || 'Verification failed.');
            });
        });

        $('.btn-reject-doc').on('click', function () {
            const docId = $(this).data('id');

            const reason =
                prompt('Enter rejection reason for this document:');

            if (!reason || !reason.trim()) return;

            API.put(`/documents/reject/${docId}`, {
                reason: reason.trim()
            })
            .done(function () {
                alert('Document marked as rejected.');
                loadApplicationDocumentsInspection(appId);
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Rejection failed.');
            });
        });
    });
}

// =========================================================================
// 7. MERIT LIST
// =========================================================================

function initAdminMeritList() {
    const user = requireAuth('ADMIN');
    if (!user) return;

    $('#form-generate-merit').on('submit', function (e) {
        e.preventDefault();

        const gender = $('#merit-gender').val();
        const branch = $('#merit-branch').val();
        const year = $('#merit-year').val();

        $('#merit-results-box').html(
            '<div class="text-center"><p>Generating merit list...</p></div>'
        );

        API.postParams('/merit/generate', {
            gender: gender,
            branch: branch,
            year: year
        })
        .done(function (list) {
            alert(`Merit list generated with ${list.length} applicants!`);

            renderAdminMeritListTable(list, gender, branch, year);
        })
        .fail(function (xhr) {
            $('#merit-results-box').html(
                `<div class="alert alert-danger">
                    ${xhr.responseText || 'Failed to generate merit list.'}
                </div>`
            );
        });
    });
}

function renderAdminMeritListTable(list, gender, branch, year) {
    if (!list) list = [];

    $('#merit-action-toolbar').removeClass('hidden');

    let html = `
        <h4>
            Merit List: ${gender} - ${branch} - Year ${year}
        </h4>

        <div class="table-responsive">
            <table class="table table-custom table-hover">
                <thead>
                    <tr>
                        <th>Rank</th>
                        <th>Student Name</th>
                        <th>Enrollment No</th>
                        <th>Category</th>
                        <th>Merit Quota</th>
                        <th>Aggregate %</th>
                        <th>ATKT</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
    `;

    list.forEach(function (m) {
        html += `
            <tr>
                <td><strong>#${m.meritRank}</strong></td>
                <td><strong>${m.studentName || '--'}</strong></td>
                <td>${m.enrollmentNo || '--'}</td>
                <td>${m.category || '--'}</td>

                <td>
                    <span class="label label-default">
                        ${m.meritCategory || '--'}
                    </span>
                </td>

                <td>
                    ${
                        m.aggregate != null
                            ? Number(m.aggregate).toFixed(2) + '%'
                            : '--'
                    }
                </td>

                <td>${m.atktStatus || 'NO'}</td>
                <td>${m.meritStatus || 'WAITING'}</td>

                <td>
                    <select
                        class="form-control input-sm select-merit-status"
                        data-id="${m.id}">

                        <option value="SELECTED"
                            ${m.meritStatus === 'SELECTED' ? 'selected' : ''}>
                            SELECTED
                        </option>

                        <option value="WAITING"
                            ${m.meritStatus === 'WAITING' ? 'selected' : ''}>
                            WAITING
                        </option>
                    </select>
                </td>
            </tr>
        `;
    });

    html += `
                </tbody>
            </table>
        </div>
    `;

    $('#merit-results-box').html(html);

    $('.select-merit-status').on('change', function () {
        const id = $(this).data('id');
        const status = $(this).val();

        API.put(`/merit/status/${id}`, {
            status: status
        })
        .done(function () {
            alert(`Status updated to ${status}`);
        })
        .fail(function (xhr) {
            alert(xhr.responseText || 'Failed to update status');
        });
    });

    $('#btn-publish-merit')
        .off('click')
        .on('click', function () {
            API.put('/merit/publish', {
                gender: gender,
                branch: branch,
                year: year
            })
            .done(function (updatedList) {
                alert('Merit list PUBLISHED!');

                renderAdminMeritListTable(
                    updatedList,
                    gender,
                    branch,
                    year
                );
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Publish failed');
            });
        });

    $('#btn-unpublish-merit')
        .off('click')
        .on('click', function () {
            API.put('/merit/unpublish', {
                gender: gender,
                branch: branch,
                year: year
            })
            .done(function (updatedList) {
                alert('Merit list UNPUBLISHED.');

                renderAdminMeritListTable(
                    updatedList,
                    gender,
                    branch,
                    year
                );
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Unpublish failed');
            });
        });

    $('#btn-delete-merit')
        .off('click')
        .on('click', function () {
            if (!confirm(
                `Delete merit list for ${gender} - ${branch} - Year ${year}?`
            )) return;

            API.delete('/merit', {
                gender: gender,
                branch: branch,
                year: year
            })
            .done(function () {
                alert('Merit list deleted.');

                $('#merit-results-box').html(
                    '<div class="empty-box"><p>Merit list deleted.</p></div>'
                );

                $('#merit-action-toolbar').addClass('hidden');
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Delete failed');
            });
        });
}

// =========================================================================
// 8. TWO-STAGE ALLOTMENT ENGINE & RESERVATION CONVERSION
// =========================================================================

function initAdminAllotment() {
    const user = requireAuth('ADMIN');
    if (!user) return;

    $('#form-generate-allotment').on('submit', function (e) {
        e.preventDefault();

        const gender = $('#allot-gender').val();
        const branch = $('#allot-branch').val();
        const year = $('#allot-year').val();

        $('#allotment-results-box').html(
            '<div class="text-center py-4"><p><span class="glyphicon glyphicon-refresh glyphicon-spin"></span> Executing Stage 1: Quota Reservation Allotment...</p></div>'
        );
        $('#allotment-summary-box').html('');
        $('#conversion-toolbar-box').addClass('hidden').html('');

        API.postParams('/allotment/generate', {
            gender: gender,
            branch: branch,
            year: year
        })
        .done(function (allotments) {
            showAlert('#alert-container', `Stage 1 Normal Allotment completed for ${allotments.length} applicants!`, 'success');
            loadAndRenderAllotmentView(gender, branch, year, allotments);
        })
        .fail(function (xhr) {
            $('#allotment-results-box').html(
                `<div class="alert alert-danger">
                    <span class="glyphicon glyphicon-exclamation-sign"></span> ${xhr.responseText || 'Allotment failed.'}
                </div>`
            );
        });
    });

    $('#form-check-vacancy').on('submit', function (e) {
        e.preventDefault();

        loadVacancyAndWaiting(
            $('#vac-gender').val(),
            $('#vac-branch').val(),
            $('#vac-year').val()
        );
    });

    $('#btn-allot-next-waiting').on('click', function () {
        const gender = $('#vac-gender').val();
        const branch = $('#vac-branch').val();
        const year = $('#vac-year').val();

        if (!confirm('Allocate available seat to next waiting candidate?')) return;

        API.put('/waiting-list/allot-next', {
            gender: gender,
            branch: branch,
            year: year
        })
        .done(function (allot) {
            alert(`Seat ${allot.seatNumber} allotted successfully!`);
            loadVacancyAndWaiting(gender, branch, year);
        })
        .fail(function (xhr) {
            alert(xhr.responseText || 'No waiting candidates or available seats.');
        });
    });

    // Initialize Stage 3 Spot Round Engine
    initAdminSpotRound();
}

function loadAndRenderAllotmentView(gender, branch, year, optionalAllotments) {
    API.get(`/allotment/summary?gender=${encodeURIComponent(gender)}&branch=${encodeURIComponent(branch)}&year=${encodeURIComponent(year)}`)
    .done(function (summary) {
        renderAllotmentSummaryAndControls(summary, gender, branch, year);
    });

    if (optionalAllotments) {
        renderAdminAllotmentTable(optionalAllotments, gender, branch, year);
    } else {
        API.get(`/allotment?gender=${encodeURIComponent(gender)}&branch=${encodeURIComponent(branch)}&year=${encodeURIComponent(year)}`)
        .done(function (list) {
            renderAdminAllotmentTable(list, gender, branch, year);
        });
    }
}

function renderAllotmentSummaryAndControls(summary, gender, branch, year) {
    if (!summary) return;

    let quotaChipsHtml = '';
    (summary.quotaBreakdown || []).forEach(function (q) {
        const isReserved = q.isReserved;
        const chipClass = isReserved ? 'reserved' : 'open';
        const typeLabel = isReserved ? 'Reserved' : 'Open Quota';
        quotaChipsHtml += `
            <div class="quota-chip ${chipClass}">
                <span class="text-muted" style="font-size: 10px; display: block; text-transform: uppercase;">${typeLabel}</span>
                <strong>${q.quotaName}</strong>: ${q.occupied}/${q.capacity}
                ${q.unused > 0 ? `<span class="badge" style="background-color: ${isReserved ? '#f59e0b' : '#3b82f6'}; font-size: 10px;">${q.unused} vacant</span>` : '<span class="text-success glyphicon glyphicon-ok" style="font-size: 11px;"></span>'}
            </div>
        `;
    });

    let summaryHtml = `
        <div class="panel panel-custom" style="background: #f8fafc; margin-bottom: 20px;">
            <div class="panel-body">
                <div class="row text-center" style="margin-bottom: 15px;">
                    <div class="col-md-3 col-xs-6">
                        <div class="metric-card primary" style="padding: 12px;">
                            <div class="metric-value" style="font-size: 26px;">${summary.totalCapacity || 0}</div>
                            <div class="metric-label" style="font-size: 11px;">Total Capacity</div>
                        </div>
                    </div>
                    <div class="col-md-3 col-xs-6">
                        <div class="metric-card success" style="padding: 12px;">
                            <div class="metric-value" style="font-size: 26px;">${summary.allottedSeats || 0}</div>
                            <div class="metric-label" style="font-size: 11px;">Seats Allotted</div>
                        </div>
                    </div>
                    <div class="col-md-3 col-xs-6">
                        <div class="metric-card warning" style="padding: 12px;">
                            <div class="metric-value" style="font-size: 26px;">${summary.unusedReservedSeats || 0}</div>
                            <div class="metric-label" style="font-size: 11px;">Unused Reserved</div>
                        </div>
                    </div>
                    <div class="col-md-3 col-xs-6">
                        <div class="metric-card info" style="padding: 12px;">
                            <div class="metric-value" style="font-size: 26px;">${summary.waitingCount || 0}</div>
                            <div class="metric-label" style="font-size: 11px;">Waiting Queue</div>
                        </div>
                    </div>
                </div>

                <div style="border-top: 1px solid #e2e8f0; padding-top: 10px;">
                    <div style="font-size: 12px; font-weight: 700; margin-bottom: 5px; color: #475569;">Quota Distribution & Seat Status:</div>
                    <div>${quotaChipsHtml}</div>
                </div>
            </div>
        </div>
    `;

    $('#allotment-summary-box').html(summaryHtml);

    // Render Stage 2 Conversion Banner
    const toolbar = $('#conversion-toolbar-box');
    if (summary.canConvert) {
        toolbar.removeClass('hidden').html(`
            <div class="alert alert-warning" style="border-left: 5px solid #f59e0b; background-color: #fffbeb; padding: 15px; border-radius: 6px;">
                <div class="row">
                    <div class="col-md-8 col-sm-7">
                        <h4 style="margin-top: 0; color: #b45309; font-weight: 700;">
                            <span class="glyphicon glyphicon-info-sign"></span> Stage 2: Convert Unused Reserved Seats
                        </h4>
                        <p style="margin-bottom: 0; color: #92400e; font-size: 13px;">
                            <strong>${summary.unusedReservedSeats}</strong> reserved quota seat(s) remain unused. You can convert them to <strong>OPEN</strong> category seats so they can be filled by eligible waiting list candidates strictly in order of overall merit rank.
                        </p>
                    </div>
                    <div class="col-md-4 col-sm-5 text-right" style="padding-top: 5px;">
                        <button id="btn-convert-reserved-seats" class="btn btn-convert-reserved btn-lg">
                            <span class="glyphicon glyphicon-random"></span> Convert Unused Reserved Seats (${summary.unusedReservedSeats})
                        </button>
                    </div>
                </div>
            </div>
        `);

        $('#btn-convert-reserved-seats').on('click', function () {
            const btn = $(this);
            if (!confirm(`Convert ${summary.unusedReservedSeats} unused reserved seat(s) to OPEN category and allocate them to waiting students in order of merit?`)) {
                return;
            }

            btn.prop('disabled', true).html('<span class="glyphicon glyphicon-refresh glyphicon-spin"></span> Converting Seats...');

            API.postParams('/allotment/convert-reserved', {
                gender: gender,
                branch: branch,
                year: year
            })
            .done(function (updatedList) {
                alert(`Stage 2 Conversion Successful! ${summary.unusedReservedSeats} unused reserved seat(s) converted to OPEN and allocated strictly by merit.`);
                loadAndRenderAllotmentView(gender, branch, year, updatedList);
            })
            .fail(function (xhr) {
                alert(xhr.responseText || 'Conversion failed.');
                btn.prop('disabled', false).html(`<span class="glyphicon glyphicon-random"></span> Convert Unused Reserved Seats (${summary.unusedReservedSeats})`);
            });
        });
    } else if (summary.isConverted) {
        toolbar.removeClass('hidden').html(`
            <div class="alert alert-success" style="border-left: 5px solid #10b981; background-color: #f0fdf4; padding: 12px 15px; border-radius: 6px;">
                <span class="glyphicon glyphicon-ok-sign" style="color: #16a34a; font-size: 16px; vertical-align: middle;"></span>
                <strong style="color: #15803d; margin-left: 5px;">Stage 2 Applied:</strong>
                <span style="color: #166534; font-size: 13px;">Unused reserved seats for this cycle have been converted into OPEN seats and allocated to waiting list students according to overall merit.</span>
            </div>
        `);
    } else {
        toolbar.addClass('hidden').html('');
    }
}

function renderAdminAllotmentTable(list, gender, branch, year) {
    if (!list) list = [];

    // Header actions with PDF Download
    $('#allotment-header-actions').html(`
        <a href="${API_BASE}/pdf/allotment?gender=${encodeURIComponent(gender)}&year=${encodeURIComponent(year)}&round=REGULAR" target="_blank" class="btn btn-primary-custom btn-sm">
            <span class="glyphicon glyphicon-print"></span> Download Allotment PDF
        </a>
    `);

    let html = `
        <div class="table-responsive">
            <table class="table table-custom table-hover">
                <thead>
                    <tr>
                        <th style="width: 70px;">Rank</th>
                        <th>Student Name</th>
                        <th>Enrollment No</th>
                        <th>Assigned Seat</th>
                        <th>Original Category</th>
                        <th>Common Category</th>
                        <th>Allotted Quota</th>
                        <th>Allocation Mode</th>
                        <th>Aggregate</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
    `;

    list.forEach(function (a) {
        const isWaiting = a.allotmentStatus === 'WAITING';
        const isConverted = Boolean(a.isConverted);
        const statusClass = a.allotmentStatus === 'ACCEPTED' ? 'status-approved' : (a.allotmentStatus === 'ALLOTTED' ? 'status-pending' : (a.allotmentStatus === 'REJECTED' ? 'status-rejected' : 'status-waiting'));

        html += `
            <tr style="${isConverted ? 'background-color: #fffbeb;' : (isWaiting ? 'color: #64748b;' : '')}">
                <td><strong>#${a.meritRank || '--'}</strong></td>
                <td>
                    <strong>${a.application?.fullName || a.meritList?.studentName || 'Student'}</strong>
                </td>
                <td>${a.application?.enrollmentNumber || a.meritList?.enrollmentNo || '--'}</td>

                <td>
                    ${isWaiting ? `<span class="badge badge-waiting">${a.seatNumber || 'WAITING'}</span>` : `<span class="label label-primary" style="font-size: 12px;">${a.seatNumber || 'N/A'}</span>`}
                </td>

                <td>
                    <span class="label label-default" style="font-size: 11px;">
                        ${a.category || '--'}
                    </span>
                </td>

                <td>
                    <strong>${a.commonCategory || '--'}</strong>
                </td>

                <td>
                    <span class="label ${isWaiting ? 'label-default' : (isConverted ? 'label-warning' : 'label-info')}">
                        ${a.allotmentCategory || '--'}
                    </span>
                </td>

                <td>
                    ${isWaiting ? `<span class="badge badge-waiting">Waiting</span>` : (isConverted ? `<span class="badge badge-stage-converted"><span class="glyphicon glyphicon-random"></span> Converted to OPEN</span>` : `<span class="badge badge-stage-normal">Stage 1 Quota</span>`)}
                </td>

                <td>
                    ${a.aggregate != null ? Number(a.aggregate).toFixed(2) + '%' : '--'}
                </td>

                <td>
                    <span class="status-pill ${statusClass}">
                        ${a.allotmentStatus || '--'}
                    </span>
                </td>
            </tr>
        `;
    });

    html += `
                </tbody>
            </table>
        </div>
    `;

    $('#allotment-results-box').html(html);
}

function loadVacancyAndWaiting(gender, branch, year) {
    $('#vacancy-stats-box').html('<p>Loading vacancy data...</p>');
    $('#waiting-list-box').html('<p>Loading waiting list...</p>');

    API.get(
        `/vacancy?gender=${encodeURIComponent(gender)}&branch=${encodeURIComponent(branch)}&year=${encodeURIComponent(year)}`
    )
    .done(function (vac) {
        $('#vacancy-stats-box').html(`
            <div class="row text-center">
                <div class="col-md-3 col-xs-6">
                    <div class="metric-card primary">
                        <div class="metric-value">${vac.totalSeats || 0}</div>
                        <div class="metric-label">Total Seats</div>
                    </div>
                </div>

                <div class="col-md-3 col-xs-6">
                    <div class="metric-card warning">
                        <div class="metric-value">${vac.allottedSeats || 0}</div>
                        <div class="metric-label">Allotted</div>
                    </div>
                </div>

                <div class="col-md-3 col-xs-6">
                    <div class="metric-card success">
                        <div class="metric-value">${vac.acceptedSeats || 0}</div>
                        <div class="metric-label">Accepted</div>
                    </div>
                </div>

                <div class="col-md-3 col-xs-6">
                    <div class="metric-card info">
                        <div class="metric-value">${vac.availableSeats || 0}</div>
                        <div class="metric-label">Available</div>
                    </div>
                </div>
            </div>
        `);
    });

    API.get(
        `/waiting-list?gender=${encodeURIComponent(gender)}&branch=${encodeURIComponent(branch)}&year=${encodeURIComponent(year)}`
    )
    .done(function (waiting) {
        if (!waiting || waiting.length === 0) {
            $('#waiting-list-box').html(
                '<div class="empty-box"><p>No candidates currently on waiting list.</p></div>'
            );

            $('#allot-next-toolbar').addClass('hidden');
            return;
        }

        $('#allot-next-toolbar').removeClass('hidden');

        let html = `
            <h5>Waiting List Queue (${waiting.length} Students)</h5>

            <div class="table-responsive">
                <table class="table table-custom">
                    <thead>
                        <tr>
                            <th>Queue</th>
                            <th>Student</th>
                            <th>Merit Rank</th>
                            <th>Category</th>
                            <th>Aggregate</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody>
        `;

        waiting.forEach(function (w, index) {
            html += `
                <tr>
                    <td>#${index + 1}</td>
                    <td>${w.application?.fullName || 'Student'}</td>
                    <td>#${w.meritRank || '--'}</td>
                    <td>${w.category || '--'}</td>

                    <td>
                        ${
                            w.aggregate != null
                                ? Number(w.aggregate).toFixed(2) + '%'
                                : '--'
                        }
                    </td>

                    <td>${w.allotmentStatus || '--'}</td>
                </tr>
            `;
        });

        html += `
                    </tbody>
                </table>
            </div>
        `;

        $('#waiting-list-box').html(html);
    });
}

// =========================================================================
// 8.5 STAGE 3: COMMON POOL SPOT ROUND ENGINE
// =========================================================================

function initAdminSpotRound() {
    $('#spot-gender').off('change').on('change', function () {
        loadSpotRoundData();
    });

    $('#btn-check-spot').off('click').on('click', function () {
        loadSpotRoundData();
    });

    $('#btn-run-spot-round').off('click').on('click', function () {
        const gender = $('#spot-gender').val() || 'BOYS';
        const genderLabel = gender === 'BOYS' ? 'Boys Hostel (165 total beds)' : 'Girls Hostel (45 total beds)';

        if (!confirm(`Execute Stage 3 Common Pool Spot Round for ${genderLabel}?\n\nThis will calculate vacant hostel beds and allocate them to eligible, unallotted candidates across all branches strictly in order of academic score.`)) {
            return;
        }

        const btn = $(this);
        btn.prop('disabled', true).html('<span class="glyphicon glyphicon-refresh glyphicon-spin"></span> Processing Spot Allotment...');

        API.postParams('/allotment/spot/generate', { gender: gender })
        .done(function (allotments) {
            showAlert('#alert-container', `Stage 3 Spot Round completed! Processed ${allotments.length} applicants into common vacant bed pool.`, 'success');
            loadSpotRoundData();
        })
        .fail(function (xhr) {
            showAlert('#alert-container', xhr.responseText || 'Spot round generation failed.', 'danger');
            loadSpotRoundData();
        })
        .always(function () {
            btn.prop('disabled', false).html('<span class="glyphicon glyphicon-flash"></span> Run Spot Round Allotment');
        });
    });

    // Initial load for Spot Round data
    loadSpotRoundData();
}

function loadSpotRoundData() {
    const gender = $('#spot-gender').val() || 'BOYS';

    $('#spot-results-box').html('<div class="text-center py-4"><p><span class="glyphicon glyphicon-refresh glyphicon-spin"></span> Loading Spot Round state...</p></div>');
    $('#spot-eligible-box').html('<div class="text-center py-3"><p class="text-muted">Loading applicant queue...</p></div>');

    // 1. Fetch Spot Summary
    API.get(`/allotment/spot/summary?gender=${encodeURIComponent(gender)}`)
    .done(function (summary) {
        renderSpotMetricsAndBanner(summary);
    })
    .fail(function (xhr) {
        $('#spot-metrics-box').html(`<div class="alert alert-danger">${xhr.responseText || 'Failed to load spot summary'}</div>`);
    });

    // 2. Fetch Spot Allotments
    API.get(`/allotment/spot?gender=${encodeURIComponent(gender)}`)
    .done(function (list) {
        renderSpotAllotmentsTable(list || [], gender);
    })
    .fail(function (xhr) {
        $('#spot-results-box').html(`<div class="alert alert-danger">${xhr.responseText || 'Failed to load spot allotments'}</div>`);
    });

    // 3. Fetch Eligible Applicants Queue
    API.get(`/allotment/spot/eligible?gender=${encodeURIComponent(gender)}`)
    .done(function (eligible) {
        renderSpotEligibleTable(eligible || [], gender);
    })
    .fail(function (xhr) {
        $('#spot-eligible-box').html(`<div class="alert alert-danger">${xhr.responseText || 'Failed to load applicant queue'}</div>`);
    });
}

function renderSpotMetricsAndBanner(summary) {
    if (!summary) return;

    const isAvailable = summary.availableSpotSeats > 0;

    let metricsHtml = `
        <div class="panel panel-custom" style="background: #faf5ff; border: 1px solid #e9d5ff; margin-bottom: 20px;">
            <div class="panel-body">
                <div class="row text-center">
                    <div class="col-md-2 col-xs-6">
                        <div class="metric-card primary" style="padding: 10px;">
                            <div class="metric-value" style="font-size: 24px;">${summary.totalCapacity || 0}</div>
                            <div class="metric-label" style="font-size: 10px;">Total Hostel Beds</div>
                        </div>
                    </div>
                    <div class="col-md-2 col-xs-6">
                        <div class="metric-card info" style="padding: 10px;">
                            <div class="metric-value" style="font-size: 24px;">${summary.regularAllottedCount || 0}</div>
                            <div class="metric-label" style="font-size: 10px;">Regular Occupied</div>
                        </div>
                    </div>
                    <div class="col-md-2 col-xs-6">
                        <div class="metric-card warning" style="padding: 10px;">
                            <div class="metric-value" style="font-size: 24px;">${summary.regularConvertedCount || 0}</div>
                            <div class="metric-label" style="font-size: 10px;">Converted Seats</div>
                        </div>
                    </div>
                    <div class="col-md-2 col-xs-6">
                        <div class="metric-card success" style="padding: 10px; background: #ede9fe; border-color: #c4b5fd;">
                            <div class="metric-value" style="font-size: 24px; color: #6d28d9;">${summary.availableSpotSeats || 0}</div>
                            <div class="metric-label" style="font-size: 10px; color: #5b21b6; font-weight: 700;">Vacant Spot Beds</div>
                        </div>
                    </div>
                    <div class="col-md-2 col-xs-6">
                        <div class="metric-card primary" style="padding: 10px;">
                            <div class="metric-value" style="font-size: 24px;">${summary.eligibleApplicantsCount || 0}</div>
                            <div class="metric-label" style="font-size: 10px;">Eligible Queue</div>
                        </div>
                    </div>
                    <div class="col-md-2 col-xs-6">
                        <div class="metric-card" style="padding: 10px; background: #f1f5f9; border-color: #cbd5e1;">
                            <div class="metric-value" style="font-size: 24px; color: #475569;">${summary.spotWaitingCount || 0}</div>
                            <div class="metric-label" style="font-size: 10px; color: #475569;">Spot Waiting</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

    $('#spot-metrics-box').html(metricsHtml);

    let bannerHtml = '';
    if (isAvailable) {
        bannerHtml = `
            <div class="alert alert-success" style="border-left: 5px solid #7c3aed; background-color: #f5f3ff; color: #5b21b6; margin-bottom: 20px;">
                <h4 style="margin-top: 0; font-weight: 700;">
                    <span class="glyphicon glyphicon-ok-sign"></span> Spot Round Open (${summary.availableSpotSeats} Vacant Beds in ${summary.hostelName})
                </h4>
                <p style="margin-bottom: 0; font-size: 13px;">
                    Common pool is open for all branches (Computer, Mechanical, Civil, Electrical, IT), years, and categories. Click <strong>"Run Spot Round Allotment"</strong> to automatically allocate the ${summary.availableSpotSeats} vacant beds to top-scoring candidates.
                </p>
            </div>
        `;
    } else {
        bannerHtml = `
            <div class="alert alert-info" style="border-left: 5px solid #0284c7; background-color: #f0f9ff; color: #0369a1; margin-bottom: 20px;">
                <h4 style="margin-top: 0; font-weight: 700;">
                    <span class="glyphicon glyphicon-info-sign"></span> 0 Vacant Beds in ${summary.hostelName}
                </h4>
                <p style="margin-bottom: 0; font-size: 13px;">
                    All ${summary.totalCapacity} beds in ${summary.hostelName} are currently occupied. If any student surrenders or rejects their seat, vacant spots will automatically open here.
                </p>
            </div>
        `;
    }

    $('#spot-banner-box').html(bannerHtml);
}

function renderSpotAllotmentsTable(list, gender) {
    if (!list || list.length === 0) {
        $('#spot-header-actions').html('');
        $('#spot-results-box').html(`
            <div class="empty-box">
                <span class="glyphicon glyphicon-star"></span>
                <h4>No Spot Allotments Yet</h4>
                <p>Click "Run Spot Round Allotment" or manually allot individual candidates from the queue below.</p>
            </div>
        `);
        return;
    }

    $('#spot-header-actions').html(`
        <div class="btn-group">
            <a href="${API_BASE}/pdf/allotment?gender=${encodeURIComponent(gender)}&year=1&round=SPOT" target="_blank" class="btn btn-default btn-sm"><span class="glyphicon glyphicon-print"></span> 1st Yr Spot PDF</a>
            <a href="${API_BASE}/pdf/allotment?gender=${encodeURIComponent(gender)}&year=2&round=SPOT" target="_blank" class="btn btn-default btn-sm"><span class="glyphicon glyphicon-print"></span> 2nd Yr Spot PDF</a>
            <a href="${API_BASE}/pdf/allotment?gender=${encodeURIComponent(gender)}&year=3&round=SPOT" target="_blank" class="btn btn-default btn-sm"><span class="glyphicon glyphicon-print"></span> 3rd Yr Spot PDF</a>
        </div>
    `);

    let html = `
        <div class="table-responsive">
            <table class="table table-custom table-hover">
                <thead>
                    <tr>
                        <th style="width: 70px;">Rank</th>
                        <th>Student Name</th>
                        <th>Enrollment No</th>
                        <th>Assigned Bed / Seat</th>
                        <th>Branch</th>
                        <th>Year</th>
                        <th>Original Category</th>
                        <th>Score</th>
                        <th>Round</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
    `;

    list.forEach(function (a) {
        const isWaiting = a.allotmentStatus === 'WAITING';
        const statusClass = a.allotmentStatus === 'ACCEPTED' ? 'status-approved' : (a.allotmentStatus === 'ALLOTTED' ? 'status-pending' : (a.allotmentStatus === 'REJECTED' ? 'status-rejected' : 'status-waiting'));

        html += `
            <tr style="${isWaiting ? 'color: #64748b;' : 'background-color: #faf5ff;'}">
                <td><strong>#${a.meritRank || '--'}</strong></td>
                <td>
                    <strong>${a.application?.fullName || 'Student'}</strong>
                </td>
                <td>${a.application?.enrollmentNumber || '--'}</td>
                <td>
                    ${isWaiting ? `<span class="badge badge-waiting">${a.seatNumber || 'WAITING'}</span>` : `<span class="badge badge-stage-spot" style="font-size: 12px;">${a.seatNumber || 'N/A'}</span>`}
                </td>
                <td><span class="label label-default">${a.branch || '--'}</span></td>
                <td>Year ${a.year || '--'}</td>
                <td>${a.category || '--'}</td>
                <td><strong>${a.aggregate != null ? Number(a.aggregate).toFixed(2) + '%' : '--'}</strong></td>
                <td><span class="badge badge-stage-spot"><span class="glyphicon glyphicon-star"></span> STAGE 3 SPOT</span></td>
                <td><span class="status-pill ${statusClass}">${a.allotmentStatus || '--'}</span></td>
            </tr>
        `;
    });

    html += `</tbody></table></div>`;
    $('#spot-results-box').html(html);
}

function renderSpotEligibleTable(list, gender) {
    if (!list || list.length === 0) {
        $('#spot-eligible-box').html('<p class="text-muted text-center">No unallotted approved applicants found in this gender pool.</p>');
        return;
    }

    let html = `
        <div class="table-responsive">
            <table class="table table-custom table-hover">
                <thead>
                    <tr>
                        <th>Queue #</th>
                        <th>Student Name</th>
                        <th>Enrollment No</th>
                        <th>Branch</th>
                        <th>Year</th>
                        <th>Category</th>
                        <th>Academic Score</th>
                        <th>Spot Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
    `;

    list.forEach(function (s, index) {
        const isAllottedInSpot = s.spotStatus === 'ALLOTTED' || s.spotStatus === 'ACCEPTED';
        const isWaitingInSpot = s.spotStatus === 'WAITING';

        html += `
            <tr>
                <td><strong>#${index + 1}</strong></td>
                <td><strong>${s.fullName || '--'}</strong><br><small class="text-muted">${s.mobileNumber || ''}</small></td>
                <td>${s.enrollmentNumber || '--'}</td>
                <td><span class="label label-default">${s.branch || '--'}</span></td>
                <td>Year ${s.year || '--'}</td>
                <td>${s.category || '--'}</td>
                <td><strong>${s.aggregate != null ? Number(s.aggregate).toFixed(2) + '%' : '--'}</strong></td>
                <td>
                    ${isAllottedInSpot ? `<span class="badge badge-stage-spot">${s.seatNumber || 'ALLOTTED'}</span>` : (isWaitingInSpot ? `<span class="badge badge-waiting">WAITING</span>` : '<span class="status-pill status-open">UNPROCESSED</span>')}
                </td>
                <td>
                    ${!isAllottedInSpot ? `
                        <button class="btn btn-xs btn-spot-run btn-single-spot-allot" data-appid="${s.applicationId}">
                            <span class="glyphicon glyphicon-plus"></span> Allot Spot Bed
                        </button>
                    ` : `
                        <span class="text-success"><span class="glyphicon glyphicon-ok"></span> Allotted</span>
                    `}
                </td>
            </tr>
        `;
    });

    html += `</tbody></table></div>`;
    $('#spot-eligible-box').html(html);

    // Bind Single Student Allotment Action (Option C)
    $('.btn-single-spot-allot').on('click', function () {
        const appId = $(this).data('appid');
        if (!confirm(`Directly allot a common pool Spot Bed to Application #${appId}?`)) {
            return;
        }

        const btn = $(this);
        btn.prop('disabled', true).text('Allotting...');

        API.postParams('/allotment/spot/allot-single', { applicationId: appId })
        .done(function (allot) {
            alert(`Spot Bed ${allot.seatNumber} allotted successfully!`);
            loadSpotRoundData();
        })
        .fail(function (xhr) {
            alert(xhr.responseText || 'Single spot allotment failed.');
            btn.prop('disabled', false).html('<span class="glyphicon glyphicon-plus"></span> Allot Spot Bed');
        });
    });
}

// =========================================================================
// 9. STUDENTS DIRECTORY
// =========================================================================

let cachedStudents = [];

function initAdminStudents() {
    const user = requireAuth('ADMIN');
    if (!user) return;

    loadStudentsDirectory();

    $('#filter-branch, #filter-year, #filter-gender, #filter-status, #filter-category')
        .on('change', function () {
            filterStudentsDirectory();
        });

    $('#search-student').on('input', function () {
        filterStudentsDirectory();
    });
}

function loadStudentsDirectory() {
    API.get('/applications')
    .done(function (apps) {
        cachedStudents = apps || [];
        filterStudentsDirectory();
    })
    .fail(function () {
        $('#students-tbody').html(
            '<tr><td colspan="8" class="text-center text-danger">Failed to load students.</td></tr>'
        );
    });
}

function filterStudentsDirectory() {
    let list = cachedStudents;

    const branch = $('#filter-branch').val();
    const year = $('#filter-year').val();
    const gender = $('#filter-gender').val();
    const status = $('#filter-status').val();
    const category = $('#filter-category').val();

    const search = ($('#search-student').val() || '').toLowerCase().trim();

    if (branch && branch !== 'ALL') {
        list = list.filter(s => s.branch === branch);
    }

    if (year && year !== 'ALL') {
        list = list.filter(s => String(s.year) === String(year));
    }

    if (gender && gender !== 'ALL') {
        list = list.filter(s => s.gender === gender);
    }

    if (status && status !== 'ALL') {
        list = list.filter(s => (s.status || 'PENDING') === status);
    }

    if (category && category !== 'ALL') {
        list = list.filter(s => s.category === category);
    }

    if (search) {
        list = list.filter(function (s) {
            return (
                (s.fullName || '').toLowerCase().includes(search) ||
                (s.enrollmentNumber || '').toLowerCase().includes(search) ||
                (s.mobileNumber || '').toLowerCase().includes(search)
            );
        });
    }

    if (list.length === 0) {
        $('#students-tbody').html(
            '<tr><td colspan="8" class="text-center text-muted">No students matching criteria.</td></tr>'
        );
        return;
    }

    let html = '';

    list.forEach(function (s) {
        const status = s.status || 'PENDING';

        const statusClass =
            status === 'APPROVED'
                ? 'status-approved'
                : status === 'REJECTED'
                    ? 'status-rejected'
                    : 'status-pending';

        html += `
            <tr>
                <td>#${s.id}</td>

                <td>
                    <strong>${s.fullName || '--'}</strong>
                </td>

                <td>${s.enrollmentNumber || '--'}</td>
                <td>${s.gender || '--'}</td>

                <td>
                    ${s.branch || '--'} (Y${s.year || '--'})
                </td>

                <td>
                    <span class="label label-default">
                        ${s.category || '--'}
                    </span>
                </td>

                <td>
                    ${
                        s.aggregate != null
                            ? Number(s.aggregate).toFixed(2) + '%'
                            : '--'
                    }
                </td>

                <td>
                    <span class="status-pill ${statusClass}">
                        ${status}
                    </span>
                </td>

                <td>
                    <div class="btn-group">
                        <a
                            href="application-view.html?id=${s.id}"
                            class="btn btn-xs btn-default"
                            title="View Application">
                            <span class="glyphicon glyphicon-eye-open"></span>
                        </a>

                        <button
                            class="btn btn-xs btn-danger btn-delete-student"
                            data-id="${s.id}"
                            title="Delete Application">
                            <span class="glyphicon glyphicon-trash"></span>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    });

    $('#students-tbody').html(html);

    $('.btn-delete-student').off('click').on('click', function () {
        const btn = $(this);
        const id = btn.data('id');

        if (!confirm(`Are you sure you want to permanently delete Application #${id}?\n\nThis will safely remove all associated documents, merit list records, and seat allotments.`)) {
            return;
        }

        btn.prop('disabled', true).html('<span class="glyphicon glyphicon-refresh glyphicon-spin"></span>');

        API.delete(`/applications/${id}`)
        .done(function (res) {
            const msg = (res && res.message) ? res.message : `Application #${id} deleted successfully.`;
            alert(msg);
            loadStudentsDirectory();
        })
        .fail(function (xhr) {
            const errorMsg = xhr.responseText || 'Deletion failed. Unable to delete application.';
            alert(errorMsg);
            btn.prop('disabled', false).html('<span class="glyphicon glyphicon-trash"></span>');
        });
    });
}

// =========================================================================
// 10. ADMIN REPORTS
// =========================================================================

function initAdminReports() {
    const user = requireAuth('ADMIN');
    if (!user) return;

    // Initialize Official Hostel PDF Generation Console
    initAdminPdfGeneration();

    API.get('/applications')
    .done(function (apps) {
        renderReportsData(apps || []);
    })
    .fail(function () {
        showAlert(
            '#alert-container',
            'Failed to load reports.',
            'danger'
        );
    });
}

function renderReportsData(apps) {
    $('#report-total-apps').text(apps.length);

    const approved = apps.filter(a => a.status === 'APPROVED').length;
    const pending = apps.filter(a => a.status === 'PENDING').length;
    const rejected = apps.filter(a => a.status === 'REJECTED').length;

    $('#rep-approved').text(approved);
    $('#rep-pending').text(pending);
    $('#rep-rejected').text(rejected);

    const branches = [
        'COMPUTER',
        'MECHANICAL',
        'CIVIL',
        'ELECTRICAL',
        'IT'
    ];

    let branchHtml = '';

    branches.forEach(function (branch) {
        const count = apps.filter(a => a.branch === branch).length;

        const percentage =
            apps.length > 0
                ? ((count / apps.length) * 100).toFixed(1)
                : '0.0';

        branchHtml += `
            <tr>
                <th>${branch}</th>

                <td>
                    <strong>${count}</strong>
                </td>

                <td>
                    <div class="progress">
                        <div
                            class="progress-bar progress-bar-info"
                            style="width:${percentage}%">
                            ${percentage}%
                        </div>
                    </div>
                </td>
            </tr>
        `;
    });

    $('#branch-breakdown-tbody').html(branchHtml);

    const boys = apps.filter(a => a.gender === 'BOYS').length;
    const girls = apps.filter(a => a.gender === 'GIRLS').length;

    $('#rep-boys').text(boys);
    $('#rep-girls').text(girls);

    const y1 = apps.filter(a => String(a.year) === '1').length;
    const y2 = apps.filter(a => String(a.year) === '2').length;
    const y3 = apps.filter(a => String(a.year) === '3').length;

    $('#rep-y1').text(y1);
    $('#rep-y2').text(y2);
    $('#rep-y3').text(y3);
}

// =========================================================================
// 11. OFFICIAL HOSTEL ALLOTMENT PDF GENERATION
// =========================================================================

function initAdminPdfGeneration() {
    // 1. Form submission handler for customized combinations
    $('#form-generate-pdf').off('submit').on('submit', function (e) {
        e.preventDefault();
        const gender = $('#pdf-gender').val() || 'BOYS';
        const year = $('#pdf-year').val() || '1';
        const round = $('#pdf-round').val() || 'REGULAR';
        const url = `${API_BASE}/pdf/allotment?gender=${encodeURIComponent(gender)}&year=${encodeURIComponent(year)}&round=${encodeURIComponent(round)}`;
        window.open(url, '_blank');
    });

    // 2. Quick generate buttons (Boys 1/2/3 Yr Regular & Spot, Girls 1/2/3 Yr Regular & Spot)
    $('.btn-quick-pdf').off('click').on('click', function (e) {
        e.preventDefault();
        const gender = $(this).data('gender') || 'BOYS';
        const year = $(this).data('year') || '1';
        const round = $(this).data('round') || 'REGULAR';
        const url = `${API_BASE}/pdf/allotment?gender=${encodeURIComponent(gender)}&year=${encodeURIComponent(year)}&round=${encodeURIComponent(round)}`;
        window.open(url, '_blank');
    });

    // 3. Complete Master Hostel PDF generator button
    $('#btn-complete-pdf').off('click').on('click', function (e) {
        e.preventDefault();
        const url = `${API_BASE}/pdf/allotment/complete`;
        window.open(url, '_blank');
    });
}
