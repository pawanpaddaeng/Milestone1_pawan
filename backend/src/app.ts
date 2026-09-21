import express, { type Express } from 'express';
import { OAuth2Client } from 'google-auth-library';

export function createApp(): Express {
  const app = express();
  app.use(express.json())
  const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

  interface GoogleAuthRequestBody {
    idToken: string;
  }

  app.post('/api/auth/google', async (req: Request<{}, {}, GoogleAuthRequestBody>, res: Response) => {
    const { idToken } = req.body;

    if (!idToken) {
      return res.status(400).json({ error: 'idToken is required' });
    }

    try {
      // Verify the token with Google
      const ticket = await googleClient.verifyIdToken({
        idToken: idToken,
        audience: process.env.GOOGLE_CLIENT_ID,
      });

      const payload = ticket.getPayload();

      if (!payload) {
        return res.status(401).json({ error: 'Invalid token payload' });
      }

      // Extract user profile information safely verified by Google
      const userId = payload.sub;          // Unique Google User ID
      const email = payload.email;        // User's email address
      const name = payload.name;          // User's full name
      const picture = payload.picture;    // User's profile image URL

      console.log(`Successfully authenticated user: ${email} (${userId})`);

      // TODO: Look up or create user in your database, generate your session token (JWT/Cookie)
      return res.status(200).json({
        message: 'Authentication successful',
        user: {
          userId,
          email,
          name,
          picture,
        },
      });
    } catch (error) {
      console.error('Token verification failed:', error);
      return res.status(401).json({ error: 'Invalid Google ID token' });
    }
  });

  app.get('/health', (_req, res) => {
    res.json({ status: 'ok' });
  });

  app.use((_req, res) => {
    res.status(404).json({ error: 'Not Found' });
  });

  return app;
}
