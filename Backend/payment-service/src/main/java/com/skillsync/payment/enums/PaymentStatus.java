package com.skillsync.payment.enums;

/**
 * Payment lifecycle states supporting Saga orchestration.
 *
 * State machine:
 *   CREATED → VERIFIED → SUCCESS_PENDING → SUCCESS
 *                                        → COMPENSATED
 *            → FAILED
 */
public enum PaymentStatus {

    /** Razorpay order created, awaiting frontend checkout */
    CREATED,

    /** Razorpay signature verified successfully */
    VERIFIED,

    /** Business action (saga step) is in progress */
    SUCCESS_PENDING,

    /** Payment fully completed — business action succeeded */
    SUCCESS,

    /** Payment verification failed (signature, amount, etc.) */
    FAILED,

    /** Payment was verified but business action failed — compensation applied */
    COMPENSATED
}
