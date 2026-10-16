import jwt from 'jsonwebtoken';
import { env } from '../config/env';

// how long a token stays valid before the app has to sign in again
const TOKEN_LIFETIME = '30d';

// contents of the JWT given to the app, user_id identifies the user in later requests
export interface AuthTokenPayload {
  user_id: number;
}

export function createAuthToken(userId: number): string {
  const payload: AuthTokenPayload = { user_id: userId };
  return jwt.sign(payload, env.jwtSecret, { algorithm: 'HS256', expiresIn: TOKEN_LIFETIME });
}
