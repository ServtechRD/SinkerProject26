# T010: Login Logging and Account Lockout

## Context
This task implements comprehensive login audit logging and automatic account lockout functionality. Every login attempt (successful or failed) is logged to the login_logs table with IP address and user agent. After 5 consecutive failed login attempts, the user account is automatically locked.

## Goal
Enhance authentication security by logging all login attempts for audit purposes and implementing automatic account lockout after repeated failed login attempts. This provides both security against brute-force attacks and audit trail for compliance.

## Scope

### In Scope
- Log every login attempt to login_logs table
- Log successful logins with user_id, username, IP, user agent
- Log failed logins with username, IP, user agent, failure reason
- Increment failed_login_count on users table for failed attempts
- Reset failed_login_count to 0 on successful login
- Set is_locked=TRUE after 5 failed attempts
- Return appropriate error message for locked account
- Extract IP address from request
- Extract user agent from request headers
- Modify AuthController/AuthService to integrate logging
- Modify login flow to check and update failed_login_count

### Out of Scope
- Manual unlock functionality (admin unlocking users - can be added later)
- Automatic unlock after time period
- CAPTCHA after N failed attempts
- Email notification on account lockout
- Login attempt rate limiting (separate from lockout)
- Geographic IP analysis
- Device fingerprinting
- Login log retention/archival automation

## Requirements
- Every POST /api/auth/login triggers a login log entry
- Successful login: login_type='success', user_id set, username, IP, user agent
- Failed login: login_type='failed', user_id NULL (or attempt to find by username), username, IP, user agent, failed_reason
- Failed reasons: "Invalid username or password", "Account locked", "Account inactive"
- Extract IP from X-Forwarded-For header (if behind proxy) or from request.getRemoteAddr()
- Extract user agent from User-Agent header
- On failed login with valid username: increment users.failed_login_count
- If failed_login_count reaches 5: set is_locked=TRUE
- On successful login: reset failed_login_count to 0
- Locked account login attempt returns 403 with message "Account is locked"
- All logging happens after authentication check (don't log before validating)
- Log entries are fire-and-forget (don't fail login if logging fails)
- Timestamps automatically set by database

## Implementation Notes
- Create LoginLogService with methods: logSuccessfulLogin(), logFailedLogin()
- Create LoginLog entity mapped to login_logs table
- Create LoginLogRepository
- Modify AuthService.login() to call logging methods
- Extract IP using utility method (handle X-Forwarded-For, X-Real-IP)
- Extract user agent from HttpServletRequest headers
- Use @Async for logging (optional, to not block login response)
- Transaction management: login log insert should not rollback login success
- Use separate transaction for log insert (REQUIRES_NEW)
- Check is_locked before attempting password verification
- Increment failed_login_count in same transaction as login attempt
- Log failed reason clearly for audit purposes

## Files to Change
- service/LoginLogService.java (new)
- repository/LoginLogRepository.java (new)
- entity/LoginLog.java (new)
- service/AuthService.java (update)
- controller/AuthController.java (update)
- util/IPAddressUtil.java (new)
- exception/AccountLockedException.java (update or create if not exists)

## Dependencies
- T007: Requires login_logs table
- T002: Requires authentication implementation
