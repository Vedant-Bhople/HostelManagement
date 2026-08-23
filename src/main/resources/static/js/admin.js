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

    $('.btn-delete-app').on('click', function () {
        const id = $(this).data('id');

        if (!confirm(`Permanently delete Application #${id}?`)) return;

        API.delete(`/applications/${id}`)
        .done(function () {
            alert(`Application #${id} deleted.`);
            loadAdminApplicationsTable();
        })
        .fail(function (xhr) {
            alert(xhr.responseText || 'Deletion failed.');
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
// 8. ALLOTMENT
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
            '<div class="text-center"><p>Executing seat allocation...</p></div>'
        );

        API.postParams('/allotment/generate', {
            gender: gender,
            branch: branch,
            year: year
        })
        .done(function (allotments) {
            alert(
                `Seat Allotment completed for ${allotments.length} applicants!`
            );

            renderAdminAllotmentTable(
                allotments,
                gender,
                branch,
                year
            );
        })
        .fail(function (xhr) {
            $('#allotment-results-box').html(
                `<div class="alert alert-danger">
                    ${xhr.responseText || 'Allotment failed.'}
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

        if (!confirm(
            'Allocate available seat to next waiting candidate?'
        )) return;

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
            alert(
                xhr.responseText ||
                'No waiting candidates or available seats.'
            );
        });
    });
}

function renderAdminAllotmentTable(list, gender, branch, year) {
    let html = `
        <h4>
            Allotment Results: ${gender} - ${branch} - Year ${year}
        </h4>

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
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
    `;

    (list || []).forEach(function (a) {
        html += `
            <tr>
                <td>#${a.meritRank || '--'}</td>
                <td>${a.application?.fullName || 'Student'}</td>

                <td>
                    <span class="label label-primary">
                        ${a.seatNumber || 'N/A'}
                    </span>
                </td>

                <td>${a.allotmentCategory || 'OPEN'}</td>
                <td>${a.category || '--'}</td>

                <td>
                    ${
                        a.aggregate != null
                            ? Number(a.aggregate).toFixed(2) + '%'
                            : '--'
                    }
                </td>

                <td>${a.allotmentStatus || '--'}</td>
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
            </tr>
        `;
    });

    $('#students-tbody').html(html);
}

// =========================================================================
// 10. ADMIN REPORTS
// =========================================================================

function initAdminReports() {
    const user = requireAuth('ADMIN');
    if (!user) return;

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
