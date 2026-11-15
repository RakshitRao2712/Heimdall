import type { ReactNode } from 'react';
import { useSession } from '@/auth/session';

// Heimdall access is free. Provider API billing remains separate.
export function PurchaseButton() {
  const { user } = useSession();
  return <div className="premium-purchase"><a className="lifetime-button" href={user ? '/dashboard' : '/login'}>{user ? 'Open dashboard' : 'Get started for free'}</a><p className="purchase-billing-disclosure">Google Places and Foursquare API usage is billed separately to your own provider account. Heimdall does not cover API fees. <a href="/terms">Terms</a></p></div>;
}
export function PremiumGate({ children }: { children: ReactNode }) { return <>{children}</>; }
