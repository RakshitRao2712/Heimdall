# Razorpay production configuration

Set server-only environment variables in Render:

- `RAZORPAY_KEY_ID`: the merchant's `rzp_live_...` key ID.
- `RAZORPAY_KEY_SECRET`: the matching live API secret.
- `RAZORPAY_WEBHOOK_SECRET`: a separate high-entropy shared webhook secret.
- `HEIMDALL_PREMIUM_EMAILS`: optional comma-separated exact account emails with complimentary Premium. This is an operator setting, never accepted from a browser request.

Configure a live-mode Razorpay webhook:

- URL: `https://heimdall.life/api/billing/razorpay/webhook`
- Events: `payment.captured`, `order.paid`, `refund.processed`.
- Use the same webhook secret as Render; do not reuse the API secret.
- Enable automatic capture in the merchant dashboard. Access requires captured INR 100 payments (10,000 paise).

Checkout creates an order on the backend and binds it to the authenticated account. Both the browser callback and raw-body signed webhooks fetch authoritative Razorpay payment state before granting access. Duplicate notifications are safe; delayed captured events cannot restore refunded purchases. Refunded payment access is revoked without affecting complimentary grants or a different later purchase.

Payments and order ownership persist in Neon when PostgreSQL storage is enabled. Test and live purchases use separate records. Keep live keys, webhook secrets and customer payment data out of source control, frontend variables and logs.

Verify checkout in Razorpay test mode before enabling live keys. Do not charge a live card solely for automated tests. Real settlement/refund and merchant KYC actions require the account owner's participation.
