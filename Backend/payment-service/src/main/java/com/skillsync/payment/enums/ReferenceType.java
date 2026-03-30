package com.skillsync.payment.enums;

/**
 * Describes the business context a payment is tied to.
 * Every payment MUST have a reference type for traceability.
 */
public enum ReferenceType {

    /** Payment for booking a session — referenceId = sessionRequestId or mentorId */
    SESSION_BOOKING
}
