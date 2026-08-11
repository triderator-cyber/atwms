package com.atwms.common.tenant;

/**
 * Wird geworfen, wenn ein Request keinen aufloesbaren Mandantenkontext besitzt.
 * Fuehrt ueber {@link com.atwms.common.api.UnresolvedTenantExceptionMapper}
 * zu einem HTTP 403.
 */
public class UnresolvedTenantException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnresolvedTenantException(String message) {
        super(message);
    }
}
